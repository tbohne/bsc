package SP.post_optimization_methods;

import SP.representations.Solution;
import SP.io.SolutionReader;

import java.util.ArrayList;

public class TabuSearchTest {

    /******** CONFIGURABLE TS OPTIONS ************************/
    // TL clears otherwise
    public static final boolean ITERATIONS_CRITERION = true;
    // best fit otherwise
    public static final boolean FIRST_FIT = true;
    public static final boolean ONLY_FEASIBLE = true;
    /*********************************************************/
    public static final String SOLVER = "TwoCapHeuristic";

    public static void main(String[] args) {
        ArrayList<Solution> solutions = SolutionReader.readSolutionsFromDir(
            "res/solutions/", "res/instances/", SOLVER
        );
        Solution sol = solutions.get(0);
        System.out.println("solved instance: " + sol.getNameOfSolvedInstance());

        TabuSearch ts = new TabuSearch(sol);
        System.out.println("COSTS BEFORE: " + sol.computeCosts());
        sol = ts.solve(ITERATIONS_CRITERION, FIRST_FIT, ONLY_FEASIBLE);
        System.out.println("FINAL COSTS: " + sol.computeCosts());
    }
}
