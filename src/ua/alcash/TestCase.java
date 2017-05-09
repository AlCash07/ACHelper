package ua.alcash;

/**
 * Created by oleksandr.bacherikov on 5/8/17.
 */
public class TestCase {
    private static String UNKNOWN_KEY = "UNKNOWN";
    private static String SKIPPED_KEY = "SKIPPED";

    String name;
    String input = "";
    String expectedOutput = "";
    String programOutput = "";
    String[] executionResults = new String[] {UNKNOWN_KEY};

    boolean skipped = false;
    boolean modifiedInput = false;
    boolean modifiedOutput = false;
    boolean solved = false;

    public TestCase(String name) { this.name = name; }

    public TestCase(String name, String input, String output) {
        this.name = name;
        this.input = input;
        this.expectedOutput = output;
    }

    public String getInput() { return input; }

    public void setInput(String value) {
        input = value;
        modifiedInput = true;
    }

    public String getExpectedOutput() {
        return solved ? programOutput : expectedOutput;
    }

    public void setExpectedOutput(String value) {
        expectedOutput = value;
        modifiedOutput = true;
    }

    public String getProgramOutput() { return programOutput; }

    public void setProgramOutput(String value) { programOutput = value; }

    public String getExecutionResults(String delimiter) {
        if (skipped) {
            return SKIPPED_KEY;
        }
        if (executionResults.length == 1 && executionResults[0] == SKIPPED_KEY) {
            return UNKNOWN_KEY;
        }
        return String.join(delimiter, executionResults);
    }

//    public void setExecutionResults(String value) { executionResults = value.split(" ", 3); }

    public void flipSkipped() { skipped = !skipped; }

    public void flipSolved() { solved = !solved; }
}
