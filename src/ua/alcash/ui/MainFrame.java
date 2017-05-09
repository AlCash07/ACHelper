package ua.alcash.ui;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.util.AbstractActionWithInteger;
import ua.alcash.util.ParseManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * Created by oleksandr.bacherikov on 5/9/17.
 */
public class MainFrame extends JFrame {
    private JPanel rootPanel;
    private JTabbedPane tabbedPane;

    private JMenuBar menuBar;
    private JMenu newMenu;
    private JMenuItem newContest;
    private JMenuItem newProblem;
    private JMenu workspaceMenu;
    private JMenuItem refreshWorkspace;
    private JMenuItem switchWorkspace;
    private JMenuItem clearWorkspace;
    private JMenu systemMenu;
    private JMenuItem exitApp;

    private String workspaceDirectory = System.getProperty("user.dir");

    public MainFrame() throws InstantiationException {
        if (!Configuration.load(workspaceDirectory)) {
            JOptionPane.showMessageDialog(this,
                    "Current directory doesn't contain configuration file "
                            + Configuration.CONFIGURATION_FILE_NAME
                            + "\nPlease, select another directory.",
                    Configuration.PROJECT_NAME,
                    JOptionPane.WARNING_MESSAGE);
            while (true) {
                SelectionResult result = selectWorkspace();
                if (result == SelectionResult.CANCEL) {
                    throw new InstantiationException("Working directory wasn't provided.");
                } else if (result == SelectionResult.SUCCESS) {
                    break;
                }
            }
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
        setupShortcuts();

        ParseManager.initialize();

        // set system proxy if there is one
        try {
            System.setProperty("java.net.useSystemProxies", "true");
        } finally {
        }

        pack();
        setLocationRelativeTo(null);
    }

    private void createMainMenu() {
        menuBar = new JMenuBar();
        newMenu = new JMenu();
        newContest = new JMenuItem();
        newProblem = new JMenuItem();
        workspaceMenu = new JMenu();
        refreshWorkspace = new JMenuItem();
        switchWorkspace = new JMenuItem();
        clearWorkspace = new JMenuItem();
        systemMenu = new JMenu();
        exitApp = new JMenuItem();

        newMenu.setText("New...");

        newContest.setText("Contest");
        newContest.addActionListener(event -> getNewContest());
        newMenu.add(newContest);

        newProblem.setText("Problem");
        newProblem.addActionListener(event -> getNewProblem());
        newMenu.add(newProblem);

        menuBar.add(newMenu);

        workspaceMenu.setText("Workspace");

        refreshWorkspace.setText("Refresh");
        refreshWorkspace.addActionListener(event -> refreshWorkspace());
        workspaceMenu.add(refreshWorkspace);

        switchWorkspace.setText("Switch");
        switchWorkspace.addActionListener(event -> {
            if (selectWorkspace() == SelectionResult.SUCCESS) {
                ParseManager.initialize();
                setupShortcuts();
            }
        });
        workspaceMenu.add(switchWorkspace);

        clearWorkspace.setText("Clear");
        clearWorkspace.addActionListener(event -> clearTestFolders());
        workspaceMenu.add(clearWorkspace);

        menuBar.add(workspaceMenu);

        systemMenu.setText("System");

        exitApp.setText("Exit");
        exitApp.addActionListener(event -> confirmAndExit());
        systemMenu.add(exitApp);

        menuBar.add(systemMenu);

        setJMenuBar(menuBar);
    }

    private void createPopupMenu() {
        // adds popup menu to tabs with options to close and delete a problem
        final JPopupMenu singleTabJPopupMenu = new JPopupMenu();
        JMenuItem deleteJMenuItem = new JMenuItem("Delete");
        deleteJMenuItem.addActionListener(event -> tabbedPane.remove(tabbedPane.getSelectedComponent()));
        singleTabJPopupMenu.add(deleteJMenuItem);
        tabbedPane.addMouseListener(new MouseAdapter() {
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
                singleTabJPopupMenu.show(event.getComponent(), event.getX(), event.getY());
            }
        });
    }

    private void setupShortcuts() {
        newContest.setAccelerator(Configuration.getShortcut("new contest"));
        newProblem.setAccelerator(Configuration.getShortcut("new problem"));
        switchWorkspace.setAccelerator(Configuration.getShortcut("refresh workspace"));
        switchWorkspace.setAccelerator(Configuration.getShortcut("switch workspace"));
        clearWorkspace.setAccelerator(Configuration.getShortcut("clear workspace"));
        exitApp.setAccelerator(Configuration.getShortcut("exit"));

        for (int index = 1; index <= 9; index++) {
            tabbedPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("alt " + index), "switch tab " + index);
            tabbedPane.getActionMap().put("switch tab " + index, new AbstractActionWithInteger(index) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (tabbedPane.getTabCount() >= getInteger()) {
                        tabbedPane.setSelectedIndex(getInteger() - 1);
                    }
                }
            });
        }
    }

    private void getNewContest() {
//        NewContestDialog contestDialog = new NewContestDialog(this);
//        contestDialog.setVisible(true);  // this is modal; it will block until the window is closed
//        addProblems(contestDialog.getProblemList());
    }

    private void getNewProblem() {
        NewProblemDialog problemDialog = new NewProblemDialog(this);
        problemDialog.setVisible(true);
        if (problemDialog.getProblem() != null) {
            addTabForProblem(problemDialog.getProblem());
        }
    }

    public void addProblems(ArrayList<Problem> problems) {
        if (problems == null || problems.isEmpty()) {
            return;
        }
        Component firstProblem = null;
        for (Problem problem : problems) {
            addTabForProblem(problem);
            if (firstProblem == null) {
                firstProblem = tabbedPane.getComponentAt(tabbedPane.getTabCount() - 1);
            }
        }
        tabbedPane.setSelectedComponent(firstProblem);
    }

    protected void addTabForProblem(Problem problem) {
//        ProblemJPanel panel = new ProblemJPanel(problem, tabbedPane, this);
//        // as recommended here: http://stackoverflow.com/questions/476678/tabs-with-equal-constant-width-in-jtabbedpane
//        tabbedPane.addTab("<html><body><table width='150'>" + problem.getProblemId() + "</table></body></html>", panel);
//        tabbedPane.setSelectedComponent(panel);
    }

    private void refreshWorkspace() {
    }

    private enum SelectionResult {SUCCESS, FAIL, CANCEL}

    private SelectionResult selectWorkspace() {
        JFileChooser fileChooser = new JFileChooser(workspaceDirectory);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fileChooser.getSelectedFile().getAbsolutePath();
            if (!Configuration.load(directory)) {
                JOptionPane.showMessageDialog(this,
                        "Selected directory doesn't contain configuration file "
                            + Configuration.CONFIGURATION_FILE_NAME,
                        Configuration.PROJECT_NAME,
                        JOptionPane.WARNING_MESSAGE);
                return SelectionResult.FAIL;
            }
            workspaceDirectory = directory;
            return SelectionResult.SUCCESS;
        }
        return SelectionResult.CANCEL;
    }

    private void clearTestFolders() {
    }

    private void confirmAndExit() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Clear workspace before exiting?",
                "Confirm exit",
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (confirm != JOptionPane.CANCEL_OPTION) {
            if (confirm == JOptionPane.YES_OPTION) {
                clearTestFolders();
            }
            dispose();
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
