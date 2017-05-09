package ua.alcash.ui;

import ua.alcash.Problem;
import ua.alcash.util.AbstractActionWithInteger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * Created by oleksandr.bacherikov on 5/9/17.
 */
public class ProblemSetPane extends JTabbedPane {
    String workspaceDirectory = System.getProperty("user.dir");

    ArrayList<Problem> problems = new ArrayList<>();

    public ProblemSetPane() {
        setFont(new Font("Tahoma", Font.BOLD, 13));
        setTabLayoutPolicy(SCROLL_TAB_LAYOUT);
        setTabPlacement(LEFT);
        setPreferredSize(new Dimension(640, 480));
        createPopupMenu();
        setupShortcuts();
    }

    private void createPopupMenu() {
        // adds popup menu to tabs with options to close or delete a problem
        final JPopupMenu singleTabPopupMenu = new JPopupMenu();
        JMenuItem closeProblem = new JMenuItem("Close problem");
        closeProblem.addActionListener(event -> {
            problems.remove(getSelectedIndex());
            remove(getSelectedComponent());
        });
        singleTabPopupMenu.add(closeProblem);
        JMenuItem deleteProblem = new JMenuItem("Delete problem");
        deleteProblem.addActionListener(event -> {
            // problems[getSelectedIndex()].
            closeProblem.dispatchEvent(event);
        });
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

    public void addProblem(Problem newProblem) {
        for (Problem problem : problems) {
            if (problem.getDirectory() == newProblem.getDirectory()) {
                JOptionPane.showMessageDialog(this,
                        "Problem with such directory already exists:\n" + newProblem.getDirectory(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        ProblemPanel panel = new ProblemPanel(newProblem);
        addTab(newProblem.getProblemId(), panel);
        setSelectedComponent(panel);
    }

    public void addContest(ArrayList<Problem> problems) {
        if (problems == null || problems.isEmpty()) {
            return;
        }
        int firstProblemIndex = getTabCount();
        for (Problem problem : problems) {
            addProblem(problem);
        }
        setSelectedIndex(firstProblemIndex);
    }


}
