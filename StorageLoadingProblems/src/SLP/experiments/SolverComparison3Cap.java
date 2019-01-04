package SLP.experiments;

import SLP.constructive_heuristics.ThreeCapPermutationHeuristic;
import SLP.constructive_heuristics.ThreeCapRecursiveMCMHeuristic;
import SLP.experiments.SolverComparison;
import SLP.mip_formulations.BinPackingFormulation;
import SLP.mip_formulations.ThreeIndexFormulation;
import SLP.representations.Instance;
import SLP.representations.InstanceReader;
import SLP.representations.Solution;
import SLP.representations.SolutionWriter;

import java.io.File;

public class SolverComparison3Cap extends SolverComparison {

    public static void main(String[] args) {

        File dir = new File(INSTANCE_PREFIX);
        File[] directoryListing = dir.listFiles();

        String allSol = createStringWithAllSolutionNames();

        if (directoryListing != null) {
            for (File file : directoryListing) {

                if (file.toString().contains("slp_instance_") && !allSol.contains(file.toString().replace("res/instances/", ""))) {

                    String instanceName = file.toString().replace("res/instances/", "").replace(".txt", "");

                    Instance instance = InstanceReader.readInstance(INSTANCE_PREFIX + instanceName + ".txt");
                    System.out.println(instance);

                    String solutionName = instanceName.replace("instance", "sol");

                    BinPackingFormulation binPackingFormulation = new BinPackingFormulation(instance, TIME_LIMIT);
                    Solution sol = binPackingFormulation.solve();
                    SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.MIP_BINPACKING);
                    SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.MIP_BINPACKING);

                    instance.resetStacks();

                    ThreeIndexFormulation threeIndexFormulation = new ThreeIndexFormulation(instance, TIME_LIMIT);
                    sol = threeIndexFormulation.solve();
                    SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.MIP_THREEINDEX);
                    SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.MIP_THREEINDEX);

                    instance.resetStacks();

                    ThreeCapPermutationHeuristic permMCMSolver = new ThreeCapPermutationHeuristic(instance, TIME_LIMIT);
                    sol = permMCMSolver.solve(false);
                    SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.CONSTRUCTIVE_THREE_CAP_PERMUTATION);
                    SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.CONSTRUCTIVE_THREE_CAP_PERMUTATION);

                    instance.resetStacks();

                    ThreeCapRecursiveMCMHeuristic recMCMSolver = new ThreeCapRecursiveMCMHeuristic(instance, TIME_LIMIT);
                    sol = recMCMSolver.solve(false);
                    SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.CONSTRUCTIVE_THREE_CAP_RECURSION);
                    SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.CONSTRUCTIVE_THREE_CAP_RECURSION);
                }
            }
        }
    }
}
