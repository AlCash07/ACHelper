package ua.alcash;

import java.util.Collection;

/**
 * Created by oleksandr.bacherikov on 5/24/17.
 */
public interface ProblemsReceiver {
    void receiveProblems(Collection<Problem> problems);

    void receiveError(String message);
}
