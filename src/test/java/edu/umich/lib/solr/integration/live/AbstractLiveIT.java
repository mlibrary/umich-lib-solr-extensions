package edu.umich.lib.solr.integration.live;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Singleton-base-class for all live Solr integration tests.
 *
 * <p>Starts ONE Solr 10 container per JVM via a static initializer (the
 * Testcontainers-recommended "singleton container" pattern). Ryuk handles
 * container shutdown automatically when the JVM exits.
 *
 * <p>Mirrors the {@code docker/docker-compose.yml} layout: copies the test
 * configset and the freshly-packaged project JAR into the container
 * (at {@code /opt/umich-ext/<jar>}), and the {@code docker/init-libs.sh}
 * script that stages the JAR into {@code ${SOLR_HOME}/lib}.
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

  private static final String SOLR_IMAGE = "solr:10";
  private static final String CORE_NAME = "test-core";
  private static final int SOLR_PORT = 8983;

  static final GenericContainer<?> SOLR;
  static final String BASE_URL;

  static {
    if (shouldSkip()) {
      SOLR = null;
      BASE_URL = null;
    } else {
      autoDiscoverDockerHost();
      hardFailIfNoDockerInCI();

      Path projectBaseDir = resolveProjectBaseDir();
      Path configset = projectBaseDir.resolve("src/test/resources/solr-integration/test-core");
      Path targetDir = projectBaseDir.resolve("target");
      Path initScript = projectBaseDir.resolve("docker/init-libs.sh");

      if (!Files.isDirectory(configset)) {
        throw new IllegalStateException("Missing configset directory: " + configset);
      }
      if (!Files.isDirectory(targetDir)) {
        throw new IllegalStateException(
            "Missing target/ directory: " + targetDir + " (run `mvn package` first).");
      }
      if (!Files.isRegularFile(initScript)) {
        throw new IllegalStateException("Missing init-libs.sh: " + initScript);
      }

      Path projectJar = findProjectJar(targetDir);

      // Stage only the project JAR in a temp directory.
      //
      // Docker's container archive API (used by withCopyFileToContainer) requires the
      // destination parent directory to already exist inside the image.  /opt/umich-ext/
      // is NOT present in solr:10, so any withCopyFileToContainer call targeting it
      // silently fails (the Java Docker client swallows the error) and the init script
      // then cannot find the JAR.
      //
      // withFileSystemBind (a host->container bind mount) does not go through the archive
      // API: Docker creates the mount-point automatically.  This mirrors docker-compose's
      //   ../target:/opt/umich-ext:ro
      // volume entry exactly, but copies only the single project JAR rather than all of
      // target/.
      Path stagingDir;
      try {
        stagingDir = Files.createTempDirectory("umich-solr-lib-");
        Files.copy(projectJar, stagingDir.resolve(projectJar.getFileName()));
      } catch (IOException e) {
        throw new IllegalStateException("Cannot create staging directory for project JAR", e);
      }
      Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteQuietly(stagingDir)));

      // EXT does not use ICU / analysis-extras; no SOLR_MODULES env var needed.
      SOLR = new GenericContainer<>(SOLR_IMAGE)
          .withExposedPorts(SOLR_PORT)
          .withCopyFileToContainer(MountableFile.forHostPath(configset, 0755), "/opt/configs/" + CORE_NAME)
          .withFileSystemBind(stagingDir.toString(), "/opt/umich-ext", BindMode.READ_ONLY)
          .withCopyFileToContainer(
              MountableFile.forHostPath(initScript, 0755),
              "/docker-entrypoint-initdb.d/init-libs.sh")
          .withCommand("solr-precreate", CORE_NAME, "/opt/configs/" + CORE_NAME)
          .waitingFor(Wait.forHttp("/solr/admin/cores?action=STATUS&core=" + CORE_NAME)
              .forPort(SOLR_PORT)
              .forStatusCode(200)
              .withStartupTimeout(Duration.ofMinutes(2)));

      SOLR.start();
      BASE_URL = "http://" + SOLR.getHost() + ":" + SOLR.getMappedPort(SOLR_PORT) + "/solr";
    }
  }

  protected SolrClient client;

  @BeforeAll
  void createClient() {
    Assumptions.assumeFalse(shouldSkip(),
        "Live IT skipped via -DskipLiveIT=true or SKIP_LIVE_IT=true");
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
   * @throws IllegalStateException if no matching JAR is found
   */
  private static Path findProjectJar(Path targetDir) {
    try {
      Optional<Path> jar = Files.list(targetDir)
          .filter(p -> {
            String name = p.getFileName().toString();
            return name.startsWith("umich-solr-extensions-")
                && name.endsWith(".jar")
                && !name.contains("-sources")
                && !name.contains("-javadoc")
                && !name.contains("-tests");
          })
          .findFirst();
      if (jar.isEmpty()) {
        throw new IllegalStateException(
            "Project JAR not found in " + targetDir
                + ". Run 'mvn package -DskipTests' first.");
      }
      return jar.get();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot list " + targetDir, e);
    }
  }

  /**
   * Recursively deletes {@code dir} and all its contents, ignoring any errors.
   * Used to clean up the temporary staging directory on JVM exit.
   */
  private static void deleteQuietly(Path dir) {
    try (Stream<Path> walk = Files.walk(dir)) {
      walk.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        } catch (IOException ignored) {
        }
      });
    } catch (IOException ignored) {
    }
  }

  /**
   * If the user/CI has not pointed Testcontainers at a Docker socket, probe the
   * usual unix-socket locations and set the first one we find as the
   * {@code DOCKER_HOST} system property. Testcontainers honors that property.
   *
   * <p>Order:
   * <ol>
   *   <li>{@code /var/run/docker.sock} — Linux default, GitHub Actions, most CI runners.</li>
   *   <li>{@code $HOME/.docker/run/docker.sock} — macOS Docker Desktop.</li>
   *   <li>{@code $XDG_RUNTIME_DIR/docker.sock} — Linux rootless Docker.</li>
   *   <li>{@code $HOME/.colima/default/docker.sock} — Colima default profile.</li>
   * </ol>
   */
  private static void autoDiscoverDockerHost() {
    if (System.getProperty("DOCKER_HOST") != null
        || System.getenv("DOCKER_HOST") != null
        || System.getProperty("testcontainers.docker.host") != null) {
      return;
    }
    String home = System.getProperty("user.home", "");
    String xdg = System.getenv("XDG_RUNTIME_DIR");
    Path[] candidates = new Path[] {
        Paths.get("/var/run/docker.sock"),
        home.isEmpty() ? null : Paths.get(home, ".docker", "run", "docker.sock"),
        xdg == null ? null : Paths.get(xdg, "docker.sock"),
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
