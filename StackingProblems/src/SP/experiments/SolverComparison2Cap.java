package SP.experiments;

import SP.constructive_heuristics.TwoCapHeuristic;
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
 * Compares the solvers for a stack capacity of 2.
 *
 * @author Tim Bohne
 */
public class SolverComparison2Cap implements SolverComparison {

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
    public void solveWithThreeIdx(Instance instance, String solutionName) {
        ThreeIndexFormulation threeIndexFormulation = new ThreeIndexFormulation(instance, TIME_LIMIT);
        Solution sol = threeIndexFormulation.solve();
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.MIP_THREEINDEX);
        SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.MIP_THREEINDEX);
    }

    /**
     * Solves the given instance with the constructive heuristic for a stack capacity of 2.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public void solveWithTwoCap(Instance instance, String solutionName) {
        TwoCapHeuristic twoCapHeuristic = new TwoCapHeuristic(instance, TIME_LIMIT);
        Solution sol = twoCapHeuristic.solve(POST_PROCESSING);
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.CONSTRUCTIVE_TWO_CAP);
        SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.CONSTRUCTIVE_TWO_CAP);
    }

    /**
     * Compares the different solvers for a stack capacity of 2.
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
                    System.out.println("working on: " + instanceName);
                    Instance instance = InstanceReader.readInstance(INSTANCE_PREFIX + instanceName + ".txt");
                    String solutionName = instanceName.replace("instance", "sol");

                    if (solversToBeCompared.contains(Solver.MIP_BINPACKING)) {
                        System.out.println("solve with binpacking");
                        this.solveWithBinPacking(instance, solutionName);
                        instance.resetStacks();
                    }
                    if (solversToBeCompared.contains(Solver.MIP_THREEINDEX)) {
                        System.out.println("solve with 3idx");
                        this.solveWithThreeIdx(instance, solutionName);
                        instance.resetStacks();
                    }
                    if (solversToBeCompared.contains(Solver.CONSTRUCTIVE_TWO_CAP)) {
                        System.out.println("solve with 2cap");
                        this.solveWithTwoCap(instance, solutionName);
                    }
                }
            }
        }
    }
}
