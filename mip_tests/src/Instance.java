import java.util.ArrayList;

public class Instance {

    private ArrayList<Integer> items;
    private ArrayList<ArrayList<Integer>> stacks;
    private ArrayList<ArrayList<Integer>> stackingConstraints;
    private ArrayList<ArrayList<Integer>> c;
    private int b;

    public Instance(
            int numberOfItems,
            int numberOfStacks,
            int b,
            ArrayList<ArrayList<Integer>> stackingConstraints,
            ArrayList<ArrayList<Integer>> c
    ) {

        this.items = new ArrayList<>();
        this.stacks = new ArrayList<>();

        for (int i = 0; i < numberOfItems; i++) {
            this.items.add(i);
        }

        for (int i = 0; i < numberOfStacks; i++) {
            this.stacks.add(new ArrayList<>());
        }

        this.b = b;
        this.stackingConstraints = stackingConstraints;
        this.c = c;
    }

    public ArrayList<Integer> getItems() {
        return items;
    }

    public ArrayList<ArrayList<Integer>> getStacks() {
        return stacks;
    }

    public ArrayList<ArrayList<Integer>> getStackingConstraints() {
        return stackingConstraints;
    }

    public ArrayList<ArrayList<Integer>> getCosts() {
        return c;
    }

    public int getStackCapacity() {
        return b;
    }
}
