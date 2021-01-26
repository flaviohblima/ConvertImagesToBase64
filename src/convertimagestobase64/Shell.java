package convertimagestobase64;

import java.io.InputStream;

public abstract class Shell {

    protected abstract ShellStreamGlobber getStreamGlobberError(InputStream is, StringBuffer sb, CommandOutput commandOutput);

    protected abstract ShellStreamGlobber getStreamGlobberOutput(InputStream is, StringBuffer sb, CommandOutput commandOutput);

    public void runCommand(String[] command, CommandOutput commandOutput) throws Exception {
        runCommand(command, commandOutput, 0.0f, 100.0f);
    }

    public void runCommand(String[] command, CommandOutput commandOutput, Float initProgress, Float endProgress) throws Exception {
        StringBuffer sb_pis = new StringBuffer();
        StringBuffer sb_pes = new StringBuffer();
        String cmdStr = "";
        for (String str : command) {
            cmdStr += str + " ";
        }

        System.out.println("[DEBUG] runing Command: " + cmdStr);

        long time = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(command);
        Process pr = pb.start();
        InputStream pis = pr.getInputStream();
        InputStream pes = pr.getErrorStream();

        // any error message?
        ShellStreamGlobber errorGobbler = getStreamGlobberError(pes, sb_pes, commandOutput);
        // any output?
        ShellStreamGlobber outputGobbler = getStreamGlobberOutput(pis, sb_pis, commandOutput);
        // set progress limits
        errorGobbler.setProgressLimits(initProgress, endProgress);
        outputGobbler.setProgressLimits(initProgress, endProgress);
        // kick them off
        errorGobbler.start();
        outputGobbler.start();

        // any error???
        int retVal = pr.waitFor();
        Thread.sleep(2000);

        while (errorGobbler.isAlive() && outputGobbler.isAlive()) {
            Thread.sleep(2000);
        }

        pis.close();
        pes.close();
        pr.destroy();

        if (retVal == 1) {

        }
    }

}
