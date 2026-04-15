package swiss.opendata.piveau.testbench.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.Socket;

/**
 * Utility for executing commands inside Vert.x shell containers.
 * Connects directly to the exposed Vert.x Telnet shell (port 5000)
 * using a raw Java Socket and implements minimal Telnet negotiation
 * to avoid server-side NPEs and correctly read outputs.
 */
public class VertxShellUtils {

    private static final Logger LOG = LoggerFactory.getLogger(VertxShellUtils.class);

    /**
     * Executes a Vert.x shell command via Telnet on port 5000.
     * <p>
     * Polls the output for the given marker string, checking every second,
     * up to timeoutSeconds.
     *
     * @param container      the Testcontainers container to connect to
     * @param command        the shell command to execute
     * @param marker         string to wait for in the output
     * @param timeoutSeconds max seconds to wait for the marker to appear
     * @return an ExecResult containing the gathered shell output in stdout
     * @throws IOException          if network fails
     * @throws InterruptedException if thread is interrupted
     */
    public static Container.ExecResult executeShellCommand(
                                                           org.testcontainers.containers.ContainerState container, String command, String marker, int timeoutSeconds) throws IOException, InterruptedException {

        LOG.info("Executing shell command via Telnet on {}: {} (marker: '{}', max timeout: {}s)", container.getContainerId().substring(0, 8), command, marker, timeoutSeconds);

        String host = container.getHost();
        Integer port = container.getMappedPort(5000);

        StringBuilder finalOutput = new StringBuilder();
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(5000);

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int read;
            boolean promptFound = false;
            long timeout = System.currentTimeMillis() + 10000;

            // Phase 1: Connect and handle Telnet negotiation until prompt
            while (System.currentTimeMillis() < timeout && !promptFound) {
                if (in.available() > 0) {
                    read = in.read(buffer);
                    if (read > 0) {
                        for (int i = 0; i < read; i++) {
                            int b = buffer[i] & 0xFF;
                            if (b == 255) { // IAC
                                if (i + 2 < read) {
                                    int cmd = buffer[i + 1] & 0xFF;
                                    int option = buffer[i + 2] & 0xFF;
                                    if (cmd == 253) { // DO -> WILL
                                        out.write(new byte[]{(byte) 255, (byte) 251, (byte) option}); // WILL
                                        out.flush();
                                        if (option == 31) { // NAWS
                                            out.write(new byte[]{(byte) 255, (byte) 250, 31, 0, 80, 0, 24, (byte) 255, (byte) 240});
                                            out.flush();
                                        }
                                    } else if (cmd == 251) { // WILL -> DO
                                        out.write(new byte[]{(byte) 255, (byte) 253, (byte) option});
                                        out.flush();
                                    }
                                    i += 2;
                                }
                            } else {
                                if ((char) b == '%') {
                                    promptFound = true;
                                }
                            }
                        }
                    }
                } else {
                    Thread.sleep(100);
                }
            }

            if (!promptFound) {
                String errmsg = "Timeout waiting for Vert.x Shell prompt on " + host + ":" + port;
                LOG.error(errmsg);
                return createExecResult(-1, "", errmsg);
            }

            // Phase 2: Send the command
            out.write((command + "\r\n").getBytes());
            out.flush();

            // Phase 3: Read output until the requested marker is found or timeout
            timeout = System.currentTimeMillis() + (timeoutSeconds * 1000L);
            boolean markerFound = false;
            while (System.currentTimeMillis() < timeout && !markerFound) {
                if (in.available() > 0) {
                    read = in.read(buffer);
                    if (read > 0) {
                        for (int i = 0; i < read; i++) {
                            int b = buffer[i] & 0xFF;
                            if (b != 255) { // ignore negotiation bytes if any trail
                                finalOutput.append((char) b);
                                if (finalOutput.toString().contains(marker)) {
                                    markerFound = true;
                                }
                            } else {
                                i += 2; // skip telnet commands
                            }
                        }
                    }
                } else {
                    Thread.sleep(100);
                }
            }

            if (!markerFound) {
                LOG.warn("Did not find target marker '{}' after {}s. Returning gathered output.", marker, timeoutSeconds);
            }

            // Phase 4: Clean exit
            out.write("exit\r\n".getBytes());
            out.flush();
            Thread.sleep(500);

        } catch (Exception e) {
            LOG.error("Telnet execution failed", e);
            return createExecResult(-1, finalOutput.toString(), e.getMessage());
        }

        String stdOut = finalOutput.toString();
        stdOut = stdOut.replace(command + "\r\n", "").replace("%", "").trim();
        LOG.debug("Shell stdout:\n{}", stdOut);

        return createExecResult(0, stdOut, "");
    }

    private static Container.ExecResult createExecResult(int exitCode, String stdout, String stderr) {
        try {
            Constructor<Container.ExecResult> constructor = Container.ExecResult.class.getDeclaredConstructor(int.class, String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(exitCode, stdout == null ? "" : stdout, stderr == null ? "" : stderr);
        } catch (Exception e) {
            LOG.error("Failed to construct ExecResult via reflection", e);
            return null; // Should not happen
        }
    }

    /**
     * Overload with default marker "Command finished".
     */
    public static Container.ExecResult executeShellCommand(
                                                           org.testcontainers.containers.ContainerState container, String command, int timeoutSeconds) throws IOException, InterruptedException {
        return executeShellCommand(container, command, "Command finished", timeoutSeconds);
    }
}
