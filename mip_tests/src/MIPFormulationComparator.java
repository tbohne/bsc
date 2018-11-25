public class MIPFormulationComparator {

    public static void main(String[] args) {

        Instance instance = InstanceReader.readInstance("res/instances/slp_instance_generated_1.txt");
        System.out.println(instance);

        System.out.println("--- BIN-PACKING-FORMULATION ---");

        BinPackingFormulation binPackingFormulation = new BinPackingFormulation(instance);
        System.out.println(binPackingFormulation.solve());

        instance.resetStacks();

        System.out.println();
        System.out.println("--- THREE-INDEX-FORMULATION ---");

        ThreeIndexFormulation threeIndexFormulation = new ThreeIndexFormulation(instance);
        System.out.println(threeIndexFormulation.solve());
    }
}
