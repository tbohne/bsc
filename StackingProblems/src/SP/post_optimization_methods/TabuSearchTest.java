package SP.post_optimization_methods;

import SP.representations.Instance;
import SP.io.InstanceReader;
import SP.representations.Solution;
import SP.io.SolutionReader;

import java.util.ArrayList;

public class TabuSearchTest {

    public final boolean ITERATIONS_CRITERION = true;
    public final int NUMBER_OF_ITERATIONS = 1000;

    public static final String SOLVER = "TwoCapHeuristic";

    public static void main(String[] args) {

        ArrayList<Solution> solutions = SolutionReader.readSolutionsFromDir(
            "res/solutions/", "res/instances/", SOLVER
        );
        Solution sol = solutions.get(0);
        System.out.println("solved instance: " + sol.getNameOfSolvedInstance());

        TabuSearch ts = new TabuSearch(sol);
        System.out.println("COSTS BEFORE: " + sol.computeCosts());
        sol = ts.solve();
        System.out.println("FINAL COSTS: " + sol.computeCosts());
    }
}
