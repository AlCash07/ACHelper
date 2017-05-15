package ua.alcash.ui;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.util.AbstractActionWithInteger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by oleksandr.bacherikov on 5/9/17.
 */
public class ProblemSetPane extends JTabbedPane {
    private Frame parentFrame;

    String workspaceDirectory = System.getProperty("user.dir");

    private ArrayList<Problem> problems = new ArrayList<>();

    public ProblemSetPane(Frame parentFrame) {
        this.parentFrame = parentFrame;
        setFont(new Font("Tahoma", Font.BOLD, 13));
        setTabLayoutPolicy(SCROLL_TAB_LAYOUT);
        setTabPlacement(LEFT);
        setPreferredSize(new Dimension(640, 480));
        createPopupMenu();
        setupShortcuts();
    }

    static public void configure() {
        ProblemPanel.configure();
    }

    private void createPopupMenu() {
        // adds popup menu to tabs with options to close or delete a problem
        final JPopupMenu singleTabPopupMenu = new JPopupMenu();
        JMenuItem closeProblem = new JMenuItem("Close problem");
        closeProblem.addActionListener(event -> closeProblem(getSelectedIndex(), false));
        singleTabPopupMenu.add(closeProblem);
        JMenuItem deleteProblem = new JMenuItem("Delete problem");
        deleteProblem.addActionListener(event -> closeProblem(getSelectedIndex(), true));
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
        for (int index = 1; index <= 9; index++) {
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("alt " + index), "switch tab " + index);
            getActionMap().put("switch tab " + index, new AbstractActionWithInteger(index) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (getTabCount() >= getInteger()) {
                        setSelectedIndex(getInteger() - 1);
                    }
                }
            });
        }
    }

    private void addProblem(Problem newProblem) {
        for (Problem problem : problems) {
            if (problem.getDirectory().equals(newProblem.getDirectory())) {
                JOptionPane.showMessageDialog(this,
                        "Problem with such directory already exists: " + newProblem.getDirectory(),
                        Configuration.PROJECT_NAME,
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        problems.add(newProblem);
        ProblemPanel panel = new ProblemPanel(parentFrame, newProblem);
        addTab(newProblem.getProblemId(), panel);
        setSelectedComponent(panel);
        writeProblemToDisk(newProblem);
    }

    public void addProblems(Collection<Problem> problems) {
        if (problems == null || problems.isEmpty()) {
            return;
        }
        int firstProblemIndex = getTabCount();
        for (Problem problem : problems) {
            addProblem(problem);
        }
        setSelectedIndex(firstProblemIndex);
        // run gerenation script if not adding a contest
    }

    private void writeProblemToDisk(Problem problem) {
        try {
            problem.writeToDisk(workspaceDirectory);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this,
                    "Writing folder " + problem.getDirectory() + " to disk caused an error:\n"
                            + exception.getMessage(),
                    Configuration.PROJECT_NAME,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void updateProblemsOnDisk() {
        for (int i = 0; i < problems.size(); ++i) {
            ((ProblemPanel)getComponentAt(i)).updateProblemFromInterface();
            writeProblemToDisk(problems.get(i));
        }
    }

    private void closeProblem(int index, boolean delete) {
        if (delete) {
            try {
                problems.get(index).deleteFromDisk(workspaceDirectory);
            } catch (IOException exception) {
                JOptionPane.showMessageDialog(this,
                        "Deleting folder " + problems.get(index).getDirectory() + " caused an error:\n"
                                + exception.getMessage(),
                        Configuration.PROJECT_NAME,
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        problems.remove(index);
        removeTabAt(index);
    }

    public void closeAllProblems(boolean delete) {
        while (!problems.isEmpty()) closeProblem(0, delete);
    }
}
