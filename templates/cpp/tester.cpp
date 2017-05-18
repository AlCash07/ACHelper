#include <chrono>
#include <cstdlib>
#include <exception>
#include <fstream>
#include <future>
#include <iomanip>
#include <iostream>
#include <memory>
#include <string>
#include <sstream>
#include <thread>
#include <utility>
#include <vector>

extern void solve(std::istream& in, std::ostream& out);

void colorOutput(const std::string& output, bool green) {
    std::cout << (green ? "\033[32;1m" : "\033[31;1m");
    std::cout << output << "\033[0m";
}

int main(int argc, const char* argv[]) {
    using namespace std;
    ios_base::sync_with_stdio(0);

    // read the path to tests list file
    string binaryPath = argv[0];
    if (binaryPath.substr(max(binaryPath.size(), static_cast<size_t>(4)) - 4) == ".exe") {
        binaryPath = binaryPath.substr(0, binaryPath.size() - 4);
    }
    ifstream pathFile(binaryPath + "Path");
    string testsFileName;
    pathFile >> testsFileName;
    pathFile.close();
    string testsDirectory = testsFileName.substr(0, testsFileName.find_last_of("/\\") + 1);

    // read test names
    vector<vector<string>> testCases;
    ifstream testsListInputFile(testsFileName);
    string line;
    while (getline(testsListInputFile, line)) {
        testCases.emplace_back();
        auto spaceIndex = line.find(' ');
        testCases.back().push_back(line.substr(0, spaceIndex));
        testCases.back().push_back(spaceIndex < line.size() ? line.substr(spaceIndex + 1) : "");
    }
    testsListInputFile.close();
    auto isSkipped = [&testCases](size_t testIndex) {
        return testCases[testIndex][1] == "SKIPPED";
    };

//#ifndef NDEBUG
#if false
    // in debug mode, tests are run without validation and updating tests list file
    for (size_t testIndex = 0, testCount = testCases.size(); testIndex < testCount; ++testIndex) {
        if (isSkipped(testIndex)) continue;
        string testFileName = testsDirectory + testCases[testIndex][0];
        ifstream in(testFileName + ".in");
        ofstream out(testFileName + ".out");
        solve(in, out);
    }
#else
    int testsTotal = 0, testsPassed = 0;
    auto allowedDetachedThreadCount = max(2U, thread::hardware_concurrency()) - 2;
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
        exception_ptr exceptionPtr = nullptr;
        auto runTest = [&testFileName, &exceptionPtr]() {
            ifstream in(testFileName + ".in");
            ofstream out(testFileName + ".out");
            try {
                solve(in, out);
            } catch (...) {
                exceptionPtr = current_exception();
            }
        };
        thread thread(runTest);
        auto start = chrono::system_clock::now();
        auto future = async(launch::async, &thread::join, &thread);
        auto futureStatus = future.wait_for(chrono::seconds(1));
        auto end = chrono::system_clock::now();
        auto duration = chrono::duration_cast<chrono::milliseconds>(end - start);
        double elapsed = static_cast<double>(duration.count()) / 1000;

        // write results to tests list file
        ostringstream resultStream;
        resultStream << fixed << setprecision(3);
        bool abortTesting = false;
        if (futureStatus == future_status::timeout) {  // time limit exceeded
            resultStream << "TLE " << elapsed;
            thread.detach();
            if (allowedDetachedThreadCount == 0) {
                abortTesting = true;
            } else {
                --allowedDetachedThreadCount;
            }
        } else {
            if (exceptionPtr) {  // runtime error
                try {
                    rethrow_exception(exceptionPtr);
                } catch (const exception& exception) {
                    resultStream << "RE " << elapsed << ' ' << exception.what();
                }
            } else {  // run checker
                /*
                string checkerCommand = binaryPath + "Checker " + testFileName + ".in"
                    + testFileName + ".out" + testFileName + ".ans";
#ifdef _MSC_VER
                shared_ptr<FILE> pipe(_popen(checkerCommand.data(), "r"), _pclose);
#else
                shared_ptr<FILE> pipe(popen(checkerCommand.data(), "r"), pclose);
#endif
                if (!pipe) throw std::runtime_error("popen() failed!");
                const int CHUNK_SIZE = 128;
                char buffer[CHUNK_SIZE];
                string checkerOutput;
                while (!feof(pipe.get())) {
                    if (fgets(buffer, CHUNK_SIZE, pipe.get()) != 0)
                        checkerOutput += buffer;
                }
                 */
                resultStream << "OK " << elapsed;
                ++testsPassed;
            }
        }
        line = testCases[testIndex][1] = resultStream.str();
        auto spaceIndex = line.find(' ');
        string verdict = line.substr(0, spaceIndex);
        colorOutput(verdict, verdict == "OK");
        cout << line.substr(spaceIndex) << endl;

        if (abortTesting) {
            colorOutput("ABORTING", false);
            cout << std::endl;
            for (; ++testIndex < testCount;) {
                if (!isSkipped(testIndex)) testCases[testIndex][1] = "UNKNOWN";
            }
            --testIndex;  // to make the last iteration that writes to file
        }
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
