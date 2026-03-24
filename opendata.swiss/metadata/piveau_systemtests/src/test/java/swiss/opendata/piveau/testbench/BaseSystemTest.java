package swiss.opendata.piveau.testbench;

import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;

import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

import org.junit.jupiter.api.extension.ExtendWith;
import swiss.opendata.piveau.testbench.dag.DagExtension;

/**
 * Base class for all System Tests.
 * Manages the singleton Docker Compose environment.
 */
@ExtendWith(DagExtension.class)
@ExtendWith(TestContextParameterResolver.class)
public abstract class BaseSystemTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseSystemTest.class);

    // Singleton Container Instance
    // We might wrap this in a customized 'EnvironmentManager' to handle restarts/reconfiguration more gracefully.
    protected static DockerComposeContainer<?> PIVEAU_ENV;

    // Configuration
    private static final File COMPOSE_FILE = new File("docker-compose.yaml");
    private static final String PROFILE_PATH = System.getProperty("piveau.profile.path", "../piveau_profile");

    // Log Capturers
    private static final org.testcontainers.containers.output.ToStringConsumer HUB_REPO_LOGS = new org.testcontainers.containers.output.ToStringConsumer();
    private static final org.testcontainers.containers.output.ToStringConsumer HUB_SEARCH_LOGS = new org.testcontainers.containers.output.ToStringConsumer();

    @BeforeAll
    public static void setupEnvironment() {
        if (PIVEAU_ENV == null) {
            startEnvironment();
        }
    }

    private static void startEnvironment() {
        LOG.info("Starting Piveau System Test Environment...");
        LOG.info("Compose File: {}", COMPOSE_FILE.getAbsolutePath());
        LOG.info("Profile Path: {}", PROFILE_PATH);

        cleanupLeftovers(COMPOSE_FILE);

        // We use the raw DockerComposeContainer. 
        // In the future this construction logic should be moved to a builder 
        // to check for @ClassLevelConfig annotations.
        PIVEAU_ENV = new DockerComposeContainer<>(COMPOSE_FILE).withOptions("--compatibility") // helpful for some memory limits etc
                // Example of exposing services (ports are usually auto-mapped but we can follow compose)
                .withExposedService("piveau-hub-repo", 8080).withExposedService("piveau-hub-repo", 5000) // Vert.x shell telnet
                .withExposedService("piveau-hub-search", 8080).withExposedService("piveau-hub-search", 5000) // Vert.x shell telnet
                .withExposedService("graphdb", 7200) // Expose GraphDB for verification
                // Disable OTEL to avoid dependency on collector in minimal test setups
                .withEnv("OTEL_SDK_DISABLED", "true").withLogConsumer("piveau-hub-repo", new Slf4jLogConsumer(LOG).withPrefix("HUB-REPO")).withLogConsumer("piveau-hub-repo", HUB_REPO_LOGS).withLogConsumer("piveau-hub-search", new Slf4jLogConsumer(LOG).withPrefix("HUB-SEARCH")).withLogConsumer("piveau-hub-search", HUB_SEARCH_LOGS)
                // Wait strategies - essential for stability
                .waitingFor("piveau-hub-repo", Wait.forHttp("/health").forPort(8080).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));

        PIVEAU_ENV.start();
        LOG.info("Environment Started successfully.");
    }

    public static void forceRestart() {
        if (PIVEAU_ENV != null) {
            LOG.info("Forcing environment restart (Fresh SUT requested)...");
            try {
                PIVEAU_ENV.stop();
            } catch (Exception e) {
                LOG.warn("Error stopping environment: {}", e.getMessage());
            }
            cleanupLeftovers(COMPOSE_FILE);
            PIVEAU_ENV = null;
        }
    }

    public static String getHubRepoLogs() {
        return HUB_REPO_LOGS.toUtf8String();
    }

    public static String getHubSearchLogs() {
        return HUB_SEARCH_LOGS.toUtf8String();
    }

    public static String getSparqlEndpoint() {
        // GraphDB is exposed on 7200. We need to find the mapped port.
        // Container name in compose is "graphdb", but we removed the fixed name.
        // DockerComposeContainer uses the service name.
        String host = PIVEAU_ENV.getServiceHost("graphdb", 7200);
        Integer port = PIVEAU_ENV.getServicePort("graphdb", 7200);
        return "http://" + host + ":" + port + "/repositories/piveau";
    }

    /**
     * Helper to get the actual mapped port for a service (since Testcontainers might randomize if configured).
     * If using host networking or fixed ports in compose, this might just return the fixed port.
     */
    protected int getServicePort(String serviceName, int originalPort) {
        return PIVEAU_ENV.getServicePort(serviceName, originalPort);
    }

    protected String getServiceHost(String serviceName, int originalPort) {
        return PIVEAU_ENV.getServiceHost(serviceName, originalPort);
    }

    protected java.util.Optional<org.testcontainers.containers.ContainerState> getContainer(String serviceName) {
        return PIVEAU_ENV.getContainerByServiceName(serviceName + "_1").or(() -> PIVEAU_ENV.getContainerByServiceName(serviceName + "-1")).or(() -> PIVEAU_ENV.getContainerByServiceName(serviceName));
    }


    private static void cleanupLeftovers(File composeFile) {
        LOG.info("Cleaning up potential leftover containers...");
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "compose", "-f", composeFile.getAbsolutePath(), "down", "--remove-orphans", "--volumes"
            );
            pb.inheritIO();
            Process p = pb.start();
            p.waitFor();
            LOG.info("Cleanup finished (Exit: {})", p.exitValue());

            // Cleanup Host Volumes
            // We use a docker container to remove files because they might be owned by root
            File volumesDir = new File(composeFile.getAbsoluteFile().getParentFile(), "volumes");
            if (volumesDir.exists()) {
                LOG.info("Deleting persistent data volumes via Docker at: {}", volumesDir.getAbsolutePath());
                ProcessBuilder pbCleanup = new ProcessBuilder(
                        "docker", "run", "--rm", "-v", volumesDir.getAbsolutePath() + ":/clean_target", "alpine", "sh", "-c", "rm -rf /clean_target/* && mkdir -p /clean_target/elasticsearch-data && mkdir -p /clean_target/graphdb-data && chmod -R 777 /clean_target"
                );
                pbCleanup.inheritIO();
                Process pCleanup = pbCleanup.start();
                pCleanup.waitFor();
                LOG.info("Volume cleanup finished (Exit: {})", pCleanup.exitValue());
            }

        } catch (Exception e) {
            LOG.warn("Failed to cleanup leftovers: {}", e.getMessage());
        }
    }

}
