package convertimagestobase64;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://www.baeldung.com/run-shell-command-in-java
 *
 * @author Flavio Lima
 */
public class ShellImageExifIptc {

    public static final String EXIF = "exif";
    public static final String IPTC = "iptc";

    /**
     * Faz o parse da saída do comando de extração e retorna uma lista de
     * queries de inserção
     *
     * @param output saída do comando de extração
     * @param option 1 para Exif, 2 para IPTC
     * @return
     */
    public Map<String, String> addExifIPTCMetadata(List<String> output, int option) {
        String key, top = null;
        String no_data_msg = "";

        switch (option) {
            case 1:
                top = EXIF;
                no_data_msg = "No Exif data found in the file";
                break;
            case 2:
                top = IPTC;
                no_data_msg = "No IPTC data found in the file";
                break;
        }

        System.out.println("[DEBUG] addExifIPTCMetadata - option = " + top);

        Map<String, String> data = new HashMap<>();

        String[] line_array;
        String value, key_str;
        int c;

        for (String line : output) {
            if (line.contains(no_data_msg)) {
                break;
            }

            if (line.startsWith("Warning") || line.startsWith("Error")) {
                continue;
            }

            if (option == 1) {
                if (!line.startsWith("Exif") || line.contains("Exif.Canon.CustomFunctions")
                        || line.contains("Exif.Canon.ColorData")) {
                    continue;
                }
            }

            if (option == 2) {
                if (!line.startsWith("Iptc")) {
                    continue;
                }
            }

//            System.out.println("line = " + line);
            line_array = line.split(" ");
            value = "";
            c = 0;
            key = null;

            for (int i = 0; i < line_array.length; i++) {
                line = line_array[i];
                if (!line.isEmpty()) {
                    c++;
                    if (c == 1) // key
                    {
                        key_str = line_array[i];
                        if (!key_str.contains(":")) {
                            key = key_str;
                        }
                    } else if (c >= 4) {
                        value += escape_invalid_xml_chars(line_array[i]);
                        if (i + 1 < line_array.length) {
                            value += " ";
                        }
                    }
                }
            }

            if (key != null) {
                data.put(key, value);
                if (top != null) {
                    top = key;
                    //McLog.log("addExifIPTCMetadata", key.getName() + ": " + key.getText());
                }
            }
        }

        return data;
    }

    public String escape_invalid_xml_chars(String in_str) //throws McErrorInfo
    {
        String stripped;
        char c1, c2, c3;

        StringBuilder out = new StringBuilder(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in_str == null || in_str.equals("")) {
            return ""; // vacancy test.
        }
        //McLog.log("escape_invalid_xml_chars > começo do for...");
        for (int i = 0; i < in_str.length(); i++) {
            current = in_str.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9)
                    || (current == 0xA)
                    || (current == 0xD)
                    || ((current >= 0x20) && (current <= 0xD7FF))
                    || ((current >= 0xE000) && (current <= 0xFFFD))
                    || ((current >= 0x10000) && (current <= 0x10FFFF))) {
                if (in_str.length() == 1) {
                    return in_str;
                }

                // &#x03;
                while (current == '&') {
                    c1 = in_str.charAt(i + 1);
                    c2 = in_str.charAt(i + 2);
                    if (c1 == '#' && c2 == 'x') {
                        stripped = "";
                        stripped += current;
                        stripped += c1;
                        stripped += c2;
                        i += 3;
                        c3 = c2;
                        while (c3 != ';') {
                            c3 = in_str.charAt(i);
                            stripped += c3;
                            i++;
                        }
                        System.out.println("[ERROR] escape_invalid_xml_chars > stripped = " + stripped);
                        current = in_str.charAt(i);
                    } else {
                        break;
                    }
                }
                out.append(current);
            }
        }
        return out.toString();
    }

    public Map<String, String> runCommand(String[] command, int option) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = null;

        String cmdStr = "";
        for (String str : command) {
            cmdStr += str + " ";
        }
        System.out.println("[DEBUG] runing Command: " + cmdStr);

        Map<String, String> data = null;
        try {
            process = pb.start();

            List<String> output = new ArrayList<>();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder error = new StringBuilder();
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));

            String line;
            line = reader.readLine();
            System.out.println("[DEBUG] command output: " + line);
            while (line != null) {
                output.add(line);
                line = reader.readLine();
                System.out.println("[DEBUG] command output: " + line);
            }

            line = errorReader.readLine();
            System.out.println("[ERROR] command error: " + line);
            while (line != null) {
                error.append(line).append("\n");
                line = errorReader.readLine();
                System.out.println("[ERROR] command error: " + line);
            }

            System.out.println("[DEBUG] Waiting for...");
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                System.out.println("[DEBUG] Success!");
                data = addExifIPTCMetadata(output, option);
            } else {
                //abnormal...
                System.out.println("[ERROR] " + error);
            }
        } catch (IOException | InterruptedException ex) {
            System.err.println("[ERROR] " + ex.getMessage());
        } finally {
            System.out.println("[DEBUG] destroying process: " + cmdStr);
            if (process != null) {
                process.destroy();
                System.out.println("[DEBUG] destroyed!");
            }
        }

        return data;
    }
}
