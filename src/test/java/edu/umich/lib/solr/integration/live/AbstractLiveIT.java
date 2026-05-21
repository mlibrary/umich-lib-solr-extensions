package edu.umich.lib.solr.integration.live;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
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
 * <p>Starts ONE Solr 10 container per JVM via a static initializer (the
 * Testcontainers-recommended "singleton container" pattern). Ryuk handles
 * container shutdown automatically when the JVM exits.
 *
 * <p>Builds a thin Docker image from {@code solr:10} with the project JAR
 * baked in at {@code /var/solr/data/lib/}. This is the Solr shared-lib
 * directory (scanned for all cores) and is initialised from the image into
 * the anonymous Docker volume on first container start -- no bind mounts,
 * no init scripts, no Docker archive-API workarounds required.
 *
 * <p>The test configset ({@code src/test/resources/solr-integration/test-core})
 * is copied into the container with {@code withCopyFileToContainer}.
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

  private static final String BASE_IMAGE = "solr:10";
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

    // Build a thin image based on solr:10 with the project JAR baked in.
    //
    // The JAR is placed at /var/solr/data/lib/ -- Solr's shared-lib directory,
    // scanned for all cores.  Because /var/solr is a VOLUME in solr:10, Docker
    // initialises the anonymous volume from the image content on first use, so
    // files placed there in the Dockerfile are visible at container startup.
    //
    // This avoids:
    //   - bind mounts (unreliable in some CI environments)
    //   - the Docker archive API (silently fails when the target dir is missing)
    //   - init scripts (run after the volume is mounted, need a staging area)
    ImageFromDockerfile image = new ImageFromDockerfile()
        .withDockerfileFromBuilder(builder ->
            builder.from(BASE_IMAGE)
                   .user("root")
                   .run("mkdir -p /var/solr/data/lib")
                   .copy(jarName, "/var/solr/data/lib/" + jarName)
                   .run("chown -R solr:solr /var/solr/data/lib")
                   .user("solr")
                   .build())
        .withFileFromPath(jarName, projectJar);

    GenericContainer<?> solr = new GenericContainer<>(image)
        .withExposedPorts(SOLR_PORT)
        .withCopyFileToContainer(
            MountableFile.forHostPath(configset, 0755),
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
