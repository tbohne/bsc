package SP.experiments;

import java.util.ArrayList;

/**
 * Defines several fields used to compare the different solvers.
 *
 * @author Tim Bohne
 */
public interface SolverComparison {

    /**
     * Enumeration containing the names of the different solvers.
     */
    enum Solver {
        MIP_BINPACKING,
        MIP_THREEINDEX,
        CONSTRUCTIVE_TWO_CAP,
        CONSTRUCTIVE_THREE_CAP,
        CONSTRUCTIVE_THREE_CAP_PERMUTATION,
        CONSTRUCTIVE_THREE_CAP_RECURSION
    }

    String INSTANCE_PREFIX = "res/instances/";
    String SOLUTION_PREFIX = "res/solutions/";

    // Specifies the time limit for the solving process in seconds.
    int TIME_LIMIT = 3600;

    /**
     * Compares the specified solvers (runtime, solution quality, ...).
     *
     * @param solversToBeCompared - determines the solvers that are supposed to be compared
     */
    void compareSolvers(ArrayList<Solver> solversToBeCompared);
}
