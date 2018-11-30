package SLP.mip_formulations;

import SLP.Instance;
import SLP.InstanceReader;
import SLP.Solution;
import SLP.SolutionWriter;

import java.io.File;

public class MIPFormulationComparator {

    public enum Formulation {
        BINPACKING,
        THREEINDEX
    }

    public static final String INSTANCE_PREFIX = "res/instances/";
    public static final String SOLUTION_PREFIX = "res/solutions/";

    // Specifies the time limit for the solving process in seconds.
    public static final int TIME_LIMIT = 300;

    public static String createStringWithAllSolutionNames() {
        File dir = new File(SOLUTION_PREFIX);
        File[] dirListing = dir.listFiles();

        String str = "";

        for (File f : dirListing) {
            str += f.toString() + " ";
        }
        return str;
    }

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
                    SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Formulation.BINPACKING);
                    SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Formulation.BINPACKING);

                    instance.resetStacks();

                    ThreeIndexFormulation threeIndexFormulation = new ThreeIndexFormulation(instance, TIME_LIMIT);
                    sol = threeIndexFormulation.solve();
                    SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", sol, Formulation.THREEINDEX);
                    SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions.csv", sol, Formulation.THREEINDEX);
                }
            }
        }
    }
}
