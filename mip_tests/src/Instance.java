public class Instance {

    private int[] items;
    private int[][] stacks;

    private int[][] stackingConstraints;
    private int[][] costs;
    private int stackCapacity;

    private String name;

    public Instance(
            int numberOfItems,
            int numberOfStacks,
            int stackCapacity,
            int[][] stackingConstraints,
            int[][] costs,
            String name
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
        this.name = name;
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

    public String getName() { return this.name; }

    public String toString() {

        String str = "********************************************************************\n";
        str += "instance: " + this.getName() + "\n";
        str += "items: ";

        for (int item : this.getItems()) {
            str += item + " ";
        }

        str += "\nnumber of stacks: " + this.getStacks().length + "\n";
        str += "stack capacity: " + this.getStackCapacity() + "\n\n";
        str += "stacking constraints:" + "\n";

        for (int i = 0; i < this.getItems().length; i++) {
            for (int j = 0; j < this.getItems().length; j++) {
                str += this.getStackingConstraints()[i][j] + " ";
            }
            str += "\n";
        }
        str += "\nstacking costs:\n";

        for (int i = 0; i < this.getItems().length; i++) {
            for (int j = 0; j < this.getStacks().length; j++) {
                str += this.getCosts()[i][j] + " ";
            }
            str += "\n";
        }
        str += "********************************************************************\n";

        return str;
    }
}
