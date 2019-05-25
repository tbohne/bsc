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

public class SolverComparison3Cap extends SolverComparison {

    private boolean postProcessing;
    private boolean prioritizeRuntime;

    public SolverComparison3Cap(
        String solutionPrefix, String instancePrefix, double timeLimit, boolean hideCPLEXOutput, int mipEmphasis, double tolerance,
        boolean postProcessing, boolean prioritizeRuntime
    ) {
        super(solutionPrefix, instancePrefix, timeLimit, hideCPLEXOutput, mipEmphasis, tolerance);
        this.postProcessing = postProcessing;
        this.prioritizeRuntime = prioritizeRuntime;
    }

    /**
     * Solves the given instance with the constructive heuristic for a stack capacity of 3.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     * @param writeSol     - determines whether or not the solution should be written to the file system
     */
    public void solveWithThreeCap(Instance instance, String solutionName, boolean writeSol) {

        // TODO: move hard coded values
        int thresholdLB = 20;
        int thresholdUB = 75;
        int stepSize = 5;
        float splitPairsDivisor = 3.4F;
        float penaltyFactor = 5.0F;

        ThreeCapHeuristic threeCapSolver = new ThreeCapHeuristic(instance, this.timeLimit, thresholdLB, thresholdUB, stepSize, splitPairsDivisor, penaltyFactor);
        Solution sol = threeCapSolver.solve(this.prioritizeRuntime, this.postProcessing);
        if (writeSol) {
            SolutionWriter.writeSolution(this.solutionPrefix + solutionName + ".txt", sol, RepresentationUtil.getNameOfSolver(Solvers.Solver.CONSTRUCTIVE_THREE_CAP));
            SolutionWriter.writeSolutionAsCSV(this.solutionPrefix + "solutions.csv", sol, RepresentationUtil.getAbbreviatedNameOfSolver(Solvers.Solver.CONSTRUCTIVE_THREE_CAP));
        }
    }

    /**
     * Compares the different solvers for a stack capacity of 3.
     *
     * @param solversToBeCompared - determines the solvers that are supposed to be compared
     */
    public void compareSolvers(List<Solvers.Solver> solversToBeCompared) {
        File dir = new File(this.instancePrefix);
        File[] directoryListing = dir.listFiles();
        Arrays.sort(directoryListing);
        String allSol = HeuristicUtil.createStringContainingAllSolutionNames(this.solutionPrefix);

        if (directoryListing != null) {
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
                        solveWithThreeCap(instance, solutionName, true);
                    }
                }
            }
        }
    }
}
