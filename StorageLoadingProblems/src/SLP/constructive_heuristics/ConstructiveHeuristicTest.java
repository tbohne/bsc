package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.InstanceReader;
import SLP.Solution;

public class ConstructiveHeuristicTest {

    public static void main (String[] args) {

        // TODO:
        //   - generate instance
        //   - generate heuristic solver and pass instance to constructor
        //   - solve instance using the heuristic
        //   - print results


        Instance instance = InstanceReader.readInstance("res/instances/slp_instance_20_8_3_0.txt");
        // System.out.println(instance);

        InitialHeuristicSLPSolver solver = new InitialHeuristicSLPSolver(instance);

        Solution sol = solver.solve();
        System.out.println(sol);
        System.out.println("feasible: " + sol.isFeasible());
    }

}
