package ua.alcash.filesystem;

import com.google.common.io.CharStreams;
import ua.alcash.Configuration;

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
 * Created by Al.Cash on 5/23/17.
 */
class Generator {
    static void generate(String directory, Collection<ProblemSync> problemSyncs, boolean project) throws IOException {
        if (!project) {
            for (ProblemSync problemSync : problemSyncs) {
                project = project || problemSync.projectRegenerationRequired();
            }
        }
        configureFile(directory, Configuration.get("generate files template"), problemSyncs);
        executeCommand(directory, Configuration.get("generate files command"));
        if (project) {
            configureFile(directory, Configuration.get("generate project template"), problemSyncs);
            executeCommand(directory, Configuration.get("generate project command"));
        }
        for (ProblemSync problemSync : problemSyncs) {
            problemSync.markUnchanged();
        }
    }

    private static void configureFile(String directory, String templatePath, Collection<ProblemSync> problemSyncs)
            throws IOException {
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
                    for (ProblemSync problemSync : problemSyncs) {
                        for (int j = i; j < last; ++j) {
                            outputLines.add(problemSync.substituteKeys(inputLines.get(j), false));
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
            throw new IOException(outputPath.getFileName() + " file generation failed:\n" + exception.getMessage());
        }
    }

    private static void executeCommand(String directory, String command) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command, null, new File(directory));
        System.out.println(CharStreams.toString(new InputStreamReader(process.getInputStream())));
        System.err.println(CharStreams.toString(new InputStreamReader(process.getErrorStream())));
    }
}
