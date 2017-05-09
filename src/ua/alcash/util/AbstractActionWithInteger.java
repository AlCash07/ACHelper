package ua.alcash.util;

import javax.swing.*;

/**
 * Created by oleksandr.bacherikov on 5/9/17.
 */
public abstract class AbstractActionWithInteger extends AbstractAction {
    private final int i;

    public AbstractActionWithInteger(int i) {
        super();
        this.i = i;
    }

    public int getInteger() {
        return i;
    }
}
