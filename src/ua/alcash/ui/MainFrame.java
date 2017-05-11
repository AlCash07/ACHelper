package ua.alcash.ui;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.util.ParseManager;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by oleksandr.bacherikov on 5/9/17.
 */
public class MainFrame extends JFrame {
    private ProblemSetPane problemsPane;

    private JMenuItem newContest;
    private JMenuItem newProblem;
    private JMenuItem saveWorkspace;
    private JMenuItem switchWorkspace;
    private JMenuItem clearWorkspace;
    private JMenuItem exitApp;

    public MainFrame() throws InstantiationException {
        problemsPane = new ProblemSetPane(this);
        if (!Configuration.load(problemsPane.workspaceDirectory)) {
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
        setContentPane(problemsPane);
        createMainMenu();
        configure();

        // set system proxy if there is one
        try {
            System.setProperty("java.net.useSystemProxies", "true");
        } finally {
        }

        pack();
        setLocationRelativeTo(null);
    }

    public void configure() {
        ParseManager.configure();
        ProblemSetPane.configure();
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
        newContest.addActionListener(event -> getNewContest());
        newMenu.add(newContest);

        newProblem.setText("Problem");
        newProblem.addActionListener(event -> getNewProblem());
        newMenu.add(newProblem);

        menuBar.add(newMenu);

        workspaceMenu.setText("Workspace");

        saveWorkspace.setText("Save to disk");
        saveWorkspace.addActionListener(event -> problemsPane.updateProblemsOnDisk());
        workspaceMenu.add(saveWorkspace);

        clearWorkspace.setText("Delete problems");
        clearWorkspace.addActionListener(event -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete all problem folders on disk?",
                    Configuration.PROJECT_NAME,
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                problemsPane.deleteAllProblems();
            }
        });
        workspaceMenu.add(clearWorkspace);

        switchWorkspace.setText("Switch");
        switchWorkspace.addActionListener(event -> {
            if (selectWorkspace() == SelectionResult.SUCCESS)
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

    private void setupShortcuts() {
        newContest.setAccelerator(Configuration.getShortcut("new contest"));
        newProblem.setAccelerator(Configuration.getShortcut("new problem"));
        saveWorkspace.setAccelerator(Configuration.getShortcut("workspace save to disk"));
        clearWorkspace.setAccelerator(Configuration.getShortcut("workspace delete problems"));
        switchWorkspace.setAccelerator(Configuration.getShortcut("workspace switch"));
        exitApp.setAccelerator(Configuration.getShortcut("exit"));
    }

    private void getNewContest() {
//        NewContestDialog contestDialog = new NewContestDialog(this);
//        contestDialog.setVisible(true);  // this is modal; it will block until the window is closed
//        problemsPane.addContest(contestDialog.getProblemList());
    }

    private void getNewProblem() {
        NewProblemDialog problemDialog = new NewProblemDialog(this);
        problemDialog.setVisible(true);
        Problem problem = problemDialog.getProblem();
        if (problem != null) {
            problemsPane.addProblem(problemDialog.getProblem());
        }
    }

    private enum SelectionResult {SUCCESS, FAIL, CANCEL}

    private SelectionResult selectWorkspace() {
        JFileChooser fileChooser = new JFileChooser(problemsPane.workspaceDirectory);
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
            problemsPane.workspaceDirectory = directory;
            problemsPane.removeAll();
            return SelectionResult.SUCCESS;
        }
        return SelectionResult.CANCEL;
    }

    private void confirmAndExit() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Clear workspace before exiting?",
                Configuration.PROJECT_NAME,
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (confirm != JOptionPane.CANCEL_OPTION) {
            if (confirm == JOptionPane.YES_OPTION) {
                problemsPane.deleteAllProblems();
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
