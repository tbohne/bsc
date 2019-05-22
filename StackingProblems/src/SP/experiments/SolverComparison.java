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
        CONSTRUCTIVE_THREE_CAP_RECURSION,
        TABU_SEARCH
    }

    String INSTANCE_PREFIX = "res/instances/";
    String SOLUTION_PREFIX = "res/solutions/";

    // Specifies the time limit for the solving process in seconds.
    double TIME_LIMIT = 300;

    // The 3Cap heuristic has an option to prioritize the runtime which can be set here.
    boolean PRIORITIZE_RUNTIME = false;
    // 2Cap and 3Cap provide post processing procedures that can be enabled here.
    boolean POST_PROCESSING = true;

    /**
     * Compares the specified solvers (runtime, solution quality, ...).
     *
     * @param solversToBeCompared - determines the solvers that are supposed to be compared
     */
    void compareSolvers(ArrayList<Solver> solversToBeCompared);
}
