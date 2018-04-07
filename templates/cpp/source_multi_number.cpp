#include "solution.h"

class @problem_id@Solver {
public:
    void solve(std::istream& in, std::ostream& out) {
    }
};

void solve(std::istream& in, std::ostream& out) {
    int testIndex = 1, testCount;
    for (in >> testCount; testIndex <= testCount; ++testIndex) {
#if !@interactive@
        out << "Case #" << testIndex << ": ";
#endif
        @problem_id@Solver solver;
        solver.solve(in, out);
    }
}
