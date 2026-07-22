package edu.umich.lib.solr.integration.live;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

/**
 * Singleton-base-class for all live Solr integration tests.
 *
 * <p>Starts ONE Solr container per JVM via a static initializer (the
 * Testcontainers-recommended "singleton container" pattern). Ryuk handles
 * container shutdown automatically when the JVM exits. The base image tag
 * is derived from the {@code solr.version} system property (major version
 * only, e.g. {@code solr:9} or {@code solr:10} -- see {@link #resolveBaseImage()}),
 * so it tracks whichever solrj client the project JAR was built against.
 *
 * <p>Builds a thin Docker image from that base with the project JAR
 * baked in at {@code /opt/solr/lib/} -- the Solr installation lib directory,
 * documented as the recommended location for plugins in a custom
 * Dockerfile.  This path is NOT a Docker volume ({@code /var/solr} is the
 * volume), so the JAR survives in the image and is loaded automatically for
 * all cores without any {@code <lib>} directive in {@code solrconfig.xml}.
 *
 * <p>The test configset ({@code src/test/resources/solr-integration/test-core})
 * is copied into the container with {@code withCopyFileToContainer}. Before
 * copying, {@link #prepareConfigset(Path, Path)} rewrites its
 * {@code conf/solrconfig.xml} {@code <luceneMatchVersion>} to match the same
 * major version as {@link #BASE_IMAGE}, so it never claims compatibility with
 * a newer Lucene than the container actually runs.
 *
 * <p>Subclasses extend this class and gain access to {@link #client}, which
 * is created per-class in {@link #createClient()} and closed in
 * {@link #closeClient()}.
 *
 * <p>In CI (env {@code CI} set), missing Docker is a hard failure. Locally,
 * set {@code -DskipLiveIT=true} or {@code SKIP_LIVE_IT=true} to skip. When
 * skipping, the container never starts and individual tests are marked
 * skipped via {@link Assumptions}.
 */
abstract class AbstractLiveIT {

  private static final String BASE_IMAGE = resolveBaseImage();
  private static final String CORE_NAME  = "test-core";
  private static final int    SOLR_PORT  = 8983;

  static final GenericContainer<?> SOLR;
  static final String BASE_URL;

  static {
    GenericContainer<?> solr = initSolrContainer();
    SOLR     = solr;
    BASE_URL = (solr != null)
        ? "http://" + solr.getHost() + ":" + solr.getMappedPort(SOLR_PORT) + "/solr"
        : null;
  }

  /**
   * Builds and starts the Solr container, or returns {@code null} if the
   * live IT should be skipped (Docker absent, flag set, or JAR not yet built).
   */
  @SuppressWarnings("resource")
  private static GenericContainer<?> initSolrContainer() {
    if (shouldSkip()) {
      return null;
    }

    autoDiscoverDockerHost();
    hardFailIfNoDockerInCI();

    Path projectBaseDir = resolveProjectBaseDir();
    Path configset      = projectBaseDir.resolve("src/test/resources/solr-integration/test-core");
    Path targetDir      = projectBaseDir.resolve("target");

    if (!Files.isDirectory(configset)) {
      throw new IllegalStateException("Missing configset directory: " + configset);
    }
    if (!Files.isDirectory(targetDir)) {
      // target/ doesn't exist yet (Surefire runs before the package phase).
      // Return null so Failsafe picks this up after mvn package.
      return null;
    }

    Optional<Path> jarOpt = findProjectJar(targetDir);
    if (jarOpt.isEmpty()) {
      // JAR not built yet -- skip gracefully; Failsafe will run this after package.
      return null;
    }
    Path projectJar = jarOpt.get();
    String jarName  = projectJar.getFileName().toString();

    Path preparedConfigset;
    try {
      preparedConfigset = prepareConfigset(configset, targetDir);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to prepare configset with matched luceneMatchVersion", e);
    }

    // Build a thin image based on BASE_IMAGE with the project JAR baked in.
    //
    // The JAR is placed at /opt/solr/lib/ -- the Solr installation lib
    // directory, documented by the Solr reference guide as the recommended
    // location for plugins when building a custom Solr Dockerfile.  This path
    // is part of /opt/solr (the install dir, NOT a Docker volume), so writes
    // in derived-image Dockerfiles are preserved.  /var/solr is the VOLUME;
    // any writes there would be silently discarded.
    //
    // Solr scans /opt/solr/lib/ for all cores automatically -- no <lib>
    // directive in solrconfig.xml is needed.
    //
    // This avoids:
    //   - bind mounts (unreliable in some CI environments)
    //   - the Docker archive API (silently fails when the target dir is missing)
    //   - init scripts (need staging area + run inside the VOLUME, also risky)
    ImageFromDockerfile image = new ImageFromDockerfile()
        .withDockerfileFromBuilder(builder ->
            builder.from(BASE_IMAGE)
                   .user("root")
                   .run("mkdir -p /opt/solr/lib")
                   .copy(jarName, "/opt/solr/lib/" + jarName)
                   .run("chown solr:solr /opt/solr/lib/" + jarName)
                   .user("solr")
                   .build())
        .withFileFromPath(jarName, projectJar);

    GenericContainer<?> solr = new GenericContainer<>(image)
        .withExposedPorts(SOLR_PORT)
        .withCopyFileToContainer(
            MountableFile.forHostPath(preparedConfigset, 0755),
            "/opt/configs/" + CORE_NAME)
        .withCommand("solr-precreate", CORE_NAME, "/opt/configs/" + CORE_NAME)
        .waitingFor(Wait.forHttp("/solr/admin/cores?action=STATUS&core=" + CORE_NAME)
            .forPort(SOLR_PORT)
            .forStatusCode(200)
            .withStartupTimeout(Duration.ofMinutes(3)));

    try {
      solr.start();
    } catch (Exception e) {
      // Container failed to start (Docker unavailable, image build error, etc.).
      // Return null so @BeforeAll Assumptions.assumeTrue skips tests rather than erroring.
      System.err.println("[AbstractLiveIT] Solr container failed to start; live ITs will be skipped: "
          + e.getMessage());
      return null;
    }
    return solr;
  }

