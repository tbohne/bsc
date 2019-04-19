package SP.post_optimization_methods;

import SP.representations.Solution;
import SP.io.SolutionReader;

import java.util.ArrayList;

public class TabuSearchTest {

    public static void main(String[] args) {
        ArrayList<Solution> solutions = SolutionReader.readSolutionsFromDir(
            "res/solutions/", "res/instances/", "TwoCapHeuristic"
        );
        Solution sol = solutions.get(0);
        System.out.println("solved instance: " + sol.getNameOfSolvedInstance());

        TabuSearch ts = new TabuSearch(sol);
        System.out.println("COSTS BEFORE: " + sol.computeCosts());
        sol = ts.solve();
        System.out.println("FINAL COSTS: " + sol.computeCosts());
    }
}
