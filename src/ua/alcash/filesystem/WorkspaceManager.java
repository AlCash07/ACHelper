package ua.alcash.filesystem;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.ui.MainFrame;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by oleksandr.bacherikov on 5/24/17.
 */
public class WorkspaceManager {
    private MainFrame parent;

    private String workspaceDirectory = System.getProperty("user.dir");
    private ArrayList<ProblemSync> problemSyncs = new ArrayList<>();

    public WorkspaceManager(MainFrame parent) throws InstantiationException {
        this.parent = parent;
        if (!Configuration.load(workspaceDirectory)) {
            parent.receiveWarning("Current directory doesn't contain valid configuration file "
                            + Configuration.CONFIGURATION_FILE_NAME + "\nPlease, select another directory.");
            while (true) {
                int result = selectWorkspace();
                if (result == JOptionPane.CLOSED_OPTION) {
                    throw new InstantiationException("Working directory wasn't provided.");
                } else if (result == JOptionPane.YES_OPTION) {
                    break;
                }
            }
        }
    }

    public int selectWorkspace() {
        JFileChooser fileChooser = new JFileChooser(workspaceDirectory);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            String directory = fileChooser.getSelectedFile().getAbsolutePath();
            if (!Configuration.load(directory)) {
                parent.receiveWarning("Selected directory doesn't contain valid configuration file "
                                + Configuration.CONFIGURATION_FILE_NAME);
                return JOptionPane.NO_OPTION;
            }
            closeAllProblems(false);
            workspaceDirectory = directory;
            return JOptionPane.YES_OPTION;
        }
        return JOptionPane.CLOSED_OPTION;
    }

    public void updateWorkspace(boolean problemSetChanged) {
        try {
            Generator.generate(workspaceDirectory, problemSyncs, problemSetChanged);
        } catch (IOException exception) {
            parent.receiveError(exception.getMessage());
        }
    }

    public ProblemSync addProblem(Problem newProblem) throws IOException {
        ProblemSync newProblemSync = new ProblemSync(workspaceDirectory, newProblem);
        for (ProblemSync problemSync : problemSyncs) {
            if (problemSync.getDirectory().equals(newProblemSync.getDirectory())) {
                throw new IOException("Problem with such directory already exists: " + problemSync.getDirectory());
            }
        }
        newProblemSync.initialize();
        problemSyncs.add(newProblemSync);
        return newProblemSync;
    }

    public void closeProblem(int index, boolean delete) {
        if (delete) {
            try {
                problemSyncs.get(index).deleteFromDisk();
            } catch (IOException exception) {
                parent.receiveError("Deleting folder " + problemSyncs.get(index).getDirectory() + " caused an error:\n"
                        + exception.getMessage());
            }
        }
        problemSyncs.remove(index);
    }

    public void closeAllProblems(boolean delete) {
        while (!problemSyncs.isEmpty()) {
            closeProblem(0, delete);
        }
    }
}