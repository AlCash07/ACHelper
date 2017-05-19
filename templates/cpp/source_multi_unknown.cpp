#include <iostream>

class Solver {
public:
    bool solve(std::istream& in, std::ostream& out) {
        if (!in.good()) return false;
        return true;
    }
};

void solve(std::istream& in, std::ostream& out) {
    while (true) {
        Solver solver;
        if (!solver.solve(in, out)) break;
    }
}
