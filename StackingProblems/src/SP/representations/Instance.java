package SP.representations;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an instance of a stacking problem.
 *
 * @author Tim Bohne
 */
public class Instance {

    private String name;
    private Item[] items;
    private int[][] stacks;
    private int stackCapacity;
    private List<Position> stackPositions;
    private int[][] stackingConstraints;
    private double[][] costs;

    /**
     * Constructor
     *
     * @param items               - the items to be stored in the stacks
     * @param numberOfStacks      - the number of available stacks
     * @param stackPositions      - the positions of the stacks in the storage area
     * @param stackCapacity       - the maximum number of items per stack
     * @param stackingConstraints - the stacking constraints to be respected
     * @param costs               - the matrix containing the costs of item-stack-assignments
     * @param name                - the name of the instance
     */
    public Instance(Item[] items, int numberOfStacks, List<Position> stackPositions, int stackCapacity,
        int[][] stackingConstraints, double[][] costs, String name) {

            this.items = items;
            this.initStacks(numberOfStacks, stackCapacity);
            this.initStackPositions(stackPositions);
            this.stackCapacity = stackCapacity;
            this.stackingConstraints = stackingConstraints;
            this.costs = costs;
            this.name = name;
    }

    /**
     * Copy-Constructor
     *
     * @param instance - the instance to be copied
     */
    public Instance(Instance instance) {
        this.stackCapacity = instance.getStackCapacity();
        this.name = instance.getName();
        this.copyStacks(instance);
        this.copyItems(instance);
        this.copyStackPositions(instance);
        this.copyStackingConstraints(instance);
        this.copyCosts(instance);
    }

    /**
     * Initializes the available stacks. Each position inside the stacks is initialized with -1
     * which means that it's free (has no item assigned to it).
     *
     * @param numberOfStacks - specifies the instance's number of stacks
     * @param stackCapacity  - specifies the capacity the stacks have
     */
    public void initStacks(int numberOfStacks,int stackCapacity) {
        this.stacks = new int[numberOfStacks][stackCapacity];
        for (int stack = 0; stack < numberOfStacks; stack++) {
            for (int level = 0; level < stackCapacity; level++) {
                this.stacks[stack][level] = -1;
            }
        }
    }

    /**
     * Initializes the stack positions inside the storage area.
     *
     * @param stackPositions - the list of stack positions
     */
    public void initStackPositions(List<Position> stackPositions) {
        this.stackPositions = new ArrayList<>();
        for (Position pos : stackPositions) {
            this.stackPositions.add(new Position(pos));
        }
    }

    /**
     * Copies the given instance's stacks.
     *
     * @param instance - the instance the stacks get copied from
     */
    public void copyStacks(Instance instance) {
        this.stacks = new int[instance.getStacks().length][instance.getStackCapacity()];
        for (int stack = 0; stack < instance.getStacks().length; stack++) {
            for (int level = 0; level < instance.getStackCapacity(); level++) {
                this.stacks[stack][level] = instance.getStacks()[stack][level];
            }
        }
    }

    /**
     * Copies the given instance's stack positions.
     *
     * @param instance - the instance the stack positions get copied from
     */
    public void copyStackPositions(Instance instance) {
        this.stackPositions = new ArrayList<>();
        for (Position pos : instance.getStackPositions()) {
            stackPositions.add(new Position(pos));
        }
    }

    /**
     * Copies the given instance's items.
     *
     * @param instance - the instance the items get copied from
     */
    public void copyItems(Instance instance) {
        this.items = new Item[instance.getItems().length];
        for (int i = 0; i < instance.getItems().length; i++) {
            this.items[i] = new Item(instance.getItemObjects()[i]);
        }
    }

    /**
     * Copies the given instance's stacking constraints.
     *
     * @param instance - the instance the stacking constraints get copied from
     */
    public void copyStackingConstraints(Instance instance) {
        this.stackingConstraints = new int[instance.getStackingConstraints().length][];
        for (int i = 0; i < instance.getStackingConstraints().length; i++) {
            this.stackingConstraints[i] = instance.getStackingConstraints()[i].clone();
        }
    }

    /**
     * Copies the given instance's costs.
     *
     * @param instance - the instance the costs get copied from
     */
    public void copyCosts(Instance instance) {
        this.costs = new double[instance.getCosts().length][];
        for (int i = 0; i < instance.getCosts().length; i++) {
            this.costs[i] = instance.getCosts()[i].clone();
        }
    }

