package SP.constructive_heuristics;

import SP.representations.Instance;
import SP.representations.InstanceReader;
import SP.representations.Solution;

public class ConstructiveHeuristicTest {

    // TODO: Should be removed later (for now used as test class)

    public static final int TIME_LIMIT = 300;

    public static void main (String[] args) {

        // TODO:
        //   - generate instance
        //   - generate heuristic solver and pass instance to constructor
        //   - solve instance using the heuristic
        //   - print results

        Instance instance = InstanceReader.readInstance("res/instances/b=3_l/slp_instance_500_200_3_04.txt");
//        // System.out.println(instance);
//
        ThreeCapHeuristic solver = new ThreeCapHeuristic(instance, TIME_LIMIT);
        Solution sol = solver.solve();
//
        System.out.println("feasible: " + sol.isFeasible());
        System.out.println("cost: " + sol.getCost());

//        System.out.println("COST: " + Integer.MAX_VALUE / 100);
    }
}
