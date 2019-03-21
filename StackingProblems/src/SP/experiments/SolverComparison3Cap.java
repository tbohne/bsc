package SP.experiments;

import SP.constructive_heuristics.ThreeCapHeuristic;
import SP.constructive_heuristics.deprecated.ThreeCapPermutationHeuristic;
import SP.constructive_heuristics.deprecated.ThreeCapRecursiveMCMHeuristic;
import SP.mip_formulations.BinPackingFormulation;
import SP.mip_formulations.ThreeIndexFormulation;
import SP.representations.Instance;
import SP.io.InstanceReader;
import SP.representations.Solution;
import SP.io.SolutionWriter;
import SP.util.HeuristicUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Compares the solvers for a stack capacity of 3.
 *
 * @author Tim Bohne
 */
public class SolverComparison3Cap implements SolverComparison {

    /**
     * Solves the given instance with the bin-packing solver.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public void solveWithBinPacking(Instance instance, String solutionName) {
        BinPackingFormulation binPackingFormulation = new BinPackingFormulation(instance, TIME_LIMIT);
        Solution sol = binPackingFormulation.solve();
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.MIP_BINPACKING);
        SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.MIP_BINPACKING);
    }

    /**
     * Solves the given instance with the three-index solver.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public void solveWithThreeIndex(Instance instance, String solutionName) {
        ThreeIndexFormulation threeIndexFormulation = new ThreeIndexFormulation(instance, TIME_LIMIT);
        Solution sol = threeIndexFormulation.solve();
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.MIP_THREEINDEX);
        SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.MIP_THREEINDEX);
    }

    /**
     * Solves the given instance with the constructive heuristic based on permutations for a stack capacity of 3.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public void solveWithThreeCapPerm(Instance instance, String solutionName) {
        ThreeCapPermutationHeuristic permMCMSolver = new ThreeCapPermutationHeuristic(instance, TIME_LIMIT);
        Solution sol = permMCMSolver.solve(false);
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.CONSTRUCTIVE_THREE_CAP_PERMUTATION);
        SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.CONSTRUCTIVE_THREE_CAP_PERMUTATION);
    }

    /**
     * Solves the given instance with the constructive heuristic based on recursive MCMs for a stack capacity of 3.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public void solveWithThreeCapRec(Instance instance, String solutionName) {
        ThreeCapRecursiveMCMHeuristic recMCMSolver = new ThreeCapRecursiveMCMHeuristic(instance, TIME_LIMIT);
        Solution sol = recMCMSolver.solve();
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.CONSTRUCTIVE_THREE_CAP_RECURSION);
        SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.CONSTRUCTIVE_THREE_CAP_RECURSION);
    }

    /**
     * Solves the given instance with the constructive heuristic for a stack capacity of 3.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public void solveWithThreeCap(Instance instance, String solutionName) {
        ThreeCapHeuristic threeCapSolver = new ThreeCapHeuristic(instance, TIME_LIMIT);
        Solution sol = threeCapSolver.solve(false);
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.CONSTRUCTIVE_THREE_CAP);
        SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.CONSTRUCTIVE_THREE_CAP);
    }

    /**
     * Compares the different solvers for a stack capacity of 3.
     *
     * @param solversToBeCompared - determines the solvers that are supposed to be compared
     */
    public void compareSolvers(ArrayList<Solver> solversToBeCompared) {
        File dir = new File(INSTANCE_PREFIX);
        File[] directoryListing = dir.listFiles();
        Arrays.sort(directoryListing);
        String allSol = HeuristicUtil.createStringContainingAllSolutionNames(SOLUTION_PREFIX);

        if (directoryListing != null) {
            for (File file : directoryListing) {
                if (file.toString().contains("slp_instance_") && !allSol.contains(file.toString().replace("res/instances/", ""))) {

                    String instanceName = file.toString().replace("res/instances/", "").replace(".txt", "");
                    Instance instance = InstanceReader.readInstance(INSTANCE_PREFIX + instanceName + ".txt");
                    System.out.println("working on: " + instanceName);
                    String solutionName = instanceName.replace("instance", "sol");

                    if (solversToBeCompared.contains(Solver.MIP_BINPACKING)) {
                        this.solveWithBinPacking(instance, solutionName);
                        instance.resetStacks();
                    }
                    if (solversToBeCompared.contains(Solver.MIP_THREEINDEX)) {
                        this.solveWithThreeIndex(instance, solutionName);
                        instance.resetStacks();
                    }
                    if (solversToBeCompared.contains(Solver.CONSTRUCTIVE_THREE_CAP_PERMUTATION)) {
                        this.solveWithThreeCapPerm(instance, solutionName);
                        instance.resetStacks();
                    }
                    if (solversToBeCompared.contains(Solver.CONSTRUCTIVE_THREE_CAP_RECURSION)) {
                        this.solveWithThreeCapRec(instance, solutionName);
                        instance.resetStacks();
                    }
                    if (solversToBeCompared.contains(Solver.CONSTRUCTIVE_THREE_CAP)) {
                        this.solveWithThreeCap(instance, solutionName);
                    }
                }
            }
        }
    }
}
