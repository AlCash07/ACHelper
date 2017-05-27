package ua.alcash;

import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import ua.alcash.parsing.ParseManager;

import java.util.ArrayList;

/**
 * Created by Al.Cash on 5/8/17.
 */
public class Problem {
    private static double defaultTimeLimit;
    private static double defaultMemoryLimit;
    private static String defaultCheckerParams;
    private static String sampleTestName;

    static public void configure() {
        defaultTimeLimit = Double.parseDouble(Configuration.get("default time limit"));
        defaultMemoryLimit = Double.parseDouble(Configuration.get("default memory limit"));
        defaultCheckerParams = Configuration.get("default checker compiler options");
        sampleTestName = Configuration.get("test sample");
    }

    private String problemId;
    private String problemName;
    private String platformId;
    private String contestName;

    private double timeLimit = defaultTimeLimit;
    private double memoryLimit = defaultMemoryLimit;

    private String inputFile = "";
    private String outputFile = "";

    private TestType testType = TestType.SINGLE;

    private boolean interactive = false;
    private boolean customChecker = false;
    private String checkerParams = defaultCheckerParams;

    private ArrayList<TestCase> testCases = new ArrayList<>();

    private static final int timeLimitBit = 0;
    private static final int memoryLimitBit = 1;
    private static final int inputFileBit = 2;
    private static final int outputFileBit = 3;
    private static final int testTypeBit = 4;
    private static final int interactiveBit = 5;
    private static final int customCheckerBit = 6;
    private static final int checkerParamsBit = 7;

    private int changesMask = (1 << 8) - 1;

    public Problem(String problemId, String problemName, String platformId, String contestName) {
        this.problemId = problemId;
        this.problemName = problemName;
        this.platformId = platformId;
        this.contestName = contestName;
    }

    public Problem(String platformId, Task task) {
        problemId = task.taskClass;
        if (problemId.startsWith("Task")) {
            problemId = problemId.substring(4);
        }
        problemName = task.name;
        this.platformId = platformId;
        contestName = task.contestName;
        if (contestName == null) contestName = "";

        // for GCJ and FHC ignore the file name, because the program is executed only locally
        if (task.input.type != StreamConfiguration.StreamType.LOCAL_REGEXP) {
            if (task.input.fileName != null) inputFile = task.input.fileName;
            if (task.output.fileName != null) outputFile = task.output.fileName;
        }
        if (task.testType != null) testType = task.testType;

        for (int i = 0; i < task.tests.length; ++i) {
            String testName = sampleTestName + (i + 1);
            testCases.add(new TestCase(testName, task.tests[i].input, task.tests[i].output));
        }
    }

    public String getValue(String key, boolean nameOnly) {
        switch (key) {
            case "problem_id":
                return problemId;
            case "problem_name":
                return problemName;
            case "platform_id":
                return platformId;
            case "platform_name":
                return ParseManager.getPlatformName(platformId);
            case "contest_name":
                return contestName;
        }
        if (nameOnly) return Configuration.get(key);
        switch (key) {
            case "time_limit":
                return String.valueOf(timeLimit);
            case "memory_limit":
                return String.valueOf(memoryLimit);
            case "input_file_name":
                return inputFile;
            case "output_file_name":
                return outputFile;
            case "test_type":
                switch (testType) {
                    case SINGLE:
                        return "single";
                    case MULTI_NUMBER:
                        return "multi_number";
                    case MULTI_EOF:
                        return "multi_eof";
                }
            case "interactive":
                return String.valueOf(interactive);
            case "custom_checker":
                return String.valueOf(customChecker);
            case "checker_compiler_options":
                return checkerParams;
            case "time_limit_changed":
                return String.valueOf((changesMask >> timeLimitBit & 1) > 0);
            case "memory_limit_changed":
                return String.valueOf((changesMask >> memoryLimitBit & 1) > 0);
            case "input_file_name_changed":
                return String.valueOf((changesMask >> inputFileBit & 1) > 0);
            case "output_file_name_changed":
                return String.valueOf((changesMask >> outputFileBit & 1) > 0);
            case "test_type_changed":
                return String.valueOf((changesMask >> testTypeBit & 1) > 0);
            case "interactive_changed":
                return String.valueOf((changesMask >> interactiveBit & 1) > 0);
            case "custom_checker_changed":
                return String.valueOf((changesMask >> customCheckerBit & 1) > 0);
            case "checker_compiler_options_changed":
                return String.valueOf((changesMask >> checkerParamsBit & 1) > 0);
            default:
                return Configuration.get(key);
        }
    }

    public String getId() { return problemId; }

    public String getFullName() {
        String name = (problemName != null && !problemName.isEmpty()) ? problemName : problemId;
        return contestName + " " + name;
    }

    public double getTimeLimit() { return timeLimit; }
    public void setTimeLimit(double value) {
        if (timeLimit != value) changesMask |= 1 << timeLimitBit;
        timeLimit = value;
    }

    public double getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(double value) {
        if (memoryLimit != value) changesMask |= 1 << memoryLimitBit;
        memoryLimit = value;
    }

    public String getInputFile() { return inputFile; }
    public void setInputFile(String value) {
        if (!inputFile.equals(value)) changesMask |= 1 << inputFileBit;
        inputFile = value;
    }

    public String getOutputFile() { return outputFile; }
    public void setOutputFile(String value) {
        if (!outputFile.equals(value)) changesMask |= 1 << outputFileBit;
        outputFile = value;
    }

    public TestType getTestType() { return testType; }
    public void setTestType(TestType value) {
        if (testType != value) changesMask |= 1 << testTypeBit;
        testType = value;
    }

    public boolean getInteractive() { return interactive; }
    public void setInteractive(boolean value) {
        if (interactive != value) changesMask |= 1 << interactiveBit;
        interactive = value;
    }

    public boolean getCustomChecker() { return customChecker; }
    public void setCustomChecker(boolean value) {
        if (customChecker != value) changesMask |= 1 << customCheckerBit;
        customChecker = value;
    }

    public String getCheckerParams() { return checkerParams; }
    public void setCheckerParams(String value) {
        if (!checkerParams.equals(value)) changesMask |= 1 << checkerParamsBit;
        checkerParams = value;
    }

    public ArrayList<TestCase> getTestCaseSet() { return testCases; }

    public boolean projectRegenerationRequired() {
        return (changesMask & (1 << interactiveBit | 1 << customCheckerBit)) > 0
                || (customChecker && (changesMask & (1 << checkerParamsBit)) > 0);
    }

    public void markUnchanged() { changesMask = 0; }
}
