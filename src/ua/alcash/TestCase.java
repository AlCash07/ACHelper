package ua.alcash;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by oleksandr.bacherikov on 5/8/17.
 */
public class TestCase {
    private final static String UNKNOWN_KEY = "UNKNOWN";
    private final static String SKIPPED_KEY = "SKIPPED";

    private static String inputExtension;
    private static String expectedOutputExtension;
    private static String programOutputExtension;

    public static void configure() {
        inputExtension = "." + Configuration.getExtension("input");
        expectedOutputExtension = "." + Configuration.getExtension("expected output");
        programOutputExtension = "." + Configuration.getExtension("program output");
    }

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
        modifiedInput = true;
        modifiedOutput = true;
    }

    public String getName() { return name; }

    public String getInput() { return input; }

    public void setInput(String value) {
        input = value;
        modifiedInput = true;
    }

    public String getExpectedOutput() { return solved ? programOutput : expectedOutput; }

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

    public void setExecutionResults(String value) { executionResults = value.split(" ", 2); }

    public void flipSkipped() { skipped = !skipped; }

    public boolean getSolved() { return solved; }

    public void flipSolved() { solved = !solved; }

    public void writeToDisk(String problemPath) throws IOException {
        Charset utf8 = StandardCharsets.UTF_8;
        if (modifiedInput) {
            Path inputFile = Paths.get(problemPath, name + inputExtension);
            Files.write(inputFile, Arrays.asList(input), utf8);
            modifiedInput = false;
        }
        Path expectedOutputFile = Paths.get(problemPath, name + expectedOutputExtension);
        Path programOutputFile = Paths.get(problemPath, name + programOutputExtension);
        if (!Files.exists(programOutputFile)) {
            Files.createFile(programOutputFile);
        }
        if (solved) {
            Files.copy(programOutputFile, expectedOutputFile);
            solved = false;
        } else if (modifiedOutput) {
            Files.write(expectedOutputFile, Arrays.asList(expectedOutput), utf8);
        }
        modifiedOutput = false;
        skipped = false;
    }

    public void deleteFromDisk(String problemPath) throws IOException {
        Files.delete(Paths.get(problemPath, name + inputExtension));
        Files.delete(Paths.get(problemPath, name + expectedOutputExtension));
        Files.delete(Paths.get(problemPath, name + programOutputExtension));
    }
}
