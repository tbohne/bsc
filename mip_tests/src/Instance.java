public class Instance {

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
            this.items[i] = i;
        }

        this.stackCapacity = stackCapacity;
        this.stackingConstraints = stackingConstraints;
        this.costs = costs;
    }

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
