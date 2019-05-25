package SP.experiments;

import SP.representations.Solvers;

import java.util.ArrayList;

/**
 * Used to start configure / start the solver comparison.
 *
 * @author Tim Bohne
 */
public class CompareSolvers {

    public static String INSTANCE_PREFIX = "res/instances/";
    public static String SOLUTION_PREFIX = "res/solutions/";

    /************ CPLEX CONFIG ************/
    public static boolean HIDE_CPLEX_OUTPUT = true;
    // 1 --> feasibility over optimality
    public static int MIP_EMPHASIS = 1;
    // 0 tolerance --> only terminating with optimal solutions before time limit
    public static double MIP_TOLERANCE = 0.0;
    /**************************************/

    // Specifies the time limit for the solving procedure in seconds.
    public static double TIME_LIMIT = 300;

    // The 3Cap heuristic has an option to prioritize the runtime which can be set here.
    public static boolean PRIORITIZE_RUNTIME = false;
    // 2Cap and 3Cap provide post processing procedures that can be enabled here.
    public static boolean POST_PROCESSING = true;

    public static int THRESHOLD_LB = 20;
    public static int THRESHOLD_UB = 75;
    public static int STEP_SIZE = 5;
    public static float SPLIT_PAIRS_DIVISOR = 3.4F;
    public static float PENALTY_FACTOR = 5.0F;

    public static void main(String[] args) {

        ArrayList<Solvers.Solver> solversToBeUsed = new ArrayList<>();
//        solversToBeUsed.add(SolverComparison.Solver.MIP_BINPACKING);
//        solversToBeUsed.add(SolverComparison.Solver.MIP_THREEINDEX);
//        solversToBeUsed.add(SolverComparison.Solver.CONSTRUCTIVE_TWO_CAP);
        solversToBeUsed.add(Solvers.Solver.CONSTRUCTIVE_THREE_CAP);

//         SolverComparison2Cap twoCap = new SolverComparison2Cap();
//         twoCap.compareSolvers(solversToBeUsed);

        SolverComparison3Cap threeCap = new SolverComparison3Cap(
            SOLUTION_PREFIX, INSTANCE_PREFIX, TIME_LIMIT, HIDE_CPLEX_OUTPUT, MIP_EMPHASIS, MIP_TOLERANCE,
            POST_PROCESSING, PRIORITIZE_RUNTIME, THRESHOLD_LB, THRESHOLD_UB, STEP_SIZE, SPLIT_PAIRS_DIVISOR, PENALTY_FACTOR
        );
       threeCap.compareSolvers(solversToBeUsed);
    }
}
