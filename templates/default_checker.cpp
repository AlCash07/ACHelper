#include "testlib.h"
#include <limits>
#include <regex>

// returns false for integers to avoid comparing big integers as doubles with precision loss
bool convertToDouble(const std::string src, double& dst) {
    char* endptr = 0;
    dst = std::strtod(src.data(), &endptr);
    if (*endptr != '\0') {
        dst = std::numeric_limits<double>::quiet_NaN();
        return false;
    }
    static std::regex integerRegex(R"~([+-]?\d+)~");
    return !std::regex_match(src.data(), integerRegex);
}

bool compareDoubles(double expected, double result) {
    if (std::isnan(expected)) return std::isnan(result);
    if (std::isinf(expected)) return expected == result;
    if (std::isnan(result) || std::isinf(result)) return false;
    bool equal = false;
#ifdef ABSOLUTE
    equal = equal || std::abs(expected - result) < EPSILON;
#endif
#ifdef RELATIVE
    equal = equal || std::abs((expected - result) / expected) < EPSILON;
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
        double doubleAnswer, doubleOutput;
        bool isAnswerDouble = convertToDouble(answer, doubleAnswer);
        bool isOutputDouble = convertToDouble(output, doubleOutput);
        if (isAnswerDouble || isOutputDouble) {
            if (!compareDoubles(doubleAnswer, doubleOutput)) {
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
