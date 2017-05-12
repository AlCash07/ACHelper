package ua.alcash.util;

import ua.alcash.Configuration;
import ua.alcash.Problem;
import ua.alcash.ui.ProblemSetPane;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Created by oleksandr.bacherikov on 5/11/17.
 */
public class ChromeListener extends SwingWorker<Void, Problem> {
    ProblemSetPane parent;

    private ServerSocket serverSocket;

    public ChromeListener(ProblemSetPane parent, int port) throws IOException {
        this.parent = parent;
        serverSocket = new ServerSocket(port);
    }

    @Override
    public Void doInBackground() {
        while (!isCancelled()) {
            try {
                if (serverSocket.isClosed())
                    return null;
                Socket socket = serverSocket.accept();
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    while (!reader.readLine().isEmpty());
                    final String platformId = reader.readLine();
                    StringBuilder builder = new StringBuilder();
                    String s;
                    while ((s = reader.readLine()) != null)
                        builder.append(s).append('\n');
                    final String page = builder.toString();
                    try {
                        publish(ParseManager.parseProblemFromHtml(platformId, page));
                    } catch (ParserConfigurationException exception) {
                        publish(new Problem("", platformId, "", ""));
                    }
                } finally {
                    socket.close();
                }
            } catch (Throwable exception) {
            }
        }
        try {
            serverSocket.close();
        } catch (IOException exception) {
        }
        return null;
    }

    @Override
    protected void process(List<Problem> problems) {
        for (Problem problem : problems) {
            if (problem.getProblemId().isEmpty()) {
                String message = "Failed to parse message from CHelper Chrome extension.\n";
                if (problem.getProblemName().isEmpty()) {
                    message += "Message doesn't specify the platform.";
                } else {
                    message += "Possibly, " + problem.getProblemName() + " platform was deleted "
                            + " from configuration file or the format has changed";
                }
                JOptionPane.showMessageDialog(parent, message, Configuration.PROJECT_NAME, JOptionPane.ERROR_MESSAGE);
            } else {
                parent.addProblem(problem);
            }
        }
    }
}
