package convertimagestobase64;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * https://www.baeldung.com/run-shell-command-in-java
 *
 * @author Flavio Lima
 */
public class ShellImageMagick {

    public void runCommand(String[] command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = null;

        String cmdStr = "";
        for (String str : command) {
            cmdStr += str + " ";
        }
        System.out.println("[DEGUB] Runing Command: " + cmdStr);

        try {
            process = pb.start();

            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder error = new StringBuilder();
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = errorReader.readLine()) != null) {
                error.append(line).append("\n");
            }

            int exitVal = process.waitFor();
            if (exitVal == 0) {
                System.out.println("[DEBUG] Success!");
                System.out.println("[DEBUG] ImageMagick output: " + output);
            } else {
                //abnormal...
                System.out.println("[ERROR] " + error);
            }
            
            reader.close();
            errorReader.close();
        } catch (IOException | InterruptedException ex) {
            System.err.println("[ERROR] " + ex.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
