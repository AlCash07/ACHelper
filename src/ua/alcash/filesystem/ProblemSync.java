package ua.alcash.filesystem;

import com.google.common.collect.Lists;
import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.TestCase;
import ua.alcash.ui.TestsTableModel;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by oleksandr.bacherikov on 5/25/17.
 */
public class ProblemSync {
    private static String testListFileName;
    private static String inputExtension;
    private static String expectedOutputExtension;
    private static String programOutputExtension;
    private static String manualTestName;

    private Problem problem;
    private String directory;

    private ArrayList<TestCase> testCases;
    private Set<String> testCaseNames = new HashSet<>();
    private int manualTestIndex = 1;

    private TestsTableModel testsTableModel;  // used to notify testsTable about the testCaseSet changes
    private boolean testSetChanged = true;

    ProblemSync(String workspaceDirectory, Problem problem) {
        this.problem = problem;
        directory = Paths.get(workspaceDirectory,
                substituteKeys(Configuration.get("problem directory"), true)).toString();
        testCases = problem.getTestCaseSet();
        testsTableModel = new TestsTableModel(testCases);
        for (TestCase testCase : testCases) {
            testCaseNames.add(testCase.getName());
        }
    }

    void initialize() throws IOException {
        Files.createDirectories(Paths.get(directory));
        // look for existing tests
        for (int i = 0; i < testCases.size(); ++i) {
            createTestCaseFiles(i);
        }
        testsListChanged();
    }

    public static void configure() {
        Problem.configure();
        testListFileName = Configuration.get("test list file");
        inputExtension = "." + Configuration.getExtension("input");
        expectedOutputExtension = "." + Configuration.getExtension("expected output");
        programOutputExtension = "." + Configuration.getExtension("program output");
        manualTestName = Configuration.get("test manual");
    }

    public Problem getProblem() { return problem; }

    public String getDirectory() { return directory; }

    public AbstractTableModel getTableModel() { return testsTableModel; }

    public TestCase getTestCase(int index) { return testCases.get(index); }

    public String getNextTestName() {
        while (testCaseNames.contains(manualTestName + manualTestIndex)) {
            ++manualTestIndex;
        }
        return manualTestName + manualTestIndex;
    }

    public void addTestCase(TestCase testCase) throws IOException {
        testSetChanged = true;
        int index = testCases.size();
        testCases.add(testCase);
        testCaseNames.add(testCase.getName());
        createTestCaseFiles(index);
        testsListChanged();
        testsTableModel.rowInserted();
    }

    public void testInputChanged(int index) throws IOException {
        Path inputFile = Paths.get(directory, testCases.get(index).getName() + inputExtension);
        Files.write(inputFile, testCases.get(index).getInput().getBytes());
        testsTableModel.rowUpdated(index);
    }

    public void testAnswerChanged(int index) throws IOException {
        Path answerFile = Paths.get(directory, testCases.get(index).getName() + expectedOutputExtension);
        Files.write(answerFile, testCases.get(index).getExpectedOutput().getBytes());
        testsTableModel.rowUpdated(index);
    }

    private void createTestCaseFiles(int index) throws IOException {
        testInputChanged(index);
        testAnswerChanged(index);
        Path outputFile = Paths.get(directory, testCases.get(index).getName() + programOutputExtension);
        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }
    }

    public void setTestSolved(int index) throws IOException {
        TestCase testCase = testCases.get(index);
        testCase.setExpectedOutput(testCase.getProgramOutput());
        String name = testCase.getName();
        Files.copy(Paths.get(directory, name + programOutputExtension),
                Paths.get(directory, name + expectedOutputExtension));
        testsTableModel.rowUpdated(index);
    }

    public void flipTestSkipped(int index) throws IOException {
        testCases.get(index).flipSkipped();
        testsListChanged();
        testsTableModel.rowUpdated(index);
    }

    public void swapTestCases(int index1, int index2) throws IOException {
        Collections.swap(testCases, index1, index2);
        testsListChanged();
        testsTableModel.rowUpdated(index1);
        testsTableModel.rowUpdated(index2);
    }

    public void deleteTestCase(int index) throws IOException {
        testSetChanged = true;
        String name = testCases.get(index).getName();
        Files.delete(Paths.get(directory, name + inputExtension));
        Files.delete(Paths.get(directory, name + expectedOutputExtension));
        Files.delete(Paths.get(directory, name + programOutputExtension));
        testCaseNames.remove(name);
        testCases.remove(index);
        testsListChanged();
        testsTableModel.rowDeleted(index);
    }

    private void testsListChanged() throws IOException {
        Charset utf8 = StandardCharsets.UTF_8;
        Path testListFile = Paths.get(directory, testListFileName);
        Files.write(testListFile, Lists.transform(testCases,
                testCase -> String.join(" ", testCase.getName(), testCase.getExecutionResults(" "))),
                utf8);
    }

//        if (Files.exists(testListFile)) {
//            Stream<String> lines = Files.lines(testListFile, utf8);
//            if (lines.anyMatch(line -> {
//                String[] tokens = line.split(" ");
//                return tokens.length > 1 && (tokens[1].equals("RUNNING") || tokens[1].equals("PENDING"));
//            })) {
//                throw new IOException("Cannot write tests to disk while testing is in progress.");
//            }
//        }

    void deleteFromDisk() throws IOException {
        Files.walkFileTree(Paths.get(directory), new SimpleFileVisitor<Path>() {
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

    String substituteKeys(String input, boolean namesOnly) {
        String[] tokens = input.split("@");
        for (int i = 1; i < tokens.length; i += 2) {
            tokens[i] = tokens[i].equals("problem_dir") ? directory : problem.getValue(tokens[i], namesOnly);
        }
        return String.join("", tokens);
    }

    boolean projectRegenerationRequired() {
        return problem.projectRegenerationRequired() || testSetChanged;
    }

    void markUnchanged() {
        problem.markUnchanged();
        testSetChanged = false;
    }
}
