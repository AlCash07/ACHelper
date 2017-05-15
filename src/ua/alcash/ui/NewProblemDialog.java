package ua.alcash.ui;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.parsing.ParseManager;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.event.*;
import java.net.MalformedURLException;
import java.util.Collection;
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

    MainFrame parent;

    public NewProblemDialog(MainFrame parent) {
        super(parent, true);
        this.parent = parent;
        setTitle("Enter problem information or URL to parse");
        setContentPane(rootPanel);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                closeDialog();
            }
        });
        parseButton.addActionListener(event -> startParsing());
        abortParsingButton.addActionListener(event -> abortParsing());
        createProblemButton.addActionListener(event -> createProblem());
    }

    public void display() {
        problemIdField.requestFocus();  // set cursor in problem ID field
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    public void configure() {
        List<String> platformIds;
        platformIds = ParseManager.getPlatformIds();
        Collections.sort(platformIds);
        platformComboBox.setMaximumRowCount(platformIds.size());
        platformComboBox.setModel(new DefaultComboBoxModel<>(platformIds.toArray(new String[0])));
        pack();
        setupShortcuts();
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

    private void createProblem() {
        String problemId = problemIdField.getText();
        if (problemId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Problem ID is empty.",
                    Configuration.PROJECT_NAME,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        Problem problem = new Problem(problemId, problemNameField.getText(),
                (String) platformComboBox.getSelectedItem(), contestNameField.getText());
        parent.problemsPane.addProblems(Collections.singletonList(problem));
        closeDialog();
    }

    class BackgroundProblemParser extends SwingWorker<Collection<Problem>, Object> {
        NewProblemDialog dialog;
        String url;

        BackgroundProblemParser(NewProblemDialog dialog, String url) {
            this.dialog = dialog;
            this.url = url;
        }

        @Override
        public Collection<Problem> doInBackground() throws MalformedURLException, ParserConfigurationException {
            return ParseManager.parseProblemByUrl(url);
        }

        @Override
        protected void done() {
            try {
                dialog.parent.problemsPane.addProblems(get());
                dialog.closeDialog();
            } catch (ExecutionException exception) {
                JOptionPane.showMessageDialog(dialog, exception.getMessage(),
                        Configuration.PROJECT_NAME,
                        JOptionPane.ERROR_MESSAGE);
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
        if (!parseButton.isEnabled()) {
            abortParsing();
        }
        setVisible(false);
    }
}
