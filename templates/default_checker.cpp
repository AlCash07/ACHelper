#include "testlib.h"
#include <regex>

bool convertToDouble(const std::string src, double& dst) {
    char* endptr = 0;
    dst = std::strtod(src.data(), &endptr);
    return !(*endptr != '\0' || endptr == src.data());
}

bool compareDoubles(double expected, double result) {
    if (std::isnan(expected)) return std::isnan(result);
    if (std::isinf(expected)) return expected == result;
    if (std::isnan(result) || std::isinf(result)) return false;
    bool equal = false;
#ifdef ABS
    equal = equal || std::abs(expected - result) < EPS;
#endif
#ifdef REL
    equal = equal || std::abs((expected - result) / expected) < EPS;
#endif
    return equal;
}

std::string outputCharacter(char c) {
    if (c) return std::string("'") + c + "'";
    else return "null";
}

int main(int argc, char* argv[]) {
    const int maxTokenLengthToOutput = 22;  // to fit standard integer numbers

    registerTestlibCmd(argc, argv);

    int index = 0;
    while (!ans.seekEof() && !ouf.seekEof()) {
        ++index;
        std::string answer = ans.readToken();
        std::string output = ouf.readToken();
        // integers are matched as regular strings to avoid converting big integers to doubles with precision loss
        std::regex integerRegex(R"~([+-]?\d+)~");
        double doubleAnswer, doubleOutput;
        if (!std::regex_match(answer.data(), integerRegex)
            && convertToDouble(answer, doubleAnswer)) {
            if (!convertToDouble(output, doubleOutput) || !compareDoubles(doubleAnswer, doubleOutput)) {
                if (answer.size() > maxTokenLengthToOutput) {
                    answer = answer.substr(0, maxTokenLengthToOutput - 3) + "...";
                }
                if (output.size() > maxTokenLengthToOutput) {
                    output = output.substr(0, maxTokenLengthToOutput - 3) + "...";
                }
                quitf(_wa, "%d%s floating point numbers differ - expected: '%s', found: '%s'",
                    index, englishEnding(index).data(), answer.data(), output.data());
            }
        } else if (answer != output) {
            if (answer.size() <= maxTokenLengthToOutput && output.size() <= maxTokenLengthToOutput) {
                quitf(_wa, "%d%s tokens differ - expected: '%s', found: '%s'",
                    index, englishEnding(index).data(), answer.data(), output.data());
            } else {
                int diff = 0;
                while (answer[diff] == output[diff]) ++diff;
                quitf(_wa, "%d%s tokens differ in %d%s character - expected: %s, found: %s",
                    index, englishEnding(index).data(),
                    diff + 1, englishEnding(diff + 1).data(),
                    outputCharacter(answer[diff]).data(), outputCharacter(output[diff]).data());
            }
        }
    }

    int answerCount = index;
    while (!ans.seekEof()) {
        ans.readLong();
        ++answerCount;
    }

    int outputCount = index;
    while (!ouf.seekEof()) {
        ouf.readLong();
        ++outputCount;
    }

    if (answerCount != outputCount) {
        quitf(_wa, "token count differs - expected: %d, found: %d", answerCount, outputCount);
    }

    quitf(_ok, "%d tokens", index);
}
