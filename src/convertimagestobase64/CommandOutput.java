package convertimagestobase64;

import java.util.ArrayList;
import java.util.List;

public class CommandOutput {

    private final List<String> output = new ArrayList<>();

    public void addOutputLine(String str) {
        output.add(str);
    }

    public void clearOutput() {
        output.clear();
    }

    public List<String> getOutput() {
        return output;
    }

}
