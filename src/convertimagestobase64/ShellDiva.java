package convertimagestobase64;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Captura a sa�da do shell e joga em buffer sem fazer nenhum tratamento Usada
 * para executar comandos do Diva e ImageMagick
 *
 * @author Jos� San Pedro
 */
public class ShellDiva extends Shell {

    @Override
    protected ShellStreamGlobber getStreamGlobberError(InputStream is, StringBuffer sb, CommandOutput commandOutput) {
        return new StreamGobblerDiva(is, sb, "ERROR", commandOutput);
    }

    @Override
    protected ShellStreamGlobber getStreamGlobberOutput(InputStream is, StringBuffer sb, CommandOutput commandOutput) {
        return new StreamGobblerDiva(is, sb, "OUTPUT", commandOutput);
    }

    class StreamGobblerDiva extends ShellStreamGlobber {

        public StreamGobblerDiva(InputStream is, StringBuffer sb, String type, CommandOutput commandOutput) {
            super(is, sb, type, commandOutput);
        }

        @Override
        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;

                while ((line = br.readLine()) != null) {
                    commandOutput.addOutputLine(line);

                    if (type.equals("OUTPUT")) {
                        if (line.contains("Invalid argument")) {
                            this.interrupt();
                        }
                    }
                }
            } catch (IOException ioe) {
                System.out.println(type + " - IOException: " + ioe.getMessage());
            }
        }
    }

}
