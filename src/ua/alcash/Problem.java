package ua.alcash;

import com.google.common.collect.Lists;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import ua.alcash.util.ParseManager;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

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
    Set<String> testCaseNames = new HashSet<>();
    int manualTestIndex = 1;

    boolean interactive = false;
    boolean customChecker = false;
    String checkerParams = Configuration.get("default checker preprocessor directives");

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
            testCaseNames.add(testName + (i + 1));
            testCases.add(new TestCase(testName + (i + 1), task.tests[i].input, task.tests[i].output));
        }

        setDirectory();
    }

    private void setDirectory() {
        if (Configuration.get("test sample") == Configuration.get("test manual")) {
            manualTestIndex = testCases.size() + 1;
        }
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

    public String getNextTestName() {
        String testName = Configuration.get("test manual");
        while (testCaseNames.contains(testName + manualTestIndex)) {
            ++manualTestIndex;
        }
        return testName + manualTestIndex;
    }

    public TestCase getTestCase(int index) { return testCases.get(index); }

    public void addTestCase(TestCase testCase) {
        testCases.add(testCase);
        testCaseNames.add(testCase.getName());
    }

    public void swapTestCases(int index1, int index2) {
        Collections.swap(testCases, index1, index2);
    }

    public void deleteTestCase(int index) {
        testCaseNames.remove(testCases.get(index).getName());
        testCases.remove(index);
    }

    public ArrayList<TestCase> getTestCaseSet() { return testCases; }

    public String getDirectory() { return directory; }

    public void writeToDisk(String workspaceDirectory) throws IOException {
        Charset utf8 = StandardCharsets.UTF_8;
        Path problemPath = Paths.get(workspaceDirectory, directory);
        Files.createDirectories(problemPath);
        Path testListFile = Paths.get(problemPath.toString(), Configuration.get("test list file"));
        if (Files.exists(testListFile)) {
            Stream<String> lines = Files.lines(testListFile, utf8);
            if (lines.anyMatch(line -> {
                String[] tokens = line.split(" ");
                return tokens.length > 1 && (tokens[1] == "RUNNING" || tokens[1] == "PENDING");
            })) {
                throw new IOException("Cannot write tests to disk while testing is in progress.");
            }
        }
        Files.write(testListFile, Lists.transform(testCases,
                testCase -> String.join(" ", testCase.getName(), testCase.getExecutionResults(" "))),
                utf8);
        for (TestCase testCase : testCases) {
            testCase.writeToDisk(problemPath.toString());
        }
    }

    public void deleteFromDisk(String workspaceDirectory) throws IOException {
        Files.walkFileTree(Paths.get(workspaceDirectory, directory), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
