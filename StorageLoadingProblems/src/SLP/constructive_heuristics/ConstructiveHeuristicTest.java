package SLP.constructive_heuristics;

import SLP.representations.Instance;
import SLP.representations.InstanceReader;
import SLP.representations.Solution;

public class ConstructiveHeuristicTest {

    // TODO: Should be removed later (for now used as test class)

    public static final int TIME_LIMIT = 300;

    public static void main (String[] args) {

        // TODO:
        //   - generate instance
        //   - generate heuristic solver and pass instance to constructor
        //   - solve instance using the heuristic
        //   - print results

        Instance instance = InstanceReader.readInstance("res/instances/slp_instance_400_240_2_2.txt");
        // System.out.println(instance);

        TwoCapHeuristic solver = new TwoCapHeuristic(instance, TIME_LIMIT);
        Solution sol = solver.solve();

        System.out.println("feasible: " + sol.isFeasible());
        System.out.println("cost: " + sol.getCost());
    }
}
