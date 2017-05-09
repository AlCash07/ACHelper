package ua.alcash;

import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import ua.alcash.util.ParseManager;

import java.util.ArrayList;

/**
 * Created by oleksandr.bacherikov on 5/8/17.
 */
public class Problem {
    String problemId;
    String problemName;
    String platformName;
    String contestName;

    String inputFile;
    String outputFile;

    TestType testType;
    double timeLimit = Double.parseDouble(Configuration.get("default time limit"));
    ArrayList<TestCase> testCases = new ArrayList<>();

    boolean interactive = false;
    boolean customChecker = false;
    String checkerParams = Configuration.get("checker default preprocessor directives");

    String directory;

    public Problem(String problemId, String problemName, String platformName, String contestName) {
        this.problemId = problemId;
        this.problemName = problemName;
        this.platformName = platformName;
        this.contestName = contestName;

        inputFile = "";
        outputFile = "";

        testType = TestType.SINGLE;

        setDirectory();
    }

    public Problem(String platformName, Task task) {
        problemId = task.taskClass;
        if (problemId.startsWith("Task")) {
            problemId = problemId.substring(4);
        }
        problemName = task.name;
        this.platformName = platformName;
        contestName = task.contestName;

        // for GCJ and FHC ignore the file name, because the program is executed only locally
        if (task.input.type != StreamConfiguration.StreamType.LOCAL_REGEXP) {
            inputFile = task.input.fileName;
            outputFile = task.input.fileName;
        }
        if (inputFile == null) inputFile = "";
        if (outputFile == null) outputFile = "";

        testType = task.testType;

        String testName = Configuration.get("test sample");
        for (int i = 0; i < task.tests.length; ++i) {
            testCases.add(new TestCase(testName + (i + 1), task.tests[i].input, task.tests[i].output));
        }

        setDirectory();
    }

    private void setDirectory() {
        String[] tokens = Configuration.get("problem directory").split("%");
        for (int i = 1; i < tokens.length; i += 2) {
            switch (tokens[i]) {
                case "platform_name":
                    tokens[i] = platformName;
                    break;
                case "platform_id":
                    tokens[i] = ParseManager.getPlatformId(platformName);
                    break;
                case "problem_id":
                    tokens[i] = problemId;
                    break;
                case "problem_name":
                    tokens[i] = problemName;
                    break;
                default:
                    tokens[i] = Configuration.get(tokens[i]);
            }
        }
        directory = String.join("", tokens);
    }

    public String getProblemId() { return problemId; }

    public String getProblemName() { return problemName; }

    public String getContestName() { return contestName; }

    public String getFullName() {
        String name = (problemName != null && !problemName.isEmpty()) ? problemName : problemId;
        return contestName + " " + name;
    }

    public TestType getTestType() { return testType; }
    public void setTestType(TestType value) { testType = value; }

    public double getTimeLimit() { return timeLimit; }
    public void setTimeLimit(double value) { timeLimit = value; }

    public String getInputFile() { return inputFile; }
    public void setInputFile(String value) { inputFile = value; }

    public String getOutputFile() { return outputFile; }
    public void setOutputFile(String value) { outputFile = value; }

    public boolean getInteractive() { return interactive; }
    public void setInteractive(boolean value) { interactive = value; }

    public boolean getCustomChecker() { return customChecker; }
    public void setCustomChecker(boolean value) { customChecker = value; }

    public String getCheckerParams() { return checkerParams; }
    public void setCheckerParams(String value) { checkerParams = value; }

    public TestCase getTestCase(int index) { return testCases.get(index); }

    public void addTestCase(TestCase testCase) { testCases.add(testCase); }

    public void deleteTestCase(int index) { testCases.remove(index); }

    public ArrayList<TestCase> getTestCaseSet() { return testCases; }

    public String getDirectory() { return directory; }

/*
    private void saveTests() {
        ArrayList<TestCase> tests = problem.getTestCaseSet();
        PrintWriter writer;
        int testIndex = 1;
        String problemDirectory = new File("problem dir").getParent() + java.io.File.separator;
        for (TestCase test : tests) {
            try {
                writer = new PrintWriter(problemDirectory + testIndex + ".in", "UTF-8");
                writer.println(test.getInput());
                writer.close();

                writer = new PrintWriter(problemDirectory + testIndex + ".out", "UTF-8");
                writer.println(test.getExpectedOutput());
                writer.close();
            } catch (FileNotFoundException | UnsupportedEncodingException ex) {
                JOptionPane.showMessageDialog(this, "Error while saving inputs/outputs.\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
//                Logger.getLogger(ProblemJPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            testIndex++;
        }
        JOptionPane.showMessageDialog(this, "Inputs and outputs saved succesfully.", "Tests saved", JOptionPane.INFORMATION_MESSAGE);
    }

    */
}
