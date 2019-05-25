package SP.experiments;

import SP.io.SolutionReader;
import SP.io.SolutionWriter;
import SP.post_optimization_methods.TabuSearch;
import SP.representations.Solution;
import SP.representations.Solvers;
import SP.util.RepresentationUtil;

import java.util.ArrayList;
import java.util.List;

public class PostOptimization {

    public static final String INSTANCE_PREFIX = "res/instances/";
    public static final String SOLUTION_PREFIX = "res/solutions/";

    public static final String SOLVER = "ThreeCapHeuristic";
//    public static final String SOLVER = "TwoCapHeuristic";

    public static void optimizeSolutions() {

        List<OptimizableSolution> solutions = SolutionReader.readSolutionsFromDir(SOLUTION_PREFIX, INSTANCE_PREFIX, SOLVER);

        for (OptimizableSolution sol : solutions) {

            double startTime = System.currentTimeMillis();
            double costsBefore = sol.getSol().computeCosts();

            TabuSearch ts = new TabuSearch(sol.getSol(), sol.getOptimalObjectiveValue());
            Solution impSol = ts.solve();
            impSol.setTimeToSolve((System.currentTimeMillis() - startTime) / 1000.0);

            System.out.println("costs before: " + costsBefore);
            System.out.println("costs after: " + impSol.computeCosts());
            System.out.println("total difference: " + (costsBefore - impSol.computeCosts()));
            System.out.println("runtime: " + impSol.getTimeToSolve());

            SolutionWriter.writeSolution(
                SOLUTION_PREFIX
                + impSol.getNameOfSolvedInstance().replace("instances/", "")
                + "_imp.txt", impSol, RepresentationUtil.getNameOfSolver(Solvers.Solver.TABU_SEARCH)
            );

            Solvers.Solver solver;
            if (sol.getSol().getFilledStacks()[0].length == 2) {
                solver = Solvers.Solver.CONSTRUCTIVE_TWO_CAP;
            } else {
                solver = Solvers.Solver.CONSTRUCTIVE_THREE_CAP;
            }
            SolutionWriter.writeSolutionAsCSV(SOLUTION_PREFIX + "solutions_imp.csv", sol.getSol(), RepresentationUtil.getNameOfSolver(solver));
            SolutionWriter.writeOptAndImpAsCSV(SOLUTION_PREFIX + "solutions_imp.csv", sol, impSol);
        }
    }

    public static void main(String[] args) {
        optimizeSolutions();
    }
}
