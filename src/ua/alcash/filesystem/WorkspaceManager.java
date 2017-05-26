package ua.alcash.filesystem;

import com.sun.nio.file.SensitivityWatchEventModifier;
import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.network.ChromeListener;
import ua.alcash.ui.MainFrame;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by oleksandr.bacherikov on 5/24/17.
 */
public class WorkspaceManager {
    private MainFrame parent;

    private String workspaceDirectory = System.getProperty("user.dir");

    private final ChromeListener chromeListener;

    private final WatchService workspaceWatcher;
    private final Thread watcherThread;

    private ArrayList<ProblemSync> problemSyncs = new ArrayList<>();
    private ArrayList<WatchKey> watchKeys = new ArrayList<>();

    public WorkspaceManager(MainFrame parent) throws InstantiationException, IOException {
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
        chromeListener = new ChromeListener(parent);
        workspaceWatcher = FileSystems.getDefault().newWatchService();
        watcherThread = new Thread(this::processEvents,"WorkspaceWatcherThread");
        watcherThread.start();
    }

    public void configure() {
        ProblemSync.configure();
        chromeListener.start(Configuration.get("CHelper port"));
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
        new Thread(() -> {
            try {
                Generator.generate(workspaceDirectory, problemSyncs, problemSetChanged);
            } catch (IOException exception) {
                SwingUtilities.invokeLater(() -> parent.receiveError(exception.getMessage()));
            }
        }, "WorkspaceUpdateThread").start();
    }

    public ProblemSync addProblem(Problem newProblem) throws IOException {
        ProblemSync newProblemSync = new ProblemSync(workspaceDirectory, newProblem);
        String directory = newProblemSync.getDirectory();
        for (ProblemSync problemSync : problemSyncs) {
            if (problemSync.getDirectory().equals(directory)) {
                throw new IOException("Problem with such directory already exists: " + problemSync.getDirectory());
            }
        }
        newProblemSync.initialize();
        synchronized (workspaceWatcher) {
            problemSyncs.add(newProblemSync);
            WatchKey key = Paths.get(directory).register(workspaceWatcher,
                    new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},
                    SensitivityWatchEventModifier.HIGH);
            watchKeys.add(key);
        }
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
        synchronized (workspaceWatcher) {
            problemSyncs.remove(index);
            watchKeys.get(index).cancel();
            watchKeys.remove(index);
        }
    }

    public void closeAllProblems(boolean delete) {
        while (!problemSyncs.isEmpty()) {
            closeProblem(0, delete);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) { return (WatchEvent<T>)event; }

    private void processEvents() {
        for (;;) {
            WatchKey key;
            try {
                key = workspaceWatcher.take();
            } catch (InterruptedException exception) {
                break;
            }

            synchronized (workspaceWatcher) {
                int index = 0;
                for (; index < watchKeys.size() && key != watchKeys.get(index); ++index);
                if (index == watchKeys.size()) continue;

                for (WatchEvent<?> event: key.pollEvents()) {
                    WatchEvent.Kind kind = event.kind();
                    if (kind == OVERFLOW) continue;
                    WatchEvent<Path> watchEvent = cast(event);
                    Path file = watchEvent.context();
                    problemSyncs.get(index).fileChanged(kind, file.toString());
                }
            }
            key.reset();
        }
    }

    public void stop() {
        chromeListener.stop();
        watcherThread.interrupt();
        try {
            workspaceWatcher.close();
        } catch (IOException ignored) {
        }
    }
}