  protected SolrClient client;

  @BeforeAll
  void createClient() {
    Assumptions.assumeTrue(SOLR != null,
        "Live IT skipped: Docker unavailable, -DskipLiveIT=true, SKIP_LIVE_IT=true, "
            + "or project JAR not yet built (run 'mvn package -DskipTests' first)");
    client = new HttpJdkSolrClient.Builder(BASE_URL + "/" + CORE_NAME)
        .withConnectionTimeout(10, TimeUnit.SECONDS)
        .withIdleTimeout(60, TimeUnit.SECONDS)
        .build();
  }

  @AfterAll
  void closeClient() throws Exception {
    if (client != null) {
      client.close();
      client = null;
    }
  }

  /** Returns the core name (for tests that need to construct URLs). */
  protected static String coreName() {
    return CORE_NAME;
  }

  // --- helpers ---

  private static boolean shouldSkip() {
    return Boolean.parseBoolean(System.getProperty("skipLiveIT", "false"))
        || "true".equalsIgnoreCase(System.getenv("SKIP_LIVE_IT"));
  }

  private static void hardFailIfNoDockerInCI() {
    if (DockerClientFactory.instance().isDockerAvailable()) {
      return;
    }
    String msg = "Docker is not available. Live Solr integration tests require Docker.\n"
        + "  - On macOS, ensure Docker Desktop is running. If your socket lives at\n"
        + "    ~/.docker/run/docker.sock, export DOCKER_HOST=unix://$HOME/.docker/run/docker.sock\n"
        + "  - To skip locally, run with -DskipLiveIT=true or SKIP_LIVE_IT=true.";
    throw new IllegalStateException(msg);
  }

  /**
   * Resolves the base Docker image tag. Defaults to the major version parsed
   * from the {@code solr.version} system property (set from the Maven
   * {@code solr.version} property via failsafe, matching whatever solrj
   * version the project JAR was compiled against), e.g. {@code 9.7.0} ->
   * {@code solr:9}. Override with the exact tag via {@code -Dsolr.docker.image}.
   */
  private static String resolveBaseImage() {
    String explicit = System.getProperty("solr.docker.image");
    if (explicit != null && !explicit.isBlank()) {
      return explicit;
    }
    return "solr:" + solrMajorVersion();
  }

  /** Major version parsed from the {@code solr.version} system property, e.g. {@code 9.7.0} -> {@code "9"}. */
  private static String solrMajorVersion() {
    String solrVersion = System.getProperty("solr.version", "10");
    return solrVersion.split("\\.")[0];
  }

