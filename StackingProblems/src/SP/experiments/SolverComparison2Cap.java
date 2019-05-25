package SP.experiments;

import SP.constructive_heuristics.TwoCapHeuristic;
import SP.io.InstanceReader;
import SP.io.SolutionWriter;
import SP.representations.Instance;
import SP.representations.Solution;
import SP.util.HeuristicUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class SolverComparison2Cap extends SolverComparison {

    /**
     * Solves the given instance with the constructive heuristic for a stack capacity of 2.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public static void solveWithTwoCap(Instance instance, String solutionName) {
        TwoCapHeuristic twoCapHeuristic = new TwoCapHeuristic(instance, TIME_LIMIT);
        Solution sol = twoCapHeuristic.solve(POST_PROCESSING);
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, getNameOfSolver(Solver.CONSTRUCTIVE_TWO_CAP));
        SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, getAbbreviatedNameOfSolver(Solver.CONSTRUCTIVE_TWO_CAP));
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
                    if (solversToBeCompared.contains(SolverComparison.Solver.CONSTRUCTIVE_TWO_CAP)) {
                        System.out.println("solve with 2cap");
                        solveWithTwoCap(instance, solutionName);
                    }
                }
            }
        }
    }
}
