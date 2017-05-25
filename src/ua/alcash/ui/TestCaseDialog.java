package ua.alcash.ui;

import ua.alcash.Configuration;
import ua.alcash.TestCase;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by oleksandr.bacherikov on 5/10/17.
 */
public class TestCaseDialog extends JDialog {
    private JPanel rootPanel;
    private JTextArea inputTextArea;
    private JTextArea expectedOutputTextArea;
    private JTextArea programOutputTextArea;
    private JButton saveButton;
    private JButton discardButton;
    private JCheckBox solvedCheckBox;

    class UpdateListener implements DocumentListener {
        boolean updated = false;

        @Override
        public void changedUpdate(DocumentEvent event) { update(); }

        @Override
        public void removeUpdate(DocumentEvent event) { update(); }

        @Override
        public void insertUpdate(DocumentEvent event) { update(); }

        private void update() {
            updated = true;
            saveButton.setEnabled(true);
        }
    }

    private UpdateListener inputListener = new UpdateListener();
    private UpdateListener outputListener = new UpdateListener();

    private TestCase testCase;
    private boolean wasSolved;

    boolean saved = false;

    TestCaseDialog(Frame parent, TestCase testCase) {
        super(parent, true);
        this.testCase = testCase;
        wasSolved = testCase.getSolved();

        setTitle("Test case " + testCase.getName());
        setContentPane(rootPanel);
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                confirmAndClose();
            }
        });

        inputTextArea.setText(testCase.getInput());
        expectedOutputTextArea.setText(testCase.getExpectedOutput());
        expectedOutputTextArea.setEnabled(!testCase.getSolved());
        programOutputTextArea.setText(testCase.getProgramOutput());
        inputTextArea.getDocument().addDocumentListener(inputListener);
        expectedOutputTextArea.getDocument().addDocumentListener(outputListener);
        solvedCheckBox.setSelected(wasSolved);

        saveButton.addActionListener(event -> saveTestCase());
        discardButton.addActionListener(event -> confirmAndClose());
        solvedCheckBox.addActionListener(event -> {
            testCase.flipSolved();
            expectedOutputTextArea.setText(testCase.getExpectedOutput());
            expectedOutputTextArea.setEnabled(!testCase.getSolved());
        });
        setupShortcuts();

        pack();
        setLocationRelativeTo(parent);
    }

    private void setupShortcuts() {
        // escape key will close the dialog
        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) { confirmAndClose(); }
        });

        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                Configuration.getShortcut("test case save"), "save");
        getRootPane().getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) { saveTestCase(); }
        });

        // when TAB is pressed, cycle textAreas instead of writing the \t
        inputTextArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        inputTextArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        expectedOutputTextArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        expectedOutputTextArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        programOutputTextArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        programOutputTextArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
    }

    private void saveTestCase() {
        if (inputListener.updated) {
            testCase.setInput(inputTextArea.getText());
        }
        if (!testCase.getSolved() && outputListener.updated) {
            testCase.setExpectedOutput(expectedOutputTextArea.getText());
        }
        saved = true;
        dispose();
    }

    private void confirmAndClose() {
        boolean somethingChanged = inputListener.updated || outputListener.updated || wasSolved != testCase.getSolved();
        if (somethingChanged) {
            int confirmed = JOptionPane.showConfirmDialog(this,
                    "There are unsaved changes. Are you sure you want to discard them?",
                    Configuration.PROJECT_NAME, JOptionPane.YES_NO_OPTION);
            somethingChanged = (confirmed == JOptionPane.NO_OPTION);
        }
        if (!somethingChanged) {
            if (testCase.getSolved() != wasSolved) testCase.flipSolved();
        }
        dispose();
    }
}
