package ua.alcash.ui;

import net.egork.chelper.task.TestType;
import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.TestCase;
import ua.alcash.filesystem.ProblemSync;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.ParseException;

/**
 * Created by Al.Cash on 5/9/17.
 */
public class ProblemPanel extends JPanel {
    private Frame parentFrame;
    private JPanel rootPanel;
    private JLabel problemName;
    private JSpinner timeLimitSpinner;
    private JSpinner memoryLimitSpinner;
    private JTextField inputFileField;
    private JTextField outputFileField;
    private JComboBox testTypeComboBox;
    private JCheckBox interactiveCheckBox;
    private JCheckBox customCheckerCheckBox;
    private JTextField checkerParamsField;

    private JButton newButton;
    private JButton editButton;
    private JButton solvedButton;
    private JButton skipButton;
    private JButton upButton;
    private JButton downButton;
    private JButton deleteButton;

    private JTable testsTable;

    private ProblemSync problemSync;

    ProblemPanel(Frame parentFrame, ProblemSync problemSync) {
        this.parentFrame = parentFrame;
        setLayout(new GridLayout(1, 1));
        add(rootPanel);

        this.problemSync = problemSync;
        Problem problem = problemSync.getProblem();
        problemName.setText(problem.getFullName());
        timeLimitSpinner.setModel(new SpinnerNumberModel(
                problem.getTimeLimit(), 0, 9999, 1));
        memoryLimitSpinner.setModel(new SpinnerNumberModel(
                problem.getMemoryLimit(), 0, 9999, 1));
        testTypeComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                TestType.SINGLE.toString(),
                TestType.MULTI_NUMBER.toString(),
                TestType.MULTI_EOF.toString()
        }));
        testTypeComboBox.setSelectedIndex(problem.getTestType().ordinal());
        inputFileField.setText(problem.getInputFile());
        outputFileField.setText(problem.getOutputFile());
        interactiveCheckBox.setSelected(problem.getInteractive());
        customCheckerCheckBox.setSelected(problem.getCustomChecker());
        checkerParamsField.setText(problem.getCheckerParams());

        testsTable.setDefaultRenderer(String.class, new MultilineTableCellRenderer());
        testsTable.setModel(problemSync.getTableModel());
        testsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        ListSelectionListener listSelectionListener = event -> {
            boolean enable = !(testsTable.getSelectionModel().isSelectionEmpty());
            editButton.setEnabled(enable);
            solvedButton.setEnabled(enable);
            skipButton.setEnabled(enable);
            upButton.setEnabled(enable && getSelectedIndex() > 0);
            downButton.setEnabled(enable && getSelectedIndex() < testsTable.getRowCount() - 1);
            deleteButton.setEnabled(enable);
        };
        testsTable.getSelectionModel().addListSelectionListener(listSelectionListener);

        testsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {  // double-click
                    editTestCase();
                }
            }
        });

        newButton.addActionListener(event -> newTestCase());
        editButton.addActionListener(event -> editTestCase());
        solvedButton.addActionListener(event -> runAndCheck(() -> problemSync.setTestSolved(getSelectedIndex())));
        skipButton.addActionListener(event -> runAndCheck(() -> problemSync.flipTestSkipped(getSelectedIndex())));
        upButton.addActionListener(event -> swapTestCases(getSelectedIndex() - 1));
        downButton.addActionListener(event -> swapTestCases(getSelectedIndex() + 1));
        deleteButton.addActionListener(event -> deleteTestCase());
        setupShortcuts();
    }

    private void setupShortcuts() {
        String keyNew = "test case new";
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Configuration.getShortcut(keyNew), keyNew);
        getActionMap().put(keyNew, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                newTestCase();
            }
        });
        String keyEdit = "test case edit";
        testsTable.getInputMap().put(Configuration.getShortcut(keyEdit), keyEdit);
        testsTable.getActionMap().put(keyEdit, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                editTestCase();
            }
        });
        String keyDelete = "test case delete";
        testsTable.getInputMap().put(Configuration.getShortcut(keyDelete), keyDelete);
        testsTable.getActionMap().put(keyDelete, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteTestCase();
            }
        });
    }

    private int getSelectedIndex() throws UnsupportedOperationException {
        int index = testsTable.getSelectedRow();
        if (index == -1) {
            throw new UnsupportedOperationException("No row selected to perform an action");
        }
        return index;
    }

    interface RunnableIO { void run() throws IOException; }

    private void runAndCheck(RunnableIO f) {
        try {
            f.run();
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    Configuration.PROJECT_NAME,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void newTestCase() {
        TestCase newTestCase = new TestCase(problemSync.getNextTestName());
        TestCaseDialog dialog = new TestCaseDialog(parentFrame, newTestCase);
        dialog.setVisible(true);
        if (dialog.somethingChanged()) {
            runAndCheck(() -> problemSync.addTestCase(newTestCase, true));
        }
    }

    private void editTestCase() {
        int index = getSelectedIndex();
        TestCase editedTestCase = problemSync.getTestCase(index);
        TestCaseDialog dialog = new TestCaseDialog(parentFrame, editedTestCase);
        dialog.setVisible(true);
        if (dialog.inputChanged()) {
            runAndCheck(() -> problemSync.testInputChanged(index));
        }
        if (dialog.markedSolved()) {
            runAndCheck(() -> problemSync.setTestSolved(index));
        } else if (dialog.answerChanged()) {
            runAndCheck(() -> problemSync.testAnswerChanged(index));
        }
    }

    private void swapTestCases(int swapIndex) {
        runAndCheck(() -> problemSync.swapTestCases(getSelectedIndex(), swapIndex));
        testsTable.setRowSelectionInterval(swapIndex, swapIndex);
    }

    private void deleteTestCase() {
        int confirmed = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this test case?",
                Configuration.PROJECT_NAME,
                JOptionPane.YES_NO_OPTION);
        if (confirmed == JOptionPane.YES_OPTION) {
            runAndCheck(() -> problemSync.deleteTestCase(getSelectedIndex(), true));
        }
    }

    void updateProblemFromInterface() {
        try {
            timeLimitSpinner.commitEdit();
            memoryLimitSpinner.commitEdit();
        } catch (ParseException ignored) {
        }
        Problem problem = problemSync.getProblem();
        problem.setTimeLimit((Double) timeLimitSpinner.getValue());
        problem.setMemoryLimit((Double) memoryLimitSpinner.getValue());
        problem.setInputFile(inputFileField.getText());
        problem.setOutputFile(outputFileField.getText());
        problem.setTestType(TestType.values()[testTypeComboBox.getSelectedIndex()]);
        problem.setInteractive(interactiveCheckBox.isSelected());
        problem.setCustomChecker(customCheckerCheckBox.isSelected());
        problem.setCheckerParams(checkerParamsField.getText());
    }
}
