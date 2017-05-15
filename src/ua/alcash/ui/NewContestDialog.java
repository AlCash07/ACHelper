package ua.alcash.ui;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.parsing.ParseManager;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * Created by oleksandr.bacherikov on 5/13/17.
 */
public class NewContestDialog extends JDialog {
    private JPanel rootPanel;
    private JTextField urlTextField;
    private JButton scheduleButton;
    private JButton parseButton;
    private JButton abortButton;
    private JSpinner hourSpinner;
    private JSpinner minuteSpinner;

    MainFrame parent;

    NewContestDialog(MainFrame parent) {
        super(parent, true);
        this.parent = parent;
        setTitle("Enter contest URL to parse");
        setContentPane(rootPanel);
        hourSpinner.setModel(new SpinnerNumberModel(0, 0, 23, 1));
        minuteSpinner.setModel(new SpinnerNumberModel(0, 0, 59, 1));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                closeDialog();
            }
        });
        scheduleButton.addActionListener(event -> schedule());
        parseButton.addActionListener(event -> startParsing());
        abortButton.addActionListener(event -> abort());
        pack();
        setupShortcuts();
    }

    public void display() {
        urlTextField.requestFocus();  // set cursor in url field
        if (scheduleButton.isEnabled()) {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int minute = Calendar.getInstance().get(Calendar.MINUTE);
            minute = (minute + 14) / 15 * 15;
            if (minute == 60) {
                hour = (hour + 1) % 24;
                minute = 0;
            }
            hourSpinner.setValue(hour);
            minuteSpinner.setValue(minute);
        }
        setLocationRelativeTo(parent);
        setVisible(true);
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
        // hitting enter will perform the same action as clicking parse button
        urlTextField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        urlTextField.getActionMap().put("enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                startParsing();
            }
        });
    }

    private void toggleButtons(boolean enabled) {
        scheduleButton.setEnabled(enabled);
        hourSpinner.setEnabled(enabled);
        minuteSpinner.setEnabled(enabled);
        parseButton.setEnabled(enabled);
        abortButton.setEnabled(!enabled);
    }

    class BackgroundContestParser extends SwingWorker<Collection<Problem>, Object> {
        NewContestDialog dialog;
        String url;

        BackgroundContestParser(NewContestDialog dialog, String url) {
            this.dialog = dialog;
            this.url = url;
        }

        @Override
        public Collection<Problem> doInBackground() throws MalformedURLException, ParserConfigurationException {
            return ParseManager.parseContestByUrl(url);
        }

        @Override
        protected void done() {
            try {
                dialog.parent.problemsPane.addProblems(get());
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
                dialog.toggleButtons(true);
            }
        }
    }

    BackgroundContestParser contestParser;

    private void schedule() {
        scheduleButton.setText("Scheduled...");
        toggleButtons(false);
        Calendar calendar = Calendar.getInstance();
        try {
            hourSpinner.commitEdit();
            minuteSpinner.commitEdit();
        } catch (ParseException exception) {
        }
        calendar.set(Calendar.HOUR_OF_DAY, (Integer) hourSpinner.getValue());
        calendar.set(Calendar.MINUTE, (Integer) minuteSpinner.getValue());
        // set random 10-20 seconds delay
        calendar.set(Calendar.SECOND, new Random().nextInt(11) + 10);
//        Date date = calendar.getTime();
//        if (date.before(Calendar.getInstance().getTime())) {
//            date = new Date(calendar.getTimeInMillis() + 24*3600*1000);
//        }
    }

    private void startParsing() {
        parseButton.setText("Parsing...");
        toggleButtons(false);
    }

    private void abort() {
        if (scheduleButton.getText().equals("Scheduled...")) {
            scheduleButton.setText("Schedule");
        } else {
            parseButton.setText("Parse");
        }
        toggleButtons(true);
    }

    private void closeDialog() {
        if (!parseButton.isEnabled()) {
            abort();
        }
        setVisible(false);
    }
}
