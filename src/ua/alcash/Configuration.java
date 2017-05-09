package ua.alcash;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by oleksandr.bacherikov on 5/8/17.
 */
public class Configuration {
    static final public String PROJECT_NAME = "ACHelper";
    static final public String CONFIGURATION_FILE_NAME = PROJECT_NAME + ".xml";

    static protected Properties properties = new Properties();

    static public String get(String key) {
        return properties.getProperty(key);
    }

    static public KeyStroke getShortcut(String action) {
        return KeyStroke.getKeyStroke(get("shortcut " + action));
    }

    static public String getPlatform(String key) {
        return get("platform " + key);
    }

    static public String getExtension(String key) {
        return get("extension " + key);
    }

    static public boolean load(String workspaceDirectory) {
        boolean ok = true;
        try {
            FileInputStream input = new FileInputStream(
                    workspaceDirectory + java.io.File.separator + CONFIGURATION_FILE_NAME);
            properties.loadFromXML(input);
        } catch (IOException exception) {
            ok = false;
        }
        return ok;
    }
}
