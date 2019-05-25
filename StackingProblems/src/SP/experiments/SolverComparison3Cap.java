package SP.experiments;

import SP.constructive_heuristics.ThreeCapHeuristic;
import SP.io.InstanceReader;
import SP.io.SolutionWriter;
import SP.representations.Instance;
import SP.representations.Solution;
import SP.util.HeuristicUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class SolverComparison3Cap extends SolverComparison {

    /**
     * Solves the given instance with the constructive heuristic for a stack capacity of 3.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     * @param writeSol     - determines whether or not the solution should be written to the file system
     */
    public static void solveWithThreeCap(Instance instance, String solutionName, boolean writeSol) {

        // TODO: move hard coded values
        int thresholdLB = 20;
        int thresholdUB = 75;
        int stepSize = 5;
        float splitPairsDivisor = 3.4F;
        float penaltyFactor = 5.0F;

        ThreeCapHeuristic threeCapSolver = new ThreeCapHeuristic(instance, TIME_LIMIT, thresholdLB, thresholdUB, stepSize, splitPairsDivisor, penaltyFactor);
        Solution sol = threeCapSolver.solve(PRIORITIZE_RUNTIME, POST_PROCESSING);
        if (writeSol) {
            SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, getNameOfSolver(Solver.CONSTRUCTIVE_THREE_CAP));
            SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, getAbbreviatedNameOfSolver(Solver.CONSTRUCTIVE_THREE_CAP));
        }
    }

    /**
     * Compares the different solvers for a stack capacity of 3.
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

                    String instanceNumber = file.getName().split("_")[5].replace(".txt", "").trim();
                    if (instanceNumber.equals("00")) {
                        prepareRuntimeMeasurementByPreLoadingAllClasses(file);
                    }
                    String instanceName = file.toString().replace("res/instances/", "").replace(".txt", "");
                    Instance instance = InstanceReader.readInstance(INSTANCE_PREFIX + instanceName + ".txt");
                    System.out.println("working on: " + instanceName);
                    String solutionName = instanceName.replace("instance", "sol");

                    computeLowerBound(instance, solutionName);

                    if (solversToBeCompared.contains(SolverComparison.Solver.MIP_BINPACKING)) {
                        solveWithBinPacking(instance, solutionName);
                        instance.resetStacks();
                    }
                    if (solversToBeCompared.contains(SolverComparison.Solver.MIP_THREEINDEX)) {
                        solveWithThreeIdx(instance, solutionName);
                        instance.resetStacks();
                    }
                    if (solversToBeCompared.contains(SolverComparison.Solver.CONSTRUCTIVE_THREE_CAP)) {
                        solveWithThreeCap(instance, solutionName, true);
                    }
                }
            }
        }
    }

}