    /**
     * Resets the instance's stacks.
     */
    public void resetStacks() {
        int numberOfStacks = this.stacks.length;
        int stackCapacity = this.stacks[0].length;
        this.stacks = new int[numberOfStacks][stackCapacity];
        for (int stack = 0; stack < numberOfStacks; stack++) {
            for (int level = 0; level < stackCapacity; level++) {
                this.stacks[stack][level] = -1;
            }
        }
    }

    /**
     * Returns the ground level for the instance's stacks.
     *
     * @return the ground level for the instance's stacks
     */
    public int getGroundLevel() {
        return this.stackCapacity - 1;
    }

    /**
     * Returns the top level for the instance's stacks.
     *
     * @return the top level for the instance's stacks
     */
    public int getTopLevel() {
        return 0;
    }

    /**
     * Removes the specified list of items from the storage area.
     *
     * @param items - the list of items to be removed
     */
    public void removeItemListFromStorageArea(List<Integer> items) {
        for (int stack = 0; stack < this.getStacks().length; stack++) {
            for (int level = 0; level < this.getStacks()[stack].length; level++) {
                if (items.contains(this.getStacks()[stack][level])) {
                    this.getStacks()[stack][level] = -1;
                }
            }
        }
    }

    /**
     * Returns the instance's items.
     *
     * @return the instance's items
     */
    public int[] getItems() {
        int[] itemIndices = new int[this.items.length];
        for (int i = 0; i < itemIndices.length; i++) {
            itemIndices[i] = i;
        }
        return itemIndices;
    }

    /**
     * Returns the instance's item objects.
     *
     * @return the instance's item objects
     */
    public Item[] getItemObjects() {
        return this.items;
    }

    /**
     * Returns the instance's stacks.
     *
     * @return the instance's stacks
     */
    public int[][] getStacks() {
        return this.stacks;
    }

    /**
     * Returns the instance's stack positions.
     *
     * @return the instance's stack positions
     */
    public List<Position> getStackPositions() {
        return this.stackPositions;
    }

    /**
     * Returns the instance's matrix of stacking constraints.
     *
     * @return the instance's stacking constraints
     */
    public int[][] getStackingConstraints() {
        return this.stackingConstraints;
    }

    /**
     * Returns the instance's transport costs.
     *
     * @return the instance's transport costs
     */
    public double[][] getCosts() {
        return this.costs;
    }

    /**
     * Returns the instance's placement constraints.
     * Since the placement constraints are implemented via high cost entries,
     * they can be retrieved from the matrix of transport costs.
     *
     * @return the instance's placement constraints
     */
    public int[][] getPlacementConstraints() {

        int[][] placementConstraints = new int[this.items.length][this.stacks.length];

        for (int item = 0; item < this.items.length; item++) {
            for (int stack = 0; stack < this.stacks.length; stack++) {
                if (this.costs[item][stack] < Integer.MAX_VALUE / this.items.length) {
                    placementConstraints[item][stack] = 1;
                } else {
                    placementConstraints[item][stack] = 0;
                }
            }
        }
        return placementConstraints;
    }

    /**
     * Returns the instance's stack capacity.
     *
     * @return the instance's stack capacity
     */
    public int getStackCapacity() {
        return this.stackCapacity;
    }

    /**
     * Returns the instance's name.
     *
     * @return the name of the instance
     */
    public String getName() { return this.name; }

    /**
     * Returns a string containing the instance's data.
     *
     * @return the string containing the instance's data
     */
    public String toString() {

        String str = "********************************************************************\n";
        str += "instance: " + this.getName() + "\n";
        str += "items: ";

        for (Item item : this.getItemObjects()) {
            str += item.getIdx() + " ";
        }

        str += "\nnumber of stacks: " + this.getStacks().length + "\n";
        str += "stack capacity: " + this.getStackCapacity() + "\n\n";

        str += "item positions:\n";

        for (Item item : this.getItemObjects()) {
            str += item.getPosition() + " ";
        }
        str += "\nstack position:\n";
        str += this.getStackPositions() + "\n\n";

        str += "stacking constraints:" + "\n";

        for (int i = 0; i < this.getItems().length; i++) {
            for (int j = 0; j < this.getItems().length; j++) {
                str += this.getStackingConstraints()[i][j] + " ";
            }
            str += "\n";
        }
        str += "\n";

        str += "\nstacking costs:\n";

        for (int i = 0; i < this.getCosts().length; i++) {
            for (int j = 0; j < this.getCosts()[i].length; j++) {
                str += this.getCosts()[i][j] + " ";
            }
            str += "\n";
        }
        str += "********************************************************************\n";

        return str;
    }
}
