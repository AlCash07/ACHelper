#include <chrono>
#include <cstdlib>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <memory>
#include <sstream>
#include <string>
#include <vector>

#include <signal.h>
#include <sys/time.h>
#include <sys/wait.h>
#include <unistd.h>

const double TIME_LIMIT = 1.5;
const double CHECKER_TIME_LIMIT = 1.0;

using namespace std;

extern void solve(istream& in, ostream& out);

void runTest(const string& testFileName) {
    ifstream in(testFileName + ".in");
    ofstream out(testFileName + ".out");
    solve(in, out);
};

// std::getline analog for file descriptors
bool getline(FILE* file, string& line) {
    line.clear();
    while (true) {
        int c = fgetc(file);
        if (c == EOF || c == '\n') break;
        line += c;
    }
    return !line.empty();
}

// return codes:
//  0 : success
// -1 : execution failed
//  1 : runtime error (error string may be TLE indicating time limit exceeded)
template <class Function>
int executeProcess(Function f, double timeLimit, string& error) {
    int pipefd[2];
    pipe(pipefd);
    auto pid = fork();
    if (pid == 0) {  // child process
        close(pipefd[0]);  // close input pipe
        dup2(pipefd[1], STDERR_FILENO);  // redirect stderr to output pipe
        signal(SIGALRM, [](int sigNum) { exit(sigNum); });  // exit after timer
        itimerval itimer;
        itimer.it_interval = {0, 0};
        itimer.it_value.tv_sec = static_cast<time_t>(timeLimit);
        itimer.it_value.tv_usec = static_cast<suseconds_t>((timeLimit - itimer.it_value.tv_sec) * 1000000);
        if (setitimer(ITIMER_REAL, &itimer, nullptr) != 0) {
            exit(SIGKILL);  // failed to start timer
        }
        f();  // function is executed here
        exit(0);
    } else if (pid < 0) {  // fork failed
        return -1;
    }
    // parent process
    close(pipefd[1]);  // close output pipe
    int status = 0;
    int returnedPid = waitpid(pid, &status, 0);
    if (returnedPid != pid) {
        kill(pid, SIGKILL);
    }
    FILE* errorPipe = fdopen(pipefd[0], "r");
    getline(errorPipe, error);  // read stderr message
    fclose(errorPipe);
    if (returnedPid == pid && status == 0) {
        return 0;
    }
    if (WEXITSTATUS(status) == SIGALRM) {
        error = "TLE";
    }
    return 1;
}

void colorOutput(const string& output, bool green) {
    cout << (green ? "\033[32;1m" : "\033[31;1m");
    cout << output << "\033[0m";
}

int main(int argc, const char* argv[]) {
    using namespace std;
    ios_base::sync_with_stdio(0);

    // read the path to tests list file
    string binaryPath = argv[0];
    ifstream pathFile(binaryPath + "Path");
    string testsFileName;
    pathFile >> testsFileName;
    pathFile.close();
    string testsDirectory = testsFileName.substr(0, testsFileName.find_last_of("/\\") + 1);

    // read test names
    vector<vector<string>> testCases;
    FILE* testsListInputFile = fopen(testsFileName.data(), "r");
    string line;
    while (getline(testsListInputFile, line)) {
        testCases.emplace_back();
        auto spaceIndex = line.find(' ');
        testCases.back().push_back(line.substr(0, spaceIndex));
        testCases.back().push_back(spaceIndex < line.size() ? line.substr(spaceIndex + 1) : "");
    }
    fclose(testsListInputFile);
    auto isSkipped = [&testCases](size_t testIndex) {
        return testCases[testIndex][1] == "SKIPPED";
    };

//#ifndef NDEBUG
#if false
    // in debug mode, tests are run without validation and updating tests list file
    for (size_t testIndex = 0, testCount = testCases.size(); testIndex < testCount; ++testIndex) {
        if (isSkipped(testIndex)) continue;
        cout << testCases[testIndex][0] << ": running" << endl;
        runTest(testsDirectory + testCases[testIndex][0]);
    }
#else
    int testsTotal = 0, testsPassed = 0;
    for (size_t testIndex = 0, testCount = testCases.size();; ++testIndex) {
        if (testIndex < testCount) {
            cout << testCases[testIndex][0] << ": ";
            if (isSkipped(testIndex)) {
                cout << "skipping" << endl;
                continue;
            }
            cout << "running" << endl;
            ++testsTotal;
        }

        // update tests list file
        ofstream testsListFile(testsFileName);
        for (size_t i = 0; i < testIndex; ++i) {
            testsListFile << testCases[i][0] << ' ' << testCases[i][1] << '\n';
        }
        if (testIndex < testCount) {
            testsListFile << testCases[testIndex][0] << " RUNNING\n";
        }
        for (size_t i = testIndex + 1; i < testCount; ++i) {
            testsListFile << testCases[i][0] << " PENDING\n";
        }
        testsListFile.close();
        if (testIndex == testCount) break;

        // run the test as a subprocess and measure time
        string testFileName = testsDirectory + testCases[testIndex][0];
        auto start = chrono::system_clock::now();
        string error;
        int status = executeProcess([&testFileName](){
            runTest(testFileName);
        }, TIME_LIMIT, error);
        auto end = chrono::system_clock::now();
        double elapsed = chrono::duration_cast<chrono::duration<double>>(end - start).count();

        ostringstream resultStream;
        resultStream << fixed << setprecision(3);
        if (status == 0) {  // run checker
            string checkerCommand = binaryPath + "Checker " + testFileName + ".in "
                + testFileName + ".out " + testFileName + ".ans";
            status = executeProcess([&checkerCommand]() {
//                execl(checkerCommand.data());
            }, CHECKER_TIME_LIMIT, error);
            if (status == 0) {
                resultStream << "OK " << elapsed;
                ++testsPassed;
            } else if (status == 1) {  // either WA or checker crashed
                if (error == "TLE") {
                    status = -1;
                } else {
                    resultStream << "WA " << elapsed << ' ' << error;
                }
            }
        } else if (status == 1) {
            if (error == "TLE") {
                resultStream << "TLE " << elapsed;
            } else {
                resultStream << "RE " << elapsed << ' ' << error;
            }
        }
        if (status == -1) {
            resultStream << "JE " << elapsed << " judgement error";
        }
        line = testCases[testIndex][1] = resultStream.str();
        auto spaceIndex = line.find(' ');
        string verdict = line.substr(0, spaceIndex);
        colorOutput(verdict, verdict == "OK");
        cout << line.substr(spaceIndex) << endl;
    }
    cout << "========================================\n";
    if (testsPassed == testsTotal) {
        colorOutput("All tests passed:", true);
    } else {
        colorOutput("Not all tests passed:", false);
    }
    cout << " " << testsPassed << " / " << testsTotal << "\n";
#endif
    return 0;
}
