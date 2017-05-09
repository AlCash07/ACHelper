package ua.alcash;

import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import net.egork.chelper.task.StreamConfiguration;
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

    StreamConfiguration input;
    StreamConfiguration output;

    TestType testType;
    double timeLimit = Double.parseDouble(Configuration.get("default time limit"));
    ArrayList<TestCase> testCases = new ArrayList<>();

    boolean isInteractive = false;
    boolean customChecker = false;
    String checkerPreprocessorDirectives = Configuration.get("checker default preprocessor directives");

    String directory;

    public Problem(String problemId, String problemName, String platformName, String contestName) {
        this.problemId = problemId;
        this.problemName = problemName;
        this.platformName = platformName;
        this.contestName = contestName;

        this.input = StreamConfiguration.STANDARD;
        this.output = StreamConfiguration.STANDARD;

        this.testType = TestType.SINGLE;

        this.setDirectory();
    }

    public Problem(String platformName, Task task) {
        this.problemId = task.taskClass;
        if (this.problemId.startsWith("Task")) {
            this.problemId = this.problemId.substring(4);
        }
        this.problemName = task.name;
        this.platformName = platformName;
        this.contestName = task.contestName;

        this.input = task.input;
        this.output = task.output;
        // this condition holds only for GCJ and FHC
        if (this.input.type == StreamConfiguration.StreamType.LOCAL_REGEXP) {
            // use standard files, because the program is executed only locally
            this.input = StreamConfiguration.STANDARD;
            this.output = StreamConfiguration.STANDARD;
        }

        this.testType = task.testType;

        String testName = Configuration.get("test sample");
        for (int i = 0; i < task.tests.length; ++i) {
            this.testCases.add(new TestCase(testName + (i + 1), task.tests[i].input, task.tests[i].output));
        }

        this.setDirectory();
    }

    private void setDirectory() {
        String[] tokens = Configuration.get("problem directory").split("%");
        for (int i = 1; i < tokens.length; i += 2) {
            switch (tokens[i]) {
                case "platform_name":
                    tokens[i] = this.platformName;
                    break;
                case "platform_id":
                    tokens[i] = ParseManager.getPlatformId(this.platformName);
                    break;
                case "problem_id":
                    tokens[i] = this.problemId;
                    break;
                case "problem_name":
                    tokens[i] = this.problemName;
                    break;
                default:
                    tokens[i] = Configuration.get(tokens[i]);
            }
        }
        this.directory = String.join("", tokens);
    }

    public String getProblemId() {
        return this.problemId;
    }

    public String getProblemName() {
        return this.problemName;
    }

    public String getContestName() {
        return this.contestName;
    }

    public String getFullName() {
        String name = (this.problemName != null && !this.problemName.isEmpty()) ? this.problemName : this.problemId;
        return this.contestName + " " + name;
    }

    public void addTestCase(TestCase testCase) {
        testCases.add(testCase);
    }

    public TestCase getTestCase(int index) {
        return testCases.get(index);
    }

    public void deleteTestCase(int index) {
        testCases.remove(index);
    }

    public ArrayList<TestCase> getTestCaseSet() {
        return testCases;
    }

    public String getDirectory() {
        return this.directory;
    }
}
