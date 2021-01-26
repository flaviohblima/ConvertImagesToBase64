/**
 * Sources
 * https://stackoverflow.com/questions/36492084/how-to-convert-an-image-to-base64-string-in-java
 * https://stackabuse.com/java-list-files-in-a-directory/
 * https://www.javatpoint.com/java-bufferedwriter-class
 * https://stackoverflow.com/questions/8142573/java-how-to-make-user-friendly-percentage-output-from-float-number
 * https://stackoverflow.com/questions/5090937/do-i-have-to-close-fileinputstream#:~:text=The%20answer%20is%2C%20you%20don,close%20FileInputStream%20no%20matter%20what.&text=Basic%20CompSci%20101%20tell%20us,in%20Java%20or%20any%20language.
 */
package convertimagestobase64;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map;

/**
 *
 * @author Flavio Lima
 */
public class ConvertImagesToBase64 {

    public static final String FILE_WITH_LIST_OF_IMAGES = "C:\\mediaportal\\lowres_images\\images.txt";
    public static final String FOLDER_TO_PUT_LOWRES_IMAGES = "C:\\mediaportal\\lowres_images\\ano\\";
//    public static final String FILE_WITH_LIST_OF_IMAGES = "D:\\Suporte\\SESC_SP\\2021\\images.txt";
//    public static final String FOLDER_TO_PUT_LOWRES_IMAGES = "D:\\Suporte\\SESC_SP\\2021";
    public static final Integer STORAGEID = 20;

    public static File sqlImagesToBeLoaded = new File(FOLDER_TO_PUT_LOWRES_IMAGES, "assetid_base64images.sql");
    public static File sqlMetadataFile = new File(FOLDER_TO_PUT_LOWRES_IMAGES, "images_metadata.sql");
    public static File sqlProxyFile = new File(FOLDER_TO_PUT_LOWRES_IMAGES, "images_proxies.sql");
    public static File shellRemoveFile = new File(FOLDER_TO_PUT_LOWRES_IMAGES, "remove_lowres_images.sh");
    public static File batRemoveFile = new File(FOLDER_TO_PUT_LOWRES_IMAGES, "remove_lowres_images.bat");
    public static File shellScriptsFile = new File(FOLDER_TO_PUT_LOWRES_IMAGES, "scripts.sh");

