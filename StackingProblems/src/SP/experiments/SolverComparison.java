package SP.experiments;

import java.util.ArrayList;

/**
 * Provides an interface for different comparisons of solvers for stacking problems.
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
        TABU_SEARCH
    }

    String INSTANCE_PREFIX = "res/instances/";
    String SOLUTION_PREFIX = "res/solutions/";

    /************ CPLEX CONFIG ************/
    boolean HIDE_CPLEX_OUTPUT = true;
    // 1 --> feasibility over optimality
    int MIP_EMPHASIS = 1;
    // 0 tolerance --> only terminating with optimal solutions before time limit
    double MIP_TOLERANCE = 0.0;
    /**************************************/

    // Specifies the time limit for the solving procedure in seconds.
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
