import java.util.ArrayList;

public class Instance {

    private ArrayList<Integer> items;
    private ArrayList<ArrayList<Integer>> stacks;
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

        this.items = new ArrayList<>();
        this.stacks = new ArrayList<>();

        for (int i = 0; i < numberOfItems; i++) {
            this.items.add(i);
        }

        for (int i = 0; i < numberOfStacks; i++) {
            this.stacks.add(new ArrayList<>());
        }

        this.stackCapacity = stackCapacity;
        this.stackingConstraints = stackingConstraints;
        this.costs = costs;
    }

    public ArrayList<Integer> getItems() {
        return items;
    }

    public ArrayList<ArrayList<Integer>> getStacks() {
        return stacks;
    }

    public int[][] getStackingConstraints() {
        return stackingConstraints;
    }

    public int[][] getCosts() {
        return costs;
    }

    public int getStackCapacity() {
        return stackCapacity;
    }
}
