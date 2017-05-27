package ua.alcash;

/**
 * Created by Al.Cash on 5/8/17.
 */
public class TestCase {
    private final static String UNKNOWN_KEY = "UNKNOWN";
    private final static String SKIPPED_KEY = "SKIPPED";
    private final static String RUNNING_KEY = "RUNNING";

    private String name;
    private String input = "";
    private String expectedOutput = "";
    private String programOutput = "";
    private String[] executionResults = new String[] {UNKNOWN_KEY};

    public TestCase(String name) { this.name = name; }

    public TestCase(String name, String input, String output) {
        this.name = name;
        this.input = input;
        this.expectedOutput = output;
    }

    public String getName() { return name; }

    public String getInput() { return input; }
    public void setInput(String value) { input = value; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String value) { expectedOutput = value; }

    public String getProgramOutput() { return programOutput; }
    public void setProgramOutput(String value) { programOutput = value; }

    public String getExecutionResults(String delimiter) { return String.join(delimiter, executionResults); }
    public void setExecutionResults(String value) { executionResults = value.split(" ", 2); }

    public void flipSkipped() {
        if (executionResults.length == 1 && executionResults[0].equals(SKIPPED_KEY)) {
            executionResults[0] = UNKNOWN_KEY;
        } else {
            executionResults = new String[] {SKIPPED_KEY};
        }
    }

    public boolean isRunning() { return executionResults.length == 1 && executionResults[0].equals(RUNNING_KEY); }
}
