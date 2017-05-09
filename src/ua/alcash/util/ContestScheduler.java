package ua.alcash.util;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import ua.alcash.ui.MainFrame;

/**
 * Created by oleksandr.bacherikov on 5/8/17.
 */
public class ContestScheduler {
    private static final Timer timer = new Timer();

    public static void schedule(String url, String workingDirectory, MainFrame mainFrame, Date date) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
//                try {
//                    ContestParser parser = SupportedSites.getContestParser(url);
//                    ArrayList<Problem> problems = parser.getProblemListFromContestURL(url);
//                    mainFrame.addProblems(problems);
//                } catch (InterruptedException | ParserException ex) {
//                    new JOptionPane("Scheduled contest error " + ex.getMessage(), JOptionPane.ERROR_MESSAGE).createDialog(Configuration.PROJECT_NAME).setVisible(true);
//                    return;
//                }
//                new JOptionPane("Scheduled contest added.", JOptionPane.INFORMATION_MESSAGE).createDialog(Configuration.PROJECT_NAME).setVisible(true);

            }
        }, date);
    }
}
