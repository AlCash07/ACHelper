#include <iostream>

class Solver {
public:
    void solve(std::istream& in, std::ostream& out) {
    }
};

void solve(std::istream& in, std::ostream& out) {
    int testIndex = 1, testCount;
    for (in >> testCount; testIndex <= testCount; ++testIndex) {
        out << "Case #" << testIndex << ": ";
        Solver solver;
        solver.solve(in, out);
    }
}
