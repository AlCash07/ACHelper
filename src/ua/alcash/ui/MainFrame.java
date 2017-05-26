package ua.alcash.ui;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.filesystem.ProblemSync;
import ua.alcash.filesystem.WorkspaceManager;
import ua.alcash.parsing.ParseManager;
import ua.alcash.util.AbstractActionWithInteger;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by oleksandr.bacherikov on 5/9/17.
 */
public class MainFrame extends JFrame {
    private JPanel rootPanel;
    private JTabbedPane problemsPane;

    private JMenuItem newContest;
    private JMenuItem newProblem;
    private JMenuItem saveWorkspace;
    private JMenuItem switchWorkspace;
    private JMenuItem clearWorkspace;
    private JMenuItem exitApp;

    private NewProblemDialog problemDialog = new NewProblemDialog(this);
    private NewContestDialog contestDialog = new NewContestDialog(this);

    private WorkspaceManager workspaceManager;

    private MainFrame() throws InstantiationException {
        try {
            workspaceManager = new WorkspaceManager(this);
        } catch (IOException exception) {
            receiveError("Failed to start workspace watcher, it's highly recommended to restart " +
                    Configuration.PROJECT_NAME + "\n" + exception.getMessage());
        }
        setTitle(Configuration.PROJECT_NAME);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                confirmAndExit();
            }
        });
        setContentPane(rootPanel);
        createMainMenu();
        createPopupMenu();

        // set system proxy if there is one
        try {
            System.setProperty("java.net.useSystemProxies", "true");
        } finally {
        }
        configure();

        pack();
        setLocationRelativeTo(null);
    }

    private void configure() {
        ParseManager.configure();
        workspaceManager.configure();
        problemDialog.configure();
        setupShortcuts();
    }

    private void createMainMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu newMenu = new JMenu();
        newContest = new JMenuItem();
        newProblem = new JMenuItem();
        JMenu workspaceMenu = new JMenu();
        saveWorkspace = new JMenuItem();
        clearWorkspace = new JMenuItem();
        switchWorkspace = new JMenuItem();
        JMenu systemMenu = new JMenu();
        exitApp = new JMenuItem();

        newMenu.setText("New...");

        newContest.setText("Contest");
        newContest.addActionListener(event -> contestDialog.display());
        newMenu.add(newContest);

        newProblem.setText("Problem");
        newProblem.addActionListener(event -> problemDialog.display());
        newMenu.add(newProblem);

        menuBar.add(newMenu);

        workspaceMenu.setText("Workspace");

        saveWorkspace.setText("Apply changes");
        saveWorkspace.addActionListener(event -> {
            for (int i = 0; i < problemsPane.getTabCount(); ++i) {
                ((ProblemPanel) problemsPane.getComponentAt(i)).updateProblemFromInterface();
            }
            workspaceManager.updateWorkspace(false);
        });
        workspaceMenu.add(saveWorkspace);

        clearWorkspace.setText("Close problems");
        clearWorkspace.addActionListener(event -> closeWorkspace());
        workspaceMenu.add(clearWorkspace);

        switchWorkspace.setText("Switch");
        switchWorkspace.addActionListener(event -> {
            if (workspaceManager.selectWorkspace() == JOptionPane.YES_OPTION) {
                workspaceManager.closeAllProblems(false);
                problemsPane.removeAll();
                configure();
            }
        });
        workspaceMenu.add(switchWorkspace);

        menuBar.add(workspaceMenu);

        systemMenu.setText("System");

        exitApp.setText("Exit");
        exitApp.addActionListener(event -> confirmAndExit());
        systemMenu.add(exitApp);

        menuBar.add(systemMenu);

        setJMenuBar(menuBar);
    }

    private void createPopupMenu() {
        // adds popup menu to tabs with options to close or delete a problem
        final JPopupMenu singleTabPopupMenu = new JPopupMenu();
        JMenuItem closeProblem = new JMenuItem("Close problem");
        closeProblem.addActionListener(event -> closeProblem(false));
        singleTabPopupMenu.add(closeProblem);
        JMenuItem deleteProblem = new JMenuItem("Delete problem");
        deleteProblem.addActionListener(event -> closeProblem(true));
        singleTabPopupMenu.add(deleteProblem);

        problemsPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    doPop(event);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.isPopupTrigger()) {
                    doPop(event);
                }
            }

            private void doPop(MouseEvent event) {
                singleTabPopupMenu.show(event.getComponent(), event.getX(), event.getY());
            }
        });
    }

    private void setupShortcuts() {
        newContest.setAccelerator(Configuration.getShortcut("new contest"));
        newProblem.setAccelerator(Configuration.getShortcut("new problem"));
        saveWorkspace.setAccelerator(Configuration.getShortcut("workspace apply changes"));
        clearWorkspace.setAccelerator(Configuration.getShortcut("workspace close problems"));
        switchWorkspace.setAccelerator(Configuration.getShortcut("workspace switch"));
        exitApp.setAccelerator(Configuration.getShortcut("exit"));

        for (int index = 1; index <= 9; index++) {
            problemsPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("alt " + index), "switch tab " + index);
            problemsPane.getActionMap().put("switch tab " + index, new AbstractActionWithInteger(index) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (problemsPane.getTabCount() >= getInteger()) {
                        problemsPane.setSelectedIndex(getInteger() - 1);
                    }
                }
            });
        }
    }

    public void receiveProblems(Collection<Problem> problems) {
        int firstProblemIndex = problemsPane.getTabCount();
        for (Problem problem : problems) {
            try {
                ProblemSync problemSync = workspaceManager.addProblem(problem);
                ProblemPanel panel = new ProblemPanel(this, problemSync);
                problemsPane.addTab(problem.getId(), panel);
            } catch (IOException exception) {
                receiveError(exception.getMessage());
            }
        }
        if (firstProblemIndex < problemsPane.getTabCount()) {
            problemsPane.setSelectedIndex(firstProblemIndex);
        }
        workspaceManager.updateWorkspace(true);
    }

    public void receiveError(String message) {
        JOptionPane.showMessageDialog(this, message,
                Configuration.PROJECT_NAME, JOptionPane.ERROR_MESSAGE);
    }

    public void receiveWarning(String message) {
        JOptionPane.showMessageDialog(this, message,
                Configuration.PROJECT_NAME, JOptionPane.WARNING_MESSAGE);
    }

    private void closeProblem(boolean delete) {
        int index = problemsPane.getSelectedIndex();
        workspaceManager.closeProblem(index, delete);
        problemsPane.removeTabAt(index);
        workspaceManager.updateWorkspace(true);
    }

    private boolean closeWorkspace() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Keep the workspace content on the disk?",
                Configuration.PROJECT_NAME,
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (confirm != JOptionPane.CANCEL_OPTION && confirm != JOptionPane.CLOSED_OPTION) {
            workspaceManager.closeAllProblems(confirm == JOptionPane.NO_OPTION);
            problemsPane.removeAll();
            return true;
        }
        return false;
    }

    private void confirmAndExit() {
        if (closeWorkspace()) {
            workspaceManager.stop();
            System.exit(0);
        }
    }

    public static void main(String args[]) {
        try {
            new MainFrame().setVisible(true);
        } catch (InstantiationException exception) {
            System.exit(0);
        }
    }
}
