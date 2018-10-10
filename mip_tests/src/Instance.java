import java.util.ArrayList;

public class Instance {

//    private ArrayList<Integer> items;
//    private ArrayList<ArrayList<Integer>> stacks;

    private int[] items;
    private int[][] stacks;

    private int[][] stackingConstraints;
    private int[][] costs;
    private int stackCapacity;

    public Instance(
            int numberOfItems,
            int numberOfStacks,
            int stackCapacity,
            int[][] stackingConstraints,
            int[][] costs
    ) {

        this.items = new int[numberOfItems];
        this.stacks = new int[numberOfStacks][stackCapacity];

        for (int i = 0; i < numberOfStacks; i++) {
            for (int j = 0; j < stackCapacity; j++) {
                this.stacks[i][j] = -1;
            }
        }

        for (int i = 0; i < numberOfItems; i++) {
            // this.items.add(i);
            this.items[i] = i;
        }

//        for (int i = 0; i < numberOfStacks; i++) {
//            this.stacks.add(new ArrayList<>());
//        }

        this.stackCapacity = stackCapacity;
        this.stackingConstraints = stackingConstraints;
        this.costs = costs;
    }

//    /**
//     * Copy constructor
//     * @param instance - the instance to be copied
//     */
//    public Instance(Instance instance) {
//
//        this.items = new ArrayList<>(instance.getItems());
//        this.stacks = new ArrayList<>(instance.getStacks());
//        this.stackCapacity = instance.getStackCapacity();
//
//        this.stackingConstraints = new int[instance.getStackingConstraints().length][];
//        for (int i = 0; i < instance.getStackingConstraints().length; i++) {
//            for (int j = 0; j < instance.getStackingConstraints()[0].length; j++) {
//                this.stackingConstraints[i][j] = instance.getStackingConstraints()[i][j];
//            }
//        }
//
//        this.costs = new int[instance.getCosts().length][];
//        for (int i = 0; i < instance.getCosts().length; i++) {
//            for (int j = 0; j < instance.getCosts()[0].length; j++) {
//                this.costs[i][j] = instance.getCosts()[i][j];
//            }
//        }
//    }

    public void resetStacks() {
        int numberOfStacks = this.stacks.length;
        int stackCapacity = this.stacks[0].length;
        this.stacks = new int[numberOfStacks][stackCapacity];
        for (int i = 0; i < numberOfStacks; i++) {
            for (int j = 0; j < stackCapacity; j++) {
                this.stacks[i][j] = -1;
            }
        }
    }

    public int[] getItems() {
        return this.items;
    }

    public int[][] getStacks() {
        return this.stacks;
    }

    public int[][] getStackingConstraints() {
        return this.stackingConstraints;
    }

    public int[][] getCosts() {
        return this.costs;
    }

    public int getStackCapacity() {
        return this.stackCapacity;
    }
}
