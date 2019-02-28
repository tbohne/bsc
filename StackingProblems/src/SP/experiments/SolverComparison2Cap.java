package SP.experiments;

import SP.mip_formulations.BinPackingFormulation;
import SP.representations.Instance;
import SP.representations.InstanceReader;
import SP.representations.Solution;
import SP.representations.SolutionWriter;

import java.io.File;
import java.util.Arrays;

public class SolverComparison2Cap extends SolverComparison {

    public static void main(String[] args) {

        File dir = new File(INSTANCE_PREFIX);
        File[] directoryListing = dir.listFiles();
        Arrays.sort(directoryListing);

        String allSol = createStringWithAllSolutionNames();

        if (directoryListing != null) {
            for (File file : directoryListing) {

                if (file.toString().contains("slp_instance_") && !allSol.contains(file.toString().replace("res/instances/", ""))) {

                    String instanceName = file.toString().replace("res/instances/", "").replace(".txt", "");
                    System.out.println("working on: " + instanceName);
                    Instance instance = InstanceReader.readInstance(INSTANCE_PREFIX + instanceName + ".txt");

                    String solutionName = instanceName.replace("instance", "sol");

                    BinPackingFormulation binPackingFormulation = new BinPackingFormulation(instance, TIME_LIMIT);
                    Solution sol = binPackingFormulation.solve();
                    SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.MIP_BINPACKING);
                    SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.MIP_BINPACKING);

//                    instance.resetStacks();
//
//                    ThreeIndexFormulation threeIndexFormulation = new ThreeIndexFormulation(instance, TIME_LIMIT);
//                    sol = threeIndexFormulation.solve();
//                    SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.MIP_THREEINDEX);
//                    SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.MIP_THREEINDEX);
//
//                    instance.resetStacks();
//
//                    TwoCapHeuristic twoCapHeuristic = new TwoCapHeuristic(instance, TIME_LIMIT);
//                    sol = twoCapHeuristic.solve();
//                    SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Solver.CONSTRUCTIVE_TWO_CAP);
//                    SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Solver.CONSTRUCTIVE_TWO_CAP);
                }
            }
        }
    }
}
