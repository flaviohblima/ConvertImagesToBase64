package convertimagestobase64;

import java.io.InputStream;

public class ShellStreamGlobber extends Thread {

    protected InputStream is;
    protected StringBuffer sb;
    protected String type;
    protected CommandOutput commandOutput;
    protected Float progressOffset;
    protected Float progressMaximum;

    public ShellStreamGlobber(InputStream is, StringBuffer sb, String type, CommandOutput commandOutput) {
        this.is = is;
        this.sb = sb;
        this.type = type;
        this.commandOutput = commandOutput;
        progressOffset = 0.0f;
        progressMaximum = 100.0f;
    }

    public void setProgressLimits(Float progressOffset, Float progressMaximum) {
        this.progressOffset = progressOffset;
        this.progressMaximum = progressMaximum;
    }
}
