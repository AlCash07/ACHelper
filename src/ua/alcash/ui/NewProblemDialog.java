package ua.alcash.ui;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.parsing.ParseManager;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by oleksandr.bacherikov on 5/9/17.
 */
public class NewProblemDialog extends JDialog {
    private JPanel rootPanel;
    private JComboBox platformComboBox;
    private JTextField contestNameField;
    private JTextField problemIdField;
    private JTextField problemNameField;
    private JTextField urlField;
    private JButton parseButton;
    private JButton abortParsingButton;
    private JButton createProblemButton;

    Problem problem;

    public NewProblemDialog(Frame parent) {
        super(parent, true);
        setContentPane(rootPanel);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (!parseButton.isEnabled()) {
                    abortParsing();
                }
                dispose();
            }
        });

        List<String> platformNames;
        platformNames = ParseManager.getPlatformNames();
        Collections.sort(platformNames);
        platformComboBox.setMaximumRowCount(platformNames.size());
        platformComboBox.setModel(new DefaultComboBoxModel<>(platformNames.toArray(new String[0])));

        parseButton.addActionListener(event -> startParsing());
        abortParsingButton.addActionListener(event -> abortParsing());
        createProblemButton.addActionListener(event -> createProblem());
        setupShortcuts();

        // sets cursor in problem ID field
        problemIdField.requestFocus();

        pack();
        setLocationRelativeTo(parent);
    }

    private void setupShortcuts() {
        // escape key will close the dialog
        getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                closeDialog();
            }
        });
        // hitting enter will perform the same action as clicking create button
        problemIdField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        problemIdField.getActionMap().put("enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                createProblem();
            }
        });
        // hitting enter will perform the same action as clicking startParsing button
        urlField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        urlField.getActionMap().put("enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                startParsing();
            }
        });
    }

    public Problem getProblem() { return problem; }

    private String getPlatformName() {
        return (String) platformComboBox.getItemAt(platformComboBox.getSelectedIndex());
    }

    private void createProblem() {
        String problemId = problemIdField.getText();
        if (problemId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Problem ID is empty.",
                    Configuration.PROJECT_NAME,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        problem = new Problem(problemId, problemNameField.getText(), getPlatformName(), contestNameField.getText());
        closeDialog();
    }

    class BackgroundProblemParser extends SwingWorker<Problem, Object> {
        NewProblemDialog dialog;
        String url;

        BackgroundProblemParser(NewProblemDialog dialog, String url) {
            this.dialog = dialog;
            this.url = url;
        }

        @Override
        public Problem doInBackground() throws MalformedURLException, ParserConfigurationException {
            String platformId = ParseManager.getPlatformId(dialog.getPlatformName());
            return ParseManager.parseProblemByUrl(platformId, url);
        }

        @Override
        protected void done() {
            try {
                dialog.problem = get();
                dialog.closeDialog();
            } catch (ExecutionException exception) {
                String message;
                if (exception.getCause() instanceof MalformedURLException) {
                    message = "Malformed URL.";
                } else {
                    message = "Entered URL doesn't match the selected platform.";
                }
                JOptionPane.showMessageDialog(dialog, message, Configuration.PROJECT_NAME, JOptionPane.ERROR_MESSAGE);
            } catch (Exception exception) {
            } finally {
                dialog.restoreButtonStateAfterParsing();
            }
        }
    }

    BackgroundProblemParser problemParser;

    private void startParsing() {
        createProblemButton.setEnabled(false);
        parseButton.setEnabled(false);
        parseButton.setText("Parsing...");
        abortParsingButton.setEnabled(true);
        problemParser = new BackgroundProblemParser(this, urlField.getText());
        problemParser.execute();
    }

    private void abortParsing() {
        problemParser.cancel(true);
    }

    private void restoreButtonStateAfterParsing() {
        createProblemButton.setEnabled(true);
        parseButton.setEnabled(true);
        parseButton.setText("Parse");
        abortParsingButton.setEnabled(false);
    }

    private void closeDialog() {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
}
