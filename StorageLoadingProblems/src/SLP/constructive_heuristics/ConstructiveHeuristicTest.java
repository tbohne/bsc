package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.InstanceReader;
import SLP.Solution;

public class ConstructiveHeuristicTest {

    // TODO: Should be removed later (for now used as test class)

    public static void main (String[] args) {

        // TODO:
        //   - generate instance
        //   - generate heuristic solver and pass instance to constructor
        //   - solve instance using the heuristic
        //   - print results



        Instance instance = InstanceReader.readInstance("res/instances/v6/slp_instance_300_120_3_89.txt");
        // System.out.println(instance);

        InitialHeuristicSLPSolver solver = new InitialHeuristicSLPSolver(instance);

        Solution sol = solver.solve(false);
        System.out.println(sol);
        System.out.println("feasible: " + sol.isFeasible());
        System.out.println("cost: " + sol.getCost());
    }
}
