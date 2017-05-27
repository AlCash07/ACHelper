package ua.alcash.network;

import ua.alcash.Problem;
import ua.alcash.parsing.ParseManager;
import ua.alcash.ui.MainFrame;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;

/**
 * Created by Al.Cash on 5/11/17.
 */
public class ChromeListener implements Runnable {
    private MainFrame receiver;
    private ServerSocket serverSocket;

    public ChromeListener(MainFrame receiver) { this.receiver = receiver; }

    public void start(String portString) {
        stop();
        if (portString == null) {
            return;
        }
        try {
            int port = Integer.parseInt(portString);
            serverSocket = new ServerSocket(port);
            new Thread(this, "ChromeListenerThread").start();
        } catch (IOException exception) {
            receiver.receiveError("Could not create serverSocket for Chrome parser, " +
                            "probably another CHelper-eligible project is running.");
        }
    }

    public void stop() {
        try {
            serverSocket.close();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void run() {
        while (true) try {
            if (serverSocket.isClosed())
                return;
            try (Socket socket = serverSocket.accept()) {
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
                    final Collection<Problem> problems = ParseManager.parseProblemsFromHtml(platformId, page);
                    SwingUtilities.invokeLater(() -> receiver.receiveProblems(problems));
                } catch (ParserConfigurationException exception) {
                    SwingUtilities.invokeLater(() -> receiver.receiveError(getErrorMessage(platformId)));
                }
            }
        } catch (Throwable ignored) {
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
