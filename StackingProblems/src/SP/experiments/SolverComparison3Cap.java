package SP.experiments;

import SP.constructive_heuristics.ThreeCapHeuristic;
import SP.io.InstanceReader;
import SP.io.SolutionWriter;
import SP.representations.Instance;
import SP.representations.Solution;
import SP.representations.Solvers;
import SP.util.HeuristicUtil;
import SP.util.RepresentationUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Provides functionalities for comparisons of different solvers for stacking problems with a stack capacity of 3.
 *
 * @author Tim Bohne
 */
public class SolverComparison3Cap extends SolverComparison {

    private final boolean postProcessing;
    private final boolean prioritizeRuntime;

    /**
     * Constructor
     *
     * @param solutionPrefix    - prefix for solutions
     * @param instancePrefix    - prefix for instances
     * @param timeLimit         - time limit for solving procedure
     * @param hideCPLEXOutput   - determines whether cplex output should be hidden
     * @param mipEmphasis       - control trade-offs between speed, feasibility and optimality
     * @param tolerance         - termination tolerance for cplex (MIPs)
     * @param postProcessing    - determines whether the post processing step is used
     * @param prioritizeRuntime - determines whether the runtime is prioritized (disables rating system)
     */
    public SolverComparison3Cap(
        String solutionPrefix, String instancePrefix, double timeLimit, boolean hideCPLEXOutput,
        int mipEmphasis, double tolerance, boolean postProcessing, boolean prioritizeRuntime
    ) {
        super(solutionPrefix, instancePrefix, timeLimit, hideCPLEXOutput, mipEmphasis, tolerance);
        this.postProcessing = postProcessing;
        this.prioritizeRuntime = prioritizeRuntime;
    }

    /**
     * Solves the given instance with the constructive heuristic for a stack capacity of 3.
     *
     * @param instance     - instance to be solved
     * @param solutionName - name of the generated solution
     */
    private void solveWithThreeCap(Instance instance, String solutionName) {

        // TODO: move hard coded values
        int thresholdLB = 20;
        int thresholdUB = 75;
        int stepSize = 5;
        float splitPairsDivisor = 3.4F;
        float penaltyFactor = 5.0F;

        ThreeCapHeuristic threeCapSolver = new ThreeCapHeuristic(
            instance, this.timeLimit, thresholdLB, thresholdUB, stepSize, splitPairsDivisor, penaltyFactor
        );
        Solution sol = threeCapSolver.solve(this.prioritizeRuntime, this.postProcessing);
        SolutionWriter.writeSolution(
            this.solutionPrefix + solutionName + ".txt", sol, RepresentationUtil.getNameOfSolver(Solvers.Solver.CONSTRUCTIVE_THREE_CAP)
        );
        SolutionWriter.writeSolutionAsCSV(
            this.solutionPrefix + "solutions.csv", sol, RepresentationUtil.getAbbreviatedNameOfSolver(Solvers.Solver.CONSTRUCTIVE_THREE_CAP)
        );
    }

    /**
     * Compares the different solvers for a stack capacity of 3.
     *
     * @param solversToBeCompared - determines the solvers that are supposed to be compared
     */
    public void compareSolvers(List<Solvers.Solver> solversToBeCompared) {

        File dir = new File(this.instancePrefix);
        File[] directoryListing = dir.listFiles();
        assert directoryListing != null;
        Arrays.sort(directoryListing);
        String allSol = HeuristicUtil.createStringContainingAllSolutionNames(this.solutionPrefix);

        for (File file : directoryListing) {
            if (file.toString().contains("slp_instance_") && !allSol.contains(file.toString().replace("res/instances/", ""))) {

                String instanceNumber = file.getName().split("_")[5].replace(".txt", "").trim();
                if (instanceNumber.equals("00")) {
                    prepareRuntimeMeasurementByPreLoadingAllClasses(file);
                }
                String instanceName = file.toString().replace("res/instances/", "").replace(".txt", "");
                Instance instance = InstanceReader.readInstance(this.instancePrefix + instanceName + ".txt");
                System.out.println("working on: " + instanceName);
                String solutionName = instanceName.replace("instance", "sol");

                computeLowerBound(instance, solutionName);

                if (solversToBeCompared.contains(Solvers.Solver.MIP_BINPACKING)) {
                    solveWithBinPacking(instance, solutionName);
                    instance.resetStacks();
                }
                if (solversToBeCompared.contains(Solvers.Solver.MIP_THREEINDEX)) {
                    solveWithThreeIdx(instance, solutionName);
                    instance.resetStacks();
                }
                if (solversToBeCompared.contains(Solvers.Solver.CONSTRUCTIVE_THREE_CAP)) {
                    solveWithThreeCap(instance, solutionName);
                }
            }
        }
    }
}
