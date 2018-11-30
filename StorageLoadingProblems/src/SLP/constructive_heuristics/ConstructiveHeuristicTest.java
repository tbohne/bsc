package SLP.constructive_heuristics;

import SLP.Instance;
import SLP.InstanceReader;

public class ConstructiveHeuristicTest {

    public static void main (String[] args) {

        // TODO:
        //   - generate instance
        //   - generate heuristic solver and pass instance to constructor
        //   - solve instance using the heuristic
        //   - print results


        Instance instance = InstanceReader.readInstance("res/instances/slp_instance_20_7_3_4.txt");
        // System.out.println(instance);

        InitialHeuristicSLPSolver solver = new InitialHeuristicSLPSolver(instance);
        System.out.println(solver.solve());

    }

}
