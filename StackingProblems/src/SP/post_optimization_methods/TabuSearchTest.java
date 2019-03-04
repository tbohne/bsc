package SP.post_optimization_methods;

import SP.representations.Instance;
import SP.io.InstanceReader;
import SP.representations.Solution;
import SP.io.SolutionReader;

import java.util.ArrayList;

public class TabuSearchTest {

    public static void main(String[] args) {


        Instance instance = InstanceReader.readInstance("res/instances/b=2_l/slp_instance_500_300_2_00.txt");

        ArrayList<Solution> solutions = SolutionReader.readSolutionsFromDir("res/solutions/b=2_l/900s", "res/instances/b=2_l/");

        System.out.println("expected instance name: " + instance.getName());
        System.out.println("actual instance name  : " + solutions.get(0).getNameOfSolvedInstance());

        TabuSearch ts = new TabuSearch(instance, solutions.get(0));
        Solution sol = ts.solve(instance);
        System.out.println("FINAL RES: " + sol.getObjectiveValue());

    }

}
