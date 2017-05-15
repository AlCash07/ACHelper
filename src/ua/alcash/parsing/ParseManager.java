package ua.alcash.parsing;

import net.egork.chelper.parser.*;
import net.egork.chelper.task.Task;
import net.egork.chelper.util.FileUtilities;
import org.jetbrains.annotations.NotNull;
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

    private static final Map<String, String> PLATFORM_ID_TO_URL;
    private static final Map<String, String> PLATFORM_ID_TO_CONTEST_URL;

    private static Map<String, String> platformIdToName = new HashMap<>();

    static {
        Map<String, Parser> platforms = new HashMap<>();
        platforms.put("atcoder", new AtCoderParser());
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

        Map<String, String> platformUrls = new HashMap<>();
        platformUrls.put("atcoder", "atcoder.jp");
        platformUrls.put("codechef", "codechef.com");
        platformUrls.put("codeforces", "codeforces.com");
        platformUrls.put("csacademy", "csacademy.com");
        platformUrls.put("facebook", "facebook.com/hackercup");
        platformUrls.put("gcj", "code.google.com/codejam");
        platformUrls.put("hackerearth", "hackerearth.com");
        platformUrls.put("hackerrank", "hackerrank.com");
        platformUrls.put("kattis", "open.kattis.com");
        platformUrls.put("rcc", "russiancodecup.ru");
        platformUrls.put("timus", "acm.timus.ru");
        platformUrls.put("usaco", "usaco.org");
        platformUrls.put("yandex", "contest.yandex.ru");
        PLATFORM_ID_TO_URL = Collections.unmodifiableMap(platformUrls);

        Map<String, String> contestUrls = new HashMap<>();
        contestUrls.put("codechef", "www.codechef.com/");
        contestUrls.put("codeforces", "codeforces.com/contest/");
        contestUrls.put("gcj", "code.google.com/codejam/contest/");
        contestUrls.put("kattis", "open.kattis.com/contests/");
        contestUrls.put("rcc", "russiancodecup.ru/championship/round/");
        contestUrls.put("timus", "acm.timus.ru/problemset.aspx?space=");
        PLATFORM_ID_TO_CONTEST_URL = Collections.unmodifiableMap(contestUrls);
    }

    public static void configure() {
        platformIdToName.clear();
        PLATFORM_ID_TO_PARSER.entrySet().forEach(entry -> {
            String platformName = Configuration.getPlatform(entry.getKey());
            if (platformName != null) {
                platformIdToName.put(entry.getKey(), platformName);
            }
        });
    }

    public static String getPlatformName(String platformId) { return platformIdToName.get(platformId); }

    public static List<String> getPlatformIds() {
        List<String> platformIds = new ArrayList<>();
        platformIdToName.entrySet().forEach(entry -> {
            platformIds.add(entry.getKey());
        });
        return platformIds;
    }

    private static String getPlatformByUrl(String url) throws MalformedURLException {
        List<String> platformIds = new ArrayList<>();
        PLATFORM_ID_TO_URL.entrySet().forEach(entry -> {
            if (url.contains(entry.getValue())) platformIds.add(entry.getKey());
        });
        if (platformIds.isEmpty()) {
            throw new MalformedURLException("Unrecognized platform.");
        } else if (platformIds.size() > 1) {
            throw new MalformedURLException("Ambiguous platform.");
        } else {
            return platformIds.get(0);
        }
    }

    @NotNull
    public static Collection<Problem> parseProblemsFromHtml(String platformId, String page)
            throws ParserConfigurationException {
        if (!platformIdToName.containsKey(platformId)) {
            throw new ParserConfigurationException("Unsupported platform.");
        }
        Parser parser = PLATFORM_ID_TO_PARSER.get(platformId);
        Collection<Task> tasks = parser.parseTaskFromHTML(page);
        if (tasks.isEmpty()) {
            throw new ParserConfigurationException("Parsing failed.");
        }
        ArrayList<Problem> problems = new ArrayList<>();
        for (Task task : tasks) {
            problems.add(new Problem(platformId, task));
        }
        return problems;
    }

    @NotNull
    public static Collection<Problem> parseProblemByUrl(String url)
            throws MalformedURLException, ParserConfigurationException {
        String platformId = getPlatformByUrl(url);
        String html = FileUtilities.getWebPageContent(url);
        if (html == null) {
            throw new MalformedURLException("Unable to get web page content.");
        }
        return parseProblemsFromHtml(platformId, html);
    }

    private static class ContestReceiver implements DescriptionReceiver {
        String platformId;
        Parser parser;
        Collection<Problem> problems = new ArrayList<>();

        ContestReceiver(String platformId, Parser parser) {
            this.platformId = platformId;
            this.parser = parser;
        }

        @Override
        public boolean isStopped() { return false; }

        @Override
        public void receiveDescriptions(Collection<Description> descriptions) {
            for (Description description : descriptions) {
                problems.add(new Problem(platformId, parser.parseTask(description)));
            }
        }
    }

    @NotNull
    public static Collection<Problem> parseContestByUrl(String url)
            throws MalformedURLException, ParserConfigurationException {
        String platformId = getPlatformByUrl(url);
        if (!PLATFORM_ID_TO_CONTEST_URL.containsKey(platformId)) {
            throw new MalformedURLException("Contest parsing is not supported for " + platformId);
        }
        String contestUrl = PLATFORM_ID_TO_CONTEST_URL.get(platformId);
        int index = url.indexOf(contestUrl);
        if (index == -1) {
            throw new MalformedURLException("Invalid contest url for " + platformId);
        }
        String contestId = url.substring(index + contestUrl.length());
        contestId = contestId.replaceFirst("[\\$\\?#/\\\\].*", "");
        Parser parser = PLATFORM_ID_TO_PARSER.get(platformId);
        ContestReceiver contestReceiver = new ContestReceiver(platformId, parser);
        parser.parseContest(contestId, contestReceiver);
        return contestReceiver.problems;
    }
}
