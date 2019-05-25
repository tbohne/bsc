package SP.experiments;

import SP.io.InstanceReader;
import SP.io.SolutionWriter;
import SP.mip_formulations.BinPackingFormulation;
import SP.mip_formulations.ThreeIndexFormulation;
import SP.representations.Instance;
import SP.representations.Solution;
import SP.representations.Solvers;
import SP.util.HeuristicUtil;
import SP.util.RepresentationUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class SolverComparison {

    protected String solutionPrefix;
    protected String instancePrefix;
    protected double timeLimit;

    protected boolean hideCPLEXOutput;
    protected int mipEmphasis;
    protected double tolerance;


    public SolverComparison(
        String solutionPrefix, String instancePrefix, double timeLimit, boolean hideCPLEXOutput, int mipEmphasis, double tolerance
    ) {
        this.solutionPrefix = solutionPrefix;
        this.instancePrefix = instancePrefix;
        this.timeLimit = timeLimit;
        this.hideCPLEXOutput = hideCPLEXOutput;
        this.mipEmphasis = mipEmphasis;
        this.tolerance = tolerance;
    }

    /**
     * Computes a lower bound for the specified instance using the LowerBoundsCalculator.
     *
     * @param instance     - the instance to compute a LB for
     * @param solutionName - the name of the generated solution
     */
    public void computeLowerBound(Instance instance, String solutionName) {
        LowerBoundsCalculator lbc = new LowerBoundsCalculator(instance);
        double lowerBound = lbc.computeLowerBound();
        SolutionWriter.writeLowerBound(this.solutionPrefix + solutionName + ".txt", lowerBound);
        SolutionWriter.writeLowerBoundAsCSV(this.solutionPrefix + "solutions.csv", lowerBound, instance.getName());
    }

    /**
     * Solves the given instance with the bin-packing solver.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public void solveWithBinPacking(Instance instance, String solutionName) {
        BinPackingFormulation binPackingFormulation = new BinPackingFormulation(
                instance, this.timeLimit, this.hideCPLEXOutput, this.mipEmphasis, this.tolerance
        );
        Solution sol = binPackingFormulation.solve();
        SolutionWriter.writeSolution(this.solutionPrefix + solutionName + ".txt", sol, RepresentationUtil.getNameOfSolver(Solvers.Solver.MIP_BINPACKING));
        SolutionWriter.writeSolutionAsCSV(this.solutionPrefix + "solutions.csv", sol, RepresentationUtil.getAbbreviatedNameOfSolver(Solvers.Solver.MIP_BINPACKING));
    }

    /**
     * Solves the given instance with the three-index solver.
     *
     * @param instance     - the instance to be solved
     * @param solutionName - the name of the generated solution
     */
    public void solveWithThreeIdx(Instance instance, String solutionName) {
        ThreeIndexFormulation threeIndexFormulation = new ThreeIndexFormulation(
                instance, this.timeLimit, this.hideCPLEXOutput, this.mipEmphasis, this.tolerance
        );
        Solution sol = threeIndexFormulation.solve();
        SolutionWriter.writeSolution(this.solutionPrefix + solutionName + ".txt", sol, RepresentationUtil.getNameOfSolver(Solvers.Solver.MIP_THREEINDEX));
        SolutionWriter.writeSolutionAsCSV(this.solutionPrefix + "solutions.csv", sol, RepresentationUtil.getAbbreviatedNameOfSolver(Solvers.Solver.MIP_THREEINDEX));
    }

    /**
     * Workaround that triggers the ClassLoader to load all classes that are needed during the solving procedure.
     * Otherwise the first instance to be solved would have a longer runtime because of the initial class loading.
     *
     * @param instanceFile - the file to load the instance from
     */
    public void prepareRuntimeMeasurementByPreLoadingAllClasses(File instanceFile) {
        String instanceName = instanceFile.toString().replace("res/instances/", "").replace(".txt", "");
        Instance instance = InstanceReader.readInstance(this.instancePrefix + instanceName + ".txt");
        String solutionName = instanceName.replace("instance", "sol");
        computeLowerBound(instance, solutionName);
    }

    /**
     * Compares the different solvers for a stack capacity of 2.
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
                    String instanceName = file.toString().replace("res/instances/", "").replace(".txt", "");
                    System.out.println("working on: " + instanceName);
                    Instance instance = InstanceReader.readInstance(this.instancePrefix + instanceName + ".txt");
                    String solutionName = instanceName.replace("instance", "sol");

                    computeLowerBound(instance, solutionName);

                    if (solversToBeCompared.contains(Solvers.Solver.MIP_BINPACKING)) {
                        System.out.println("solve with binpacking");
                        solveWithBinPacking(instance, solutionName);
                        instance.resetStacks();
                    }
                    if (solversToBeCompared.contains(Solvers.Solver.MIP_THREEINDEX)) {
                        System.out.println("solve with 3idx");
                        solveWithThreeIdx(instance, solutionName);
                        instance.resetStacks();
                    }
                }
            }
        }
    }
}