    public static BufferedWriter sqlMetadata;
    public static BufferedWriter sqlProxy;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        try (BufferedReader sourceFileReader = new BufferedReader(new FileReader(FILE_WITH_LIST_OF_IMAGES));
                BufferedWriter imagesInBase64Writer = new BufferedWriter(new FileWriter(sqlImagesToBeLoaded))) {

            sqlMetadata = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sqlMetadataFile), Charset.forName("ISO-8859-1")));
            sqlMetadata.write("-- Script de inserção de metadados Exif e Iptc das imagens");
            sqlMetadata.newLine();

            sqlProxy = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sqlProxyFile), Charset.forName("ISO-8859-1")));
            sqlProxy.write("-- Script de criação de proxies das imagens");
            sqlProxy.newLine();

            BufferedWriter shellRemove = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(shellRemoveFile), Charset.forName("ISO-8859-1")));
            BufferedWriter batRemove = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(batRemoveFile), Charset.forName("ISO-8859-1")));
            BufferedWriter shellScripts = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(shellScriptsFile), Charset.forName("ISO-8859-1")));

            System.out.println("[DEBUG] Reading file from file: " + FILE_WITH_LIST_OF_IMAGES);
            System.out.println("[DEBUG] Generating lowres images at folder: " + FOLDER_TO_PUT_LOWRES_IMAGES);
            System.out.println();

            String currentFile;
            int counter = 0;
            while ((currentFile = sourceFileReader.readLine()) != null) {
                counter += 1;
                if (currentFile.isEmpty()) {
                    continue;
                }

                System.out.println("[DEBUG] Reading image: " + currentFile);
                File original = new File(currentFile);

                // Checking image assetid
                String assetidStr = getNameWithoutExtension(original.getName());
                try {
                    Integer.parseInt(assetidStr);
                } catch (NumberFormatException ex) {
                    System.out.println("[ERROR] O nome do arquivo não o assetid: " + assetidStr);
                    continue;
                }

                createProxyQuery(assetidStr, original);
                
                extractExifIptcQueries(assetidStr, original, 1);
                extractExifIptcQueries(assetidStr, original, 2);

                String lowResName = generateLowResImage(original);
                if (lowResName == null || lowResName.isEmpty()) {
                    continue;
                }

//                System.out.println("[DEBUG] Reading lowRes image: " + currentFile);
                File lowResImage = new File(FOLDER_TO_PUT_LOWRES_IMAGES, lowResName);
//                String encodeString = encodeFileToBase64Binary(lowResImage);

                System.out.println("[DEBUG] Adding lines for lowres image insertion at database.");
                String createBlobAssetMaster = "UPDATE mc_assetmaster set thumbnail ="
                        + " row('ifx_blob', filetoblob('"
                        + original.getName()
                        + "', 'client')::lld_lob, NULL)::lld_locator"
                        + " WHERE assetid = " + assetidStr + ";";
                imagesInBase64Writer.write(createBlobAssetMaster);
                imagesInBase64Writer.newLine();

                String createBlobImageAsset = "INSERT INTO mi_imageasset"
                        + " ( assetid, preview ) VALUES ( "
                        + assetidStr + ", "
                        + "row('ifx_blob', filetoblob('"
                        + original.getName()
                        + "', 'client')::lld_lob, NULL)::lld_locator );";
                imagesInBase64Writer.write(createBlobImageAsset);
                imagesInBase64Writer.newLine();

                System.out.println("");

                shellRemove.write("rm -f " + original.getName() + ";");
                shellRemove.newLine();
                batRemove.write("del " + original.getName() + ";");
                batRemove.newLine();
            }

            shellScripts.write("dbaccess mediaxp assetid_base64images.sql 1> assetid_base64images.log 2>&1;\n"
                    + "dbaccess mediaxp images_proxies.sql 1> images_proxies.log 2>&1;\n"
                    + "dbaccess mediaxp images_metadata.sql 1> images_metadata.log 2>&1;");
            shellScripts.close();

            if (sourceFileReader != null) {
                sourceFileReader.close();
            }

            imagesInBase64Writer.close();
            sqlMetadata.close();
            sqlProxy.close();
            shellRemove.close();
            batRemove.close();

            System.out.println("Total images: " + counter);
        } catch (IOException ex) {
            System.out.println("[ERROR] Falha ao escrever algum arquivo --- " + ex.getLocalizedMessage());
        }

        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Time spent: " + elapsedTime + "s");
    }

    private static void createProxyQuery(String assetidStr, File image) {
        try {
            System.out.println("[DEBUG] Writing proxy queries for assetid: " + assetidStr);

            // TODO: nome do arquivo em vez de assetid
            String query = "EXECUTE PROCEDURE mx_createproxy ( "
                    + assetidStr + ", 1048 , " + STORAGEID + " ,"
                    + " '" + image.getName() + "',"
                    + " '00:00:00:00', 'NONE', 'informix', 1 );";
            System.out.println("[DEBUG] query: " + query);

            sqlProxy.write(query);
            sqlProxy.newLine();
        } catch (IOException ex) {
            System.out.println("[ERROR] Something went wrong writing proxies sql file.");
        }
    }

    private static String generateLowResImage(File original) throws IOException {
        // Run shell
        System.out.println("[DEBUG] generateLowResImage: " + original.getAbsolutePath());

        ShellImageMagick shell = new ShellImageMagick();

        String[] command = new String[5];
        command[0] = "C:\\mediaportal\\ImageMagick-6.9.3-Q16\\convert";
//        command[0] = "D:\\mediaportal\\ImageMagick-6.9.3-Q16\\convert";
        command[1] = original.getAbsolutePath();
        command[2] = "-resize";
        command[3] = "100x100";
        File destiny = new File(FOLDER_TO_PUT_LOWRES_IMAGES, original.getName());
        command[4] = destiny.getAbsolutePath();

        try {
            shell.runCommand(command);
        } catch (IOException ex) {
            System.out.println("[ERROR] " + ex.getLocalizedMessage());
            return null;
        }

        return original.getName();
    }

    private static String encodeFileToBase64Binary(File file) throws IOException {
        String encodedfile = null;
        if (file.exists()) {
            try (FileInputStream fileInputStreamReader = new FileInputStream(file)) {
                byte[] bytes = new byte[(int) file.length()];
                fileInputStreamReader.read(bytes);
                encodedfile = Base64.getEncoder().encodeToString(bytes);
            } catch (FileNotFoundException ex) {
                System.out.println("[ERROR] " + ex.getMessage());
            }

            return encodedfile;
        }

        return "";
    }

    private static void extractExifIptcQueries(String assetid, File original, Integer option) {
        // Run shell
        System.out.println("[DEBUG] extractExifIptcQueries: " + original.getAbsolutePath());

        ShellDiva exiv2shell = new ShellDiva();
//        ShellImageExifIptc shell = new ShellImageExifIptc();
        String[] command = new String[3];
        Integer strataid;

        command[0] = "C:\\mediaportal\\exiv2\\exiv2";
//        command[0] = "D:\\mediaportal\\exiv2\\exiv2";
        switch (option) {
            case 1: //EXIF
                // C:\mediaportal\exiv2\exiv2 -PE C:/mediaportal/files/VOD/33-26.png
                command[1] = "-PE";
                strataid = 20042;
                break;
            case 2: //IPTC
                // C:\mediaportal\exiv2\exiv2 -PI C:/mediaportal/files/VOD/33-27.png 
                command[1] = "-PI";
                strataid = 20004;
                break;
            default:
                return;
        }
        command[2] = original.getAbsolutePath();

        Map<String, String> data = null;
        CommandOutput commandOutput = new CommandOutput();
        try {
            exiv2shell.runCommand(command, commandOutput);
        } catch (Exception ex) {
            System.out.println("[ERROR] " + ex.getLocalizedMessage());
        }

        ShellImageExifIptc converter = new ShellImageExifIptc();

        data = converter.addExifIPTCMetadata(commandOutput.getOutput(), option);

        writeMetadataInsertion(assetid, data, strataid);
    }

    private static String getNameWithoutExtension(String name) {
        return name.replaceAll("(.+?)(\\.[^.]*$|$)", "$1");
    }

    private static void writeMetadataInsertion(String assetid, Map<String, String> data, Integer strataid) {
        System.out.println("[DEBUG] Writing metadata queries for assetid: " + assetid);
        if (data != null && !data.isEmpty()) {
            data.entrySet().stream().forEach(entry -> {
                try {
                    String query = "INSERT INTO bmm_metadados (assetid, strataid,"
                            + " description, qualification, tipo_alteracao,"
                            + " data_alteracao, userid) VALUES ("
                            + assetid + "," + strataid + ","
                            + "'" + entry.getValue() + "',"
                            + "'" + entry.getKey() + "',"
                            + "1, current, 2);";
                    System.out.println("[DEBUG] query: " + query);
                    sqlMetadata.write(query);
                    sqlMetadata.newLine();
                } catch (IOException ex) {
                    System.out.println("[ERROR] Falha ao escrever query para o assetid: " + assetid);
                }
            });
        }
    }

}
