package ua.alcash.filesystem;

import com.google.common.io.CharStreams;
import ua.alcash.Configuration;
import ua.alcash.Problem;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by oleksandr.bacherikov on 5/23/17.
 */
public class Generator {
    public static void generate(String directory, Collection<Problem> problems, boolean project) {
        if (!project) {
            for (Problem problem : problems) {
                project = project || problem.projectRegenerationRequired();
            }
        }
        generateFiles(directory, problems);
        if (project) {
            generateProject(directory, problems);
        }
    }

    private static void generateFiles(String directory, Collection<Problem> problems) {
        configureFile(directory, Configuration.get("generate files template"), problems);
        executeCommand(directory, Configuration.get("generate files command"));
    }

    private static void generateProject(String directory, Collection<Problem> problems) {
        configureFile(directory, Configuration.get("generate project template"), problems);
        executeCommand(directory, Configuration.get("generate project command"));
    }

    private static void configureFile(String directory, String templatePath, Collection<Problem> problems) {
        String delimiter = Configuration.get("problems delimiter");
        Path inputPath = Paths.get(templatePath);
        Path outputPath = Paths.get(directory, inputPath.getFileName().toString());
        try {
            List<String> inputLines = Files.readAllLines(inputPath);
            ArrayList<String> outputLines = new ArrayList<>();
            for (int i = 0; i < inputLines.size(); ++i) {
                if (inputLines.get(i).equals(delimiter)) {
                    ++i;
                    int last = i;
                    while (last < inputLines.size() && !inputLines.get(last).equals(delimiter)) {
                        ++last;
                    }
                    for (Problem problem : problems) {
                        for (int j = i; j < last; ++j) {
                            outputLines.add(problem.substituteKeys(inputLines.get(j), false));
                        }
                    }
                    i = last;
                } else {
                    String[] tokens = inputLines.get(i).split("@");
                    for (int j = 1; j < tokens.length; j += 2) {
                        tokens[j] = Configuration.get(tokens[j]);
                    }
                    outputLines.add(String.join("", tokens));
                }
            }
            Files.write(outputPath, outputLines);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(null,
                    outputPath.getFileName() + " file generation failed:\n" + exception.getMessage(),
                    Configuration.PROJECT_NAME,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void executeCommand(String directory, String command) {
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(command, null, new File(directory));
            System.out.println(CharStreams.toString(new InputStreamReader(process.getInputStream())));
            System.err.println(CharStreams.toString(new InputStreamReader(process.getErrorStream())));
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(null,
                    "Failed to execute command " + command + "\n" + exception.getMessage(),
                    Configuration.PROJECT_NAME,
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
