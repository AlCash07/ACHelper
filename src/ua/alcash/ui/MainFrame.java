package ua.alcash.ui;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.filesystem.WorkspaceManager;
import ua.alcash.ProblemsReceiver;
import ua.alcash.network.ChromeListener;
import ua.alcash.parsing.ParseManager;
import ua.alcash.util.AbstractActionWithInteger;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by oleksandr.bacherikov on 5/9/17.
 */
public class MainFrame extends JFrame implements ProblemsReceiver {
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
    private ChromeListener chromeListener;

    private MainFrame() throws InstantiationException {
        workspaceManager = new WorkspaceManager(this);
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
        chromeListener = new ChromeListener(this);
        configure();

        pack();
        setLocationRelativeTo(null);
    }

    private void configure() {
        ParseManager.configure();
        ProblemPanel.configure();
        problemDialog.configure();
        setupShortcuts();
        chromeListener.start(Configuration.get("CHelper port"));
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

        saveWorkspace.setText("Save to disk");
        saveWorkspace.addActionListener(event -> {
            for (int i = 0; i < problemsPane.getTabCount(); ++i) {
                ((ProblemPanel) problemsPane.getComponentAt(i)).updateProblemFromInterface();
            }
            workspaceManager.updateProblemsOnDisk();
        });
        workspaceMenu.add(saveWorkspace);

        clearWorkspace.setText("Delete problems");
        clearWorkspace.addActionListener(event -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete all problem folders on disk?",
                    Configuration.PROJECT_NAME,
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                workspaceManager.closeAllProblems(true);
            }
        });
        workspaceMenu.add(clearWorkspace);

        switchWorkspace.setText("Switch");
        switchWorkspace.addActionListener(event -> {
            if (workspaceManager.selectWorkspace() == JOptionPane.YES_OPTION)
                configure();
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

        addMouseListener(new MouseAdapter() {
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
        saveWorkspace.setAccelerator(Configuration.getShortcut("workspace save to disk"));
        clearWorkspace.setAccelerator(Configuration.getShortcut("workspace delete problems"));
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

    @Override
    public void receiveProblems(Collection<Problem> problems) {
        int firstProblemIndex = problemsPane.getTabCount();
        for (Problem problem : problems) {
            try {
                workspaceManager.addProblem(problem);
                ProblemPanel panel = new ProblemPanel(this, problem);
                problemsPane.addTab(problem.getProblemId(), panel);
            } catch (IOException exception) {
                receiveError(exception.getMessage());
            }
        }
        if (firstProblemIndex < problemsPane.getTabCount()) {
            problemsPane.setSelectedIndex(firstProblemIndex);
        }
    }

    @Override
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
    }

    private void confirmAndExit() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Keep the workspace content on the disk?",
                Configuration.PROJECT_NAME,
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (confirm != JOptionPane.CANCEL_OPTION && confirm != JOptionPane.CLOSED_OPTION) {
            workspaceManager.closeAllProblems(confirm == JOptionPane.NO_OPTION);
            chromeListener.stop();
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
