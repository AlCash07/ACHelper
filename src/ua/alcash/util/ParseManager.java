package ua.alcash.util;

import net.egork.chelper.parser.*;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.FileUtilities;
import ua.alcash.Configuration;
import ua.alcash.Problem;

import javax.xml.parsers.ParserConfigurationException;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created by oleksandr.bacherikov on 5/8/17.
 */
public class ParseManager {
    private static final Map<String, Parser> PLATFORM_ID_TO_PARSER;
    private static final Set<String> ALL_CONTEST_PLATFORMS;

    private static Map<String, String> platformNameToId = new HashMap<>();

    static {
        Map<String, Parser> platforms = new HashMap<>();
        platforms.put("atcoder", new AtCoderParser());
        platforms.put("bayan", new BayanParser());
        platforms.put("codechef", new CodeChefParser());
        platforms.put("codeforces", new CodeforcesParser());
        platforms.put("csacademy", new CSAcademyParser());
        platforms.put("facebook", new FacebookParser());
        platforms.put("gcj", new GCJParser());
        platforms.put("hackerearth", new HackerEarthParser());
        platforms.put("hackerrank", new HackerRankParser());
        platforms.put("kattis", new KattisParser());
        platforms.put("rcc", new RCCParser());
        platforms.put("timus", new TimusParser());
        platforms.put("usaco", new UsacoParser());
        platforms.put("yandex", new YandexParser());
        PLATFORM_ID_TO_PARSER = Collections.unmodifiableMap(platforms);

        ALL_CONTEST_PLATFORMS = new HashSet<>(Arrays.asList(
                "codechef", "codeforces", "gcj", "kattis", "rcc", "timus"));
    }

    public static void configure() {
        platformNameToId.clear();
        PLATFORM_ID_TO_PARSER.entrySet().forEach((entry) -> {
            String platformName = Configuration.getPlatform(entry.getKey());
            if (platformName != null) {
                platformNameToId.put(platformName, entry.getKey());
            }
        });
    }

    public static String getPlatformId(String platformName) {
        return platformNameToId.get(platformName);
    }

    public static List<String> getPlatformNames() {
        List<String> platformNames = new ArrayList<>();
        platformNameToId.entrySet().forEach((entry) -> {
            platformNames.add(entry.getKey());
        });
        return platformNames;
    }

    public static List<String> getContestPlatformNames() {
        List<String> platformNames = new ArrayList<>();
        platformNameToId.entrySet().forEach((entry) -> {
            if (ALL_CONTEST_PLATFORMS.contains(entry.getValue())) {
                platformNames.add(entry.getKey());
            }
        });
        return platformNames;
    }

    public static Problem parseProblem(String platformName, String url)
            throws MalformedURLException, ParserConfigurationException {
        Parser parser = PLATFORM_ID_TO_PARSER.get(platformNameToId.get(platformName));
        String problemText = FileUtilities.getWebPageContent(url);
        if (problemText == null) {
            throw new MalformedURLException();
        }
        Collection<Task> tasks = parser.parseTaskFromHTML(problemText);
        if (tasks.isEmpty()) {
            throw new ParserConfigurationException();
        }
        return new Problem(platformName, tasks.iterator().next());
    }
}
