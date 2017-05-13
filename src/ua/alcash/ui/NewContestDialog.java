package ua.alcash.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;

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

    NewContestDialog(Frame parent) {
        super(parent, true);
        setTitle("Enter contest URL to parse now or later");
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

    public void show(Frame parent) {
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

    private void schedule() {
        scheduleButton.setText("Scheduled...");
        toggleButtons(false);
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
