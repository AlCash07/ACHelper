package ua.alcash;

/**
 * Created by oleksandr.bacherikov on 5/8/17.
 */
public class TestCase {
    String name;
    String input = "";
    String expectedOutput = "";
    String programOutput = "";
    String[] executionResults = new String[] {"UNKNOWN"};

    public TestCase(String name) {
        this.name = name;
    }

    public TestCase(String name, String input, String output) {
        this.name = name;
        this.input = input;
        this.expectedOutput = output;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public String getProgramOutput() {
        return programOutput;
    }

    public void setProgramOutput(String programOutput) {
        this.programOutput = programOutput;
    }

    public String getExecutionResults(String delimiter) {
        return String.join(delimiter, this.executionResults);
    }

    public void setExecutionResults(String result) {
        this.executionResults = result.split(" ", 3);
    }
}
