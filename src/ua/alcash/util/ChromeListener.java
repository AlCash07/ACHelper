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

/**
 * Created by oleksandr.bacherikov on 5/11/17.
 */
public class ChromeListener implements Runnable {
    ProblemSetPane parent;

    private ServerSocket serverSocket;

    public ChromeListener(ProblemSetPane parent) {
        this.parent = parent;
    }

    public void start(String portString) {
        stop();
        if (portString == null) {
            return;
        }
        System.out.println(portString);
        try {
            int port = Integer.parseInt(portString);
            serverSocket = new ServerSocket(port);
            new Thread(this, "ChromeListenerThread").start();
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(parent,
                    "Could not create serverSocket for Chrome parser," +
                            "probably another CHelper-eligible project is running.",
                    Configuration.PROJECT_NAME,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (Throwable exception) {
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (serverSocket.isClosed())
                    return;
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
                        final Problem problem = ParseManager.parseProblemFromHtml(platformId, page);
                        SwingUtilities.invokeLater(() -> {
                            parent.addProblem(problem);
                        });
                    } catch (ParserConfigurationException exception) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(parent, getErrorMessage(platformId),
                                    Configuration.PROJECT_NAME,
                                    JOptionPane.ERROR_MESSAGE);
                        });
                    }
                } finally {
                    socket.close();
                }
            } catch (Throwable exception) {
            }
        }
    }

    private String getErrorMessage(String platformId) {
        String message = "Failed to parse message from CHelper Chrome extension.\n";
        if (platformId.isEmpty()) {
            message += "Message is empty.";
        } else {
            message += "Possibly, " + platformId + " platform was deleted from configuration file"
                    + " or the format has changed";
        }
        return message;
    }
}
