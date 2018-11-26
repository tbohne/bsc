public class MIPFormulationComparator {

    public enum Formulation {
        BINPACKING,
        THREEINDEX
    }

    public static final String INSTANCE_PREFIX = "res/instances/";
    public static final String SOLUTION_PREFIX = "res/solutions/";

    public static void main(String[] args) {

        String instanceName = "slp_instance_10_5_3_1";

        Instance instance = InstanceReader.readInstance(INSTANCE_PREFIX + instanceName + ".txt");
        System.out.println(instance);

        BinPackingFormulation binPackingFormulation = new BinPackingFormulation(instance);
        String solutionName = instanceName.replace("instance", "sol");
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", binPackingFormulation.solve(), Formulation.BINPACKING);

        instance.resetStacks();

        ThreeIndexFormulation threeIndexFormulation = new ThreeIndexFormulation(instance);
        SolutionWriter.writeSolution(SOLUTION_PREFIX + solutionName + ".txt", threeIndexFormulation.solve(), Formulation.THREEINDEX);
    }
}
