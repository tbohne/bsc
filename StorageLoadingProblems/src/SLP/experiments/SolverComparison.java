package SLP.experiments;

import java.io.File;

public class SolverComparison {

    public enum Solver {
        MIP_BINPACKING,
        MIP_THREEINDEX,
        CONSTRUCTIVE_TWO_CAP,
        CONSTRUCTIVE_THREE_CAP_PERMUTATION,
        CONSTRUCTIVE_THREE_CAP_RECURSION
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
}
