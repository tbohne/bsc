package SP.post_optimization_methods;

import SP.experiments.OptimizableSolution;
import SP.representations.Solution;
import SP.io.SolutionReader;

import java.util.ArrayList;
import java.util.List;

public class TabuSearchTest {

    public static void main(String[] args) {
        List<OptimizableSolution> solutions = SolutionReader.readSolutionsFromDir(
            "res/solutions/", "res/instances/", "ThreeCapHeuristic"
        );
        Solution sol = solutions.get(0).getSol();
        System.out.println("solved instance: " + sol.getNameOfSolvedInstance());

        TabuSearch ts = new TabuSearch(sol, solutions.get(0).getOptimalObjectiveValue());
        System.out.println("COSTS BEFORE: " + sol.computeCosts());
        sol = ts.solve();
        System.out.println("FINAL COSTS: " + sol.computeCosts() + " --> " + sol.isFeasible() + " --> " + sol.getNumberOfAssignedItems());
    }
}
