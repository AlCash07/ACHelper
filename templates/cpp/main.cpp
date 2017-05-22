#include "solution.h"

#include <cstdio>

int main() {
    std::cin.tie(0);
    std::ios_base::sync_with_stdio(false);

    const char* inputFileName = "@input_file_name@";
    const char* outputFileName = "@output_file_name@";

    if (*inputFileName) std::freopen(inputFileName, "r", stdin);
    if (*outputFileName) std::freopen(outputFileName, "w", stdout);

    solve(std::cin, std::cout);
    return 0;
}
