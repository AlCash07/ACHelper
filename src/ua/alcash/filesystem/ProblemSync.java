package ua.alcash.filesystem;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.TestCase;
import ua.alcash.ui.TestsTableModel;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
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
    private static int maxLength;

    private Problem problem;
    private String directory;

    private ArrayList<TestCase> testCases;
    private Set<String> testCaseNames = new HashSet<>();
    private int manualTestIndex = 1;

    private TestsTableModel testsTableModel;  // used to notify testsTable about the testCaseSet changes
    private boolean testSetChanged = true;

    private final Set<String> writtenFiles = new HashSet<>();

    private boolean testsAreRunning = false;

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
        Path problemPath = Paths.get(directory);
        Files.createDirectories(problemPath);
        Files.walkFileTree(problemPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println(file.toString());
                return FileVisitResult.CONTINUE;
            }
        });
        for (int i = 0; i < testCases.size(); ++i) {
            createTestCaseFiles(i);
        }
        testsListChanged();
    }

    static void configure() {
        Problem.configure();
        testListFileName = Configuration.get("test list file");
        inputExtension = "." + Configuration.getExtension("input");
        expectedOutputExtension = "." + Configuration.getExtension("expected output");
        programOutputExtension = "." + Configuration.getExtension("program output");
        manualTestName = Configuration.get("test manual");
        maxLength = Integer.parseInt(Configuration.get("test maximum loaded length"));
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
        checkNotRunning();
        testSetChanged = true;
        int index = testCases.size();
        testCases.add(testCase);
        testCaseNames.add(testCase.getName());
        createTestCaseFiles(index);
        testsListChanged();
        testsTableModel.rowInserted();
    }

    private void writeToFile(String fileName, String data) throws IOException {
        synchronized (writtenFiles) {
            writtenFiles.add(fileName);
        }
        Path inputFile = Paths.get(directory, fileName);
        Files.write(inputFile, data.getBytes());
    }

    public void testInputChanged(int index) throws IOException {
        testsTableModel.rowUpdated(index);
        writeToFile(testCases.get(index).getName() + inputExtension, testCases.get(index).getInput());
    }

    public void testAnswerChanged(int index) throws IOException {
        testsTableModel.rowUpdated(index);
        writeToFile(testCases.get(index).getName() + expectedOutputExtension,
                testCases.get(index).getExpectedOutput());
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
        testsTableModel.rowUpdated(index);
        String name = testCase.getName();
        synchronized (writtenFiles) {
            writtenFiles.add(name + expectedOutputExtension);
        }
        Files.copy(Paths.get(directory, name + programOutputExtension),
                Paths.get(directory, name + expectedOutputExtension));
    }

    public void flipTestSkipped(int index) throws IOException {
        checkNotRunning();
        testCases.get(index).flipSkipped();
        testsTableModel.rowUpdated(index);
        testsListChanged();
    }

    public void swapTestCases(int index1, int index2) throws IOException {
        checkNotRunning();
        Collections.swap(testCases, index1, index2);
        testsTableModel.rowUpdated(index1);
        testsTableModel.rowUpdated(index2);
        testsListChanged();
    }

    public void deleteTestCase(int index) throws IOException {
        checkNotRunning();
        testSetChanged = true;
        String name = testCases.get(index).getName();
        testCaseNames.remove(name);
        testCases.remove(index);
        testsTableModel.rowDeleted(index);
        testsListChanged();
        Files.delete(Paths.get(directory, name + inputExtension));
        Files.delete(Paths.get(directory, name + expectedOutputExtension));
        Files.delete(Paths.get(directory, name + programOutputExtension));
    }

    private void checkNotRunning() throws IOException {
        if (testsAreRunning) throw new IOException("Cannot perform an action when tests are running.");
    }

    private void testsListChanged() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (TestCase testCase : testCases) {
            builder.append(String.join(" ", testCase.getName(), testCase.getExecutionResults(" ")));
            builder.append("\n");
        }
        writeToFile(testListFileName, builder.toString());
    }

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

    void fileChanged(WatchEvent.Kind kind, String fileName) {
        System.out.format("%s: %s (%s)\n", kind.name(), fileName, getProblem().getId());
        synchronized (writtenFiles) {
            if (writtenFiles.contains(fileName)) {
                writtenFiles.remove(fileName);
                System.out.println("just written");
                return;
            }
            if (fileName.equals(testListFileName)) {
//            Stream<String> lines = Files.lines(testListFile, utf8);
//            if (lines.anyMatch(line -> {
//                String[] tokens = line.split(" ");
//                return tokens.length > 1 && (tokens[1].equals("RUNNING") || tokens[1].equals("PENDING"));
//            })) {
//                throw new IOException("Cannot write tests to disk while testing is in progress.");
//            }
            } else {

            }
        }
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
