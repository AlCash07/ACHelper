package ua.alcash.ui;

import net.egork.chelper.task.TestType;
import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.TestCase;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created by oleksandr.bacherikov on 5/9/17.
 */
public class ProblemPanel extends JPanel {
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
    private TestsTableModel testsTableModel;  // used to notify testsTable about the testCaseSet changes

    private Problem problem;

    public ProblemPanel(Problem problem) {
        setLayout(new GridLayout(1, 1));
        add(rootPanel);

        this.problem = problem;
        problemName.setText(problem.getFullName());
        timeLimitSpinner.setModel(new SpinnerNumberModel(
                problem.getTimeLimit(), 0, 9999, 1));
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

        testsTableModel = new TestsTableModel(problem.getTestCaseSet());
        testsTable.setDefaultRenderer(String.class, new MultilineTableCellRenderer());
        testsTable.setModel(testsTableModel);
        testsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        ListSelectionListener listSelectionListener = event -> {
            boolean enable = !(testsTable.getSelectionModel().isSelectionEmpty());
            editButton.setEnabled(enable);
            solvedButton.setEnabled(enable);
            skipButton.setEnabled(enable);
            upButton.setEnabled(enable && getSelectedIndex() > 0);
            downButton.setEnabled(enable && getSelectedIndex() < testsTableModel.getRowCount() - 1);
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
        solvedButton.addActionListener(event -> solvedTestCase());
        skipButton.addActionListener(event -> skipTestCase());
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
        int selectedRow = testsTable.getSelectedRow();
        if (selectedRow == -1) {
            throw new UnsupportedOperationException("No row selected to perform an action");
        }
        return selectedRow;
    }

    private void newTestCase() {
//        TestCase newTestCase = new TestCase(problem.getNextTestName());
//        TestCaseJDialog dialog = new TestCaseJDialog(parentWindow, newTestCase);
//        dialog.setVisible(true); // this is modal; it will block until window is closed
//        if (dialog.getReturnValue()) {
//            // we need to add the newTestCase
//            problem.addTestCase(newTestCase);
//            testsTableModel.rowInserted();
//        }
    }

    private void editTestCase() {
//        TestCase editedTestCase = problem.getTestCase(selectedRow);
//        TestCaseJDialog dialog = new TestCaseJDialog(parentWindow, editedTestCase);
//        dialog.setVisible(true); // this is modal; it will block until window is closed
//        if (dialog.getReturnValue()) {
//            // the test case was edited
//            testsTableModel.rowUpdated(selectedRow);
//        }
    }

    private void solvedTestCase() {
        int index = getSelectedIndex();
        problem.getTestCase(index).flipSolved();
        testsTableModel.rowUpdated(index);
    }

    private void skipTestCase() {
        int index = getSelectedIndex();
        problem.getTestCase(index).flipSkipped();
        testsTableModel.rowUpdated(index);
    }

    private void swapTestCases(int swapIndex) {
        int index = getSelectedIndex();
        problem.swapTestCases(index, swapIndex);
        testsTable.setRowSelectionInterval(swapIndex, swapIndex);
        testsTableModel.rowUpdated(index);
        testsTableModel.rowUpdated(swapIndex);
    }

    private void deleteTestCase() {
        int index = getSelectedIndex();
        int confirmed = JOptionPane.showConfirmDialog(this,
                "Are you sure?", "Confirm delete", JOptionPane.YES_NO_OPTION);
        if (confirmed == JOptionPane.YES_OPTION) {
            problem.deleteTestCase(index);
            testsTableModel.rowDeleted(index);
        }
    }
}
