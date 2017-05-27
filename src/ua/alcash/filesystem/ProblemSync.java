package ua.alcash.filesystem;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.TestCase;
import ua.alcash.ui.TestsTableModel;

import javax.swing.table.AbstractTableModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.Files.newBufferedReader;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by Al.Cash on 5/25/17.
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
        // add existing test cases from the disk
        Files.walkFileTree(problemPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                String fileName = path.toString();
                if (fileName.endsWith(inputExtension)) {
                    String testName = fileName.substring(0, fileName.lastIndexOf("."));
                    if (!testCaseNames.contains(testName)) {
                        addTestCaseFromDisk(testName);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        for (int i = 0; i < testCases.size(); ++i) {
            createTestCaseFiles(i);
        }
        testsListChanged();
        writtenFiles.clear();
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

    public void addTestCase(TestCase testCase, boolean createFiles) throws IOException {
        checkNotRunning();
        testSetChanged = true;
        int index = testCases.size();
        testCases.add(testCase);
        testCaseNames.add(testCase.getName());
        testsTableModel.testCaseAdded();
        if (createFiles) {
            createTestCaseFiles(index);
        }
        testsListChanged();
    }

    private void addTestCaseFromDisk(String name) throws IOException {
        TestCase testCase = new TestCase(name,
                readFromFile(name + inputExtension),
                readFromFile(name + expectedOutputExtension));
        testCase.setProgramOutput(readFromFile(name + programOutputExtension));
        addTestCase(testCase, false);
    }

    private String readFromFile(String fileName) throws IOException {
        Path path = Paths.get(directory, fileName);
        if (!Files.exists(path)) {
            writeToFile(fileName, "");
            return "";
        } else {
            int fileSize = (int) new File(path.toString()).length();
            char[] data = new char[Math.min(fileSize, maxLength)];
            BufferedReader reader = newBufferedReader(path, StandardCharsets.UTF_8);
            fileSize = reader.read(data, 0, data.length);
            String result = new String(data, 0, fileSize);
            if (fileSize == maxLength) {
                result += "...";
            }
            return result;
        }
    }

    private void writeToFile(String fileName, String data) throws IOException {
        synchronized (writtenFiles) {
            writtenFiles.add(fileName);
        }
        Path inputFile = Paths.get(directory, fileName);
        Files.write(inputFile, data.getBytes());
    }

    public void testInputChanged(int index) throws IOException {
        testsTableModel.testCaseUpdated(index);
        writeToFile(testCases.get(index).getName() + inputExtension, testCases.get(index).getInput());
    }

    public void testAnswerChanged(int index) throws IOException {
        testsTableModel.testCaseUpdated(index);
        writeToFile(testCases.get(index).getName() + expectedOutputExtension,
                testCases.get(index).getExpectedOutput());
    }

    private void createTestCaseFiles(int index) throws IOException {
        testInputChanged(index);
        testAnswerChanged(index);
        writeToFile(testCases.get(index).getName() + programOutputExtension, "");
    }

    public void setTestSolved(int index) throws IOException {
        TestCase testCase = testCases.get(index);
        String name = testCase.getName();
        synchronized (writtenFiles) {
            writtenFiles.add(name + expectedOutputExtension);
        }
        Files.copy(Paths.get(directory, name + programOutputExtension),
                Paths.get(directory, name + expectedOutputExtension),
                StandardCopyOption.REPLACE_EXISTING);
        testCase.setExpectedOutput(testCase.getProgramOutput());
        testsTableModel.testCaseUpdated(index);
    }

    public void flipTestSkipped(int index) throws IOException {
        checkNotRunning();
        testCases.get(index).flipSkipped();
        testsTableModel.testCaseUpdated(index);
        testsListChanged();
    }

    public void swapTestCases(int index1, int index2) throws IOException {
        checkNotRunning();
        Collections.swap(testCases, index1, index2);
        testsTableModel.testCaseUpdated(index1);
        testsTableModel.testCaseUpdated(index2);
        testsListChanged();
    }

    public void deleteTestCase(int index, boolean deleteFiles) throws IOException {
        checkNotRunning();
        testSetChanged = true;
        String name = testCases.get(index).getName();
        testCaseNames.remove(name);
        testCases.remove(index);
        testsTableModel.testCaseDeleted(index);
        testsListChanged();
        if (deleteFiles) {
            Files.delete(Paths.get(directory, name + inputExtension));
            Files.delete(Paths.get(directory, name + expectedOutputExtension));
            Files.delete(Paths.get(directory, name + programOutputExtension));
        }
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

    boolean fileChanged(final WatchEvent.Kind kind, String fileName) throws IOException {
        System.out.format("%s: %s (%s)\n", kind.toString(), fileName, problem.getId());
        synchronized (writtenFiles) {
            if (writtenFiles.contains(fileName)) {
                writtenFiles.remove(fileName);
                return true;
            }
            if (fileName.equals(testListFileName)) {  // tests list file change
                testsAreRunning = false;
                if (kind == ENTRY_CREATE) {
                    return false;
                } else if (kind == ENTRY_MODIFY) {
                    List<String> lines = Files.readAllLines(Paths.get(directory, fileName));
                    if (lines.size() != testCases.size()) {
                        return false;
                    }
                    for (int i = 0; i < lines.size(); ++i) {
                        String[] tokens = lines.get(i).split(" ", 2);
                        if (tokens.length != 2 || !tokens[0].equals(testCases.get(i).getName())) {
                            return false;
                        }
                        testCases.get(i).setExecutionResults(tokens[1]);
                        if (testCases.get(i).isRunning()) {
                            testsAreRunning = true;
                        }
                    }
                    testsTableModel.executionResultsUpdated();
                } else {  // ENTRY_DELETE
                    testsListChanged();
                }
            } else {
                int type = -1;
                if (fileName.endsWith(inputExtension)) {
                    type = 0;
                } else if (fileName.endsWith(expectedOutputExtension)) {
                    type = 1;
                } else if (fileName.endsWith(programOutputExtension)) {
                    type = 2;
                }
                if (type == -1) {
                    return true;
                }
                String testName = fileName.substring(0, fileName.lastIndexOf("."));
                if (!testCaseNames.contains(testName)) {  // unknown test case file change
                    if (type != 0) {
                        return true;
                    }
                    if (kind == ENTRY_CREATE) {
                        addTestCaseFromDisk(testName);
                    } else {
                        return false;
                    }
                } else {  // known test case file change
                    int testIndex = 0;
                    for (; !testCases.get(testIndex).getName().equals(testName); ++testIndex);
                    TestCase testCase = testCases.get(testIndex);
                    if (kind == ENTRY_CREATE) {
                        return false;
                    } else if (kind == ENTRY_MODIFY) {
                        String data = readFromFile(fileName);
                        switch (type) {
                            case 0:
                                testCase.setInput(data);
                                break;
                            case 1:
                                testCase.setExpectedOutput(data);
                                break;
                            case 2:
                                testCase.setProgramOutput(data);
                                break;
                        }
                        testsTableModel.testCaseUpdated(testIndex);
                    } else {  // ENTRY_DELETE
                        if (type == 0) {
                            deleteTestCase(testIndex, false);
                        } else {
                            if (type == 1) {
                                testCase.setExpectedOutput("");
                            } else {
                                testCase.setProgramOutput("");
                            }
                            testsTableModel.testCaseUpdated(testIndex);
                            writeToFile(fileName, "");
                        }
                    }
                }
            }
            return true;
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
