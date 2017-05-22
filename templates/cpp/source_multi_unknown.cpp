#include "solution.h"

class @problem_id@Solver {
public:
    bool solve(std::istream& in, std::ostream& out) {
        if (!in.good()) return false;
        return true;
    }
};

void solve(std::istream& in, std::ostream& out) {
    while (true) {
        @problem_id@Solver solver;
        if (!solver.solve(in, out)) break;
    }
}
