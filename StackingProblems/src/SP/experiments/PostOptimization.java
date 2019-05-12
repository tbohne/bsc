package SP.experiments;

import SP.io.SolutionReader;
import SP.io.SolutionWriter;
import SP.post_optimization_methods.TabuSearch;
import SP.representations.Solution;

import java.util.ArrayList;

public class PostOptimization {

    public static final String INSTANCE_PREFIX = "res/instances/";
    public static final String SOLUTION_PREFIX = "res/solutions/";

    public static final String SOLVER = "ThreeCapHeuristic";
//    public static final String SOLVER = "TwoCapHeuristic";

    public static void optimizeSolutions() {

        ArrayList<Solution> solutions = SolutionReader.readSolutionsFromDir(SOLUTION_PREFIX, INSTANCE_PREFIX, SOLVER);

        for (Solution sol : solutions) {

            double startTime = System.currentTimeMillis();

            System.out.println("solved instance: " + sol.getNameOfSolvedInstance());
            TabuSearch ts = new TabuSearch(sol);
            System.out.println("COSTS BEFORE: " + sol.computeCosts());
            sol = ts.solve();

            double time = (System.currentTimeMillis() - startTime) / 1000.0;

            sol.setTimeToSolve(time);

            System.out.println("COSTS AFTER: " + sol.computeCosts());
            System.out.println("TIME: " + time);

            SolutionWriter.writeSolution(SOLUTION_PREFIX + sol.getNameOfSolvedInstance().replace("instances/", "") + "_imp.txt", sol, SolverComparison.Solver.TABU_SEARCH);
            SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions_imp.csv", sol, SolverComparison.Solver.TABU_SEARCH);
        }
    }

    public static void main(String[] args) {
        optimizeSolutions();
    }
}
