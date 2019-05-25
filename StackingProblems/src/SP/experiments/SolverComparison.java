package SP.experiments;

import SP.constructive_heuristics.ThreeCapHeuristic;
import SP.constructive_heuristics.TwoCapHeuristic;
import SP.io.InstanceReader;
import SP.io.SolutionWriter;
import SP.mip_formulations.BinPackingFormulation;
import SP.mip_formulations.ThreeIndexFormulation;
import SP.post_optimization_methods.TabuSearch;
import SP.representations.Instance;
import SP.representations.Solution;
import SP.util.HeuristicUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class SolverComparison {

    /**
     * Enumeration containing the names of the different solvers.
     */
    public enum Solver {
        MIP_BINPACKING,
        MIP_THREEINDEX,
        CONSTRUCTIVE_TWO_CAP,
        CONSTRUCTIVE_THREE_CAP,
        TABU_SEARCH
    }

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

    /**
     * Computes a lower bound for the specified instance using the LowerBoundsCalculator.
     *
     * @param instance     - the instance to compute a LB for
     * @param solutionName - the name of the generated solution
     */
    public static void computeLowerBound(Instance instance, String solutionName) {
        LowerBoundsCalculator lbc = new LowerBoundsCalculator(instance);
        double lowerBound = lbc.computeLowerBound();
        SolutionWriter.writeLowerBound(SOLUTION_PREFIX + solutionName + ".txt", lowerBound);
        SolutionWriter.writeLowerBoundAsCSV(SOLUTION_PREFIX + "solutions.csv", lowerBound, instance.getName());
    }

    /**
     * Solves the given instance with the bin-packing solver.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public static void solveWithBinPacking(Instance instance, String solutionName) {
        BinPackingFormulation binPackingFormulation = new BinPackingFormulation(
                instance, TIME_LIMIT, HIDE_CPLEX_OUTPUT, MIP_EMPHASIS, MIP_TOLERANCE
        );
        Solution sol = binPackingFormulation.solve();
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, getNameOfSolver(Solver.MIP_BINPACKING));
        SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, getAbbreviatedNameOfSolver(Solver.MIP_BINPACKING));
    }

    /**
     * Solves the given instance with the three-index solver.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public static void solveWithThreeIdx(Instance instance, String solutionName) {
        ThreeIndexFormulation threeIndexFormulation = new ThreeIndexFormulation(
                instance, TIME_LIMIT, HIDE_CPLEX_OUTPUT, MIP_EMPHASIS, MIP_TOLERANCE
        );
        Solution sol = threeIndexFormulation.solve();
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, getNameOfSolver(Solver.MIP_THREEINDEX));
        SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, getAbbreviatedNameOfSolver(Solver.MIP_THREEINDEX));
    }

    /**
     * Workaround that triggers the ClassLoader to load all classes that are needed during the solving procedure.
     * Otherwise the first instance to be solved would have a longer runtime because of the initial class loading.
     *
     * @param instanceFile - the file to load the instance from
     */
    public static void prepareRuntimeMeasurementByPreLoadingAllClasses(File instanceFile) {
        String instanceName = instanceFile.toString().replace("res/instances/", "").replace(".txt", "");
        Instance instance = InstanceReader.readInstance(INSTANCE_PREFIX + instanceName + ".txt");
        String solutionName = instanceName.replace("instance", "sol");
        computeLowerBound(instance, solutionName);
    }

    /**
     * Returns the name of the solver used to create the solution.
     *
     * @param solver - specifies the used solver
     * @return name of the used solver
     */
    public static String getNameOfSolver(SolverComparison.Solver solver) {
        switch (solver) {
            case MIP_BINPACKING:
                return BinPackingFormulation.class.getName();
            case MIP_THREEINDEX:
                return ThreeIndexFormulation.class.getName();
            case CONSTRUCTIVE_TWO_CAP:
                return TwoCapHeuristic.class.getName();
            case CONSTRUCTIVE_THREE_CAP:
                return ThreeCapHeuristic.class.getName();
            case TABU_SEARCH:
                return TabuSearch.class.getName();
            default:
                return "";
        }
    }

    /**
     * Returns the abbreviated name of the solver used to create the solution.
     *
     * @param solver - specifies the used solver
     * @return abbreviated name of the used solver
     */
    public static String getAbbreviatedNameOfSolver(SolverComparison.Solver solver) {
        switch (solver) {
            case MIP_BINPACKING:
                return "BinP";
            case MIP_THREEINDEX:
                return "3Idx";
            case CONSTRUCTIVE_TWO_CAP:
                return "2Cap";
            case CONSTRUCTIVE_THREE_CAP:
                return "3Cap";
            case TABU_SEARCH:
                return "TS";
            default:
                return "";
        }
    }

    /**
     * Compares the different solvers for a stack capacity of 2.
     *
     * @param solversToBeCompared - determines the solvers that are supposed to be compared
     */
    public static void compareSolvers(ArrayList<SolverComparison.Solver> solversToBeCompared) {
        File dir = new File(INSTANCE_PREFIX);
        File[] directoryListing = dir.listFiles();
        Arrays.sort(directoryListing);
        String allSol = HeuristicUtil.createStringContainingAllSolutionNames(SOLUTION_PREFIX);

        if (directoryListing != null) {
            for (File file : directoryListing) {

                if (file.toString().contains("slp_instance_") && !allSol.contains(file.toString().replace("res/instances/", ""))) {
                    String instanceName = file.toString().replace("res/instances/", "").replace(".txt", "");
                    System.out.println("working on: " + instanceName);
                    Instance instance = InstanceReader.readInstance(INSTANCE_PREFIX + instanceName + ".txt");
                    String solutionName = instanceName.replace("instance", "sol");

                    computeLowerBound(instance, solutionName);

                    if (solversToBeCompared.contains(SolverComparison.Solver.MIP_BINPACKING)) {
                        System.out.println("solve with binpacking");
                        solveWithBinPacking(instance, solutionName);
                        instance.resetStacks();
                    }
                    if (solversToBeCompared.contains(SolverComparison.Solver.MIP_THREEINDEX)) {
                        System.out.println("solve with 3idx");
                        solveWithThreeIdx(instance, solutionName);
                        instance.resetStacks();
                    }
                }
            }
        }
    }
}