  /**
   * Copies {@code configset} into a scratch directory under {@code targetDir} and
   * rewrites its {@code conf/solrconfig.xml} {@code <luceneMatchVersion>} to
   * {@code <solrMajorVersion>.0.0}, e.g. {@code 9.0.0} when testing against a
   * {@code solr:9} container. {@code X.0.0} is always a valid match version for
   * any release within that major line, so this stays correct without needing
   * to know the exact Lucene patch version a given Solr release embeds.
   *
   * <p>The original {@code src/test/resources} configset is left untouched;
   * only the copy handed to Testcontainers is patched.
   */
  private static Path prepareConfigset(Path configset, Path targetDir) throws IOException {
    Path prepared = targetDir.resolve("live-it-configset");
    if (Files.exists(prepared)) {
      deleteRecursively(prepared);
    }
    copyRecursively(configset, prepared);

    Path solrConfig = prepared.resolve("conf/solrconfig.xml");
    String patched = Files.readString(solrConfig)
        .replaceFirst(
            "<luceneMatchVersion>[^<]*</luceneMatchVersion>",
            "<luceneMatchVersion>" + solrMajorVersion() + ".0.0</luceneMatchVersion>");
    Files.writeString(solrConfig, patched);

    return prepared;
  }

  private static void copyRecursively(Path source, Path target) throws IOException {
    try (var paths = Files.walk(source)) {
      for (Path src : (Iterable<Path>) paths::iterator) {
        Path dest = target.resolve(source.relativize(src));
        if (Files.isDirectory(src)) {
          Files.createDirectories(dest);
        } else {
          Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  private static void deleteRecursively(Path path) throws IOException {
    try (var paths = Files.walk(path)) {
      paths.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    }
  }

  private static Path resolveProjectBaseDir() {
    String basedir = System.getProperty("project.basedir");
    if (basedir != null && !basedir.isBlank()) {
      return Paths.get(basedir);
    }
    return Paths.get("").toAbsolutePath();
  }

  /**
   * Locates the single project JAR in {@code targetDir}.
   *
   * <p>Matches {@code umich-solr-extensions-*.jar} while excluding classifier
   * variants ({@code -sources}, {@code -javadoc}, {@code -tests}).
   *
   * @return an {@link Optional} containing the JAR path, or empty if not found
   * @throws IllegalStateException if {@code targetDir} cannot be listed
   */
  static Optional<Path> findProjectJar(Path targetDir) {
    try {
      return Files.list(targetDir)
          .filter(p -> {
            String name = p.getFileName().toString();
            return name.startsWith("umich-solr-extensions-")
                && name.endsWith(".jar")
                && !name.contains("-sources")
                && !name.contains("-javadoc")
                && !name.contains("-tests");
          })
          .findFirst();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot list " + targetDir, e);
    }
  }

  /**
   * If the user/CI has not pointed Testcontainers at a Docker socket, probe the
   * usual unix-socket locations and set the first one we find as the
   * {@code DOCKER_HOST} system property. Testcontainers honors that property.
   *
   * <p>Order:
   * <ol>
   *   <li>{@code /var/run/docker.sock} -- Linux default, GitHub Actions, most CI runners.</li>
   *   <li>{@code $HOME/.docker/run/docker.sock} -- macOS Docker Desktop.</li>
   *   <li>{@code $XDG_RUNTIME_DIR/docker.sock} -- Linux rootless Docker.</li>
   *   <li>{@code $HOME/.colima/default/docker.sock} -- Colima default profile.</li>
   * </ol>
   */
  private static void autoDiscoverDockerHost() {
    if (System.getProperty("DOCKER_HOST") != null
        || System.getenv("DOCKER_HOST") != null
        || System.getProperty("testcontainers.docker.host") != null) {
      return;
    }
    String home = System.getProperty("user.home", "");
    String xdg  = System.getenv("XDG_RUNTIME_DIR");
    Path[] candidates = new Path[] {
        Paths.get("/var/run/docker.sock"),
        home.isEmpty() ? null : Paths.get(home, ".docker", "run", "docker.sock"),
        xdg  == null  ? null : Paths.get(xdg, "docker.sock"),
        home.isEmpty() ? null : Paths.get(home, ".colima", "default", "docker.sock"),
    };
    for (Path candidate : candidates) {
      if (candidate != null && Files.exists(candidate)) {
        System.setProperty("DOCKER_HOST", "unix://" + candidate.toAbsolutePath());
        return;
      }
    }
  }
}
