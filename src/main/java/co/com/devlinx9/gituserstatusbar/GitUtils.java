package co.com.devlinx9.gituserstatusbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class GitUtils {
    private static final Logger LOGGER =
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * Utility method to run a shell command and capture/log its output.
     * Throws ContextK8sException on failure (non-zero exit or process error).
     */
    protected static String runCommand(String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = null;
        StringBuilder output = new StringBuilder();

        try {
            process = processBuilder.start();

            // Read command output
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    LOGGER.info("cmd output: " + line);
                    output.append(line).append(System.lineSeparator());
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.severe("'%s' failed with exit code %d".formatted(String.join(" ", command), exitCode));
                throw new GitUserVisorException(
                        "'" + String.join(" ", command) + "' command failed with exit code " + exitCode
                );
            }
        } catch (IOException | InterruptedException e) {
            throw new GitUserVisorException("Error executing command: " + String.join(" ", command));
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return output.toString();
    }
}
