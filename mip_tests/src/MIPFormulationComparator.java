public class MIPFormulationComparator {

    public static void main(String[] args) {

        Instance instance = Reader.readInstance("res/slp_instance_generated_1.txt");

        System.out.println("********************************************************************");
        System.out.println("items: " + instance.getItems());
        System.out.println("stacks: " + instance.getStacks());
        System.out.println("stack capacity: " + instance.getStackCapacity());
        System.out.println();

        System.out.println("stacking constraints:");
        for (int i = 0; i < instance.getItems().length; i++) {
            for (int j = 0; j < instance.getItems().length; j++) {
                System.out.print(instance.getStackingConstraints()[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("stacking costs:");
        for (int i = 0; i < instance.getItems().length; i++) {
            for (int j = 0; j < instance.getStacks().length; j++) {
                System.out.print(instance.getCosts()[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("********************************************************************");

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
