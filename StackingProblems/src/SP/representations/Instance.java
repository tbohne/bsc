package SP.representations;

import java.util.ArrayList;

/**
 * Represents an instance of a stacking problem.
 *
 * @author Tim Bohne
 */
public class Instance {

    private String name;

    private int[] items;
    private int[][] stacks;
    private int stackCapacity;
    private ArrayList<Position> itemPositions;
    private ArrayList<Position> stackPositions;
    private int[][] stackingConstraints;
    private double[][] costs;

    /**
     * Constructor
     *
     * @param numberOfItems       - the number of items to be stored in the storage area
     * @param numberOfStacks      - the number of available stacks
     * @param itemPositions       - the list of item positions
     * @param stackPositions      - the list of stack positions
     * @param stackCapacity       - the maximum number of items per stack
     * @param stackingConstraints - the stacking constraints to be respected
     * @param costs               - the matrix containing the costs of item-stack-assignments
     * @param name                - the name of the instance
     */
    public Instance(int numberOfItems, int numberOfStacks, ArrayList<Position> itemPositions,
        ArrayList<Position> stackPositions, int stackCapacity, int[][] stackingConstraints, double[][] costs, String name) {

            this.initItems(numberOfItems);
            this.initStacks(numberOfStacks, stackCapacity);
            this.initItemPositions(itemPositions);
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
        this.copyStacks(instance);
        this.items = instance.getItems().clone();
        this.stackCapacity = instance.getStackCapacity();
        this.copyItemPositions(instance);
        this.copyStackPositions(instance);
        this.copyStackingConstraints(instance);
        this.copyCosts(instance);
        this.name = instance.getName();
    }

    /**
     * Initializes the array of items.
     *
     * @param numberOfItems - specifies the instance's number of items
     */
    public void initItems(int numberOfItems) {
        this.items = new int[numberOfItems];
        for (int i = 0; i < numberOfItems; i++) {
            this.items[i] = i;
        }
    }

    /**
     * Initializes the available stacks. Each position inside the stacks
     * is initialized with -1 which means that it's free.
     *
     * @param numberOfStacks - specifies the instance's number of stacks
     * @param stackCapacity  - specifies the capacity the stacks have
     */
    public void initStacks(int numberOfStacks,int stackCapacity) {
        this.stacks = new int[numberOfStacks][stackCapacity];
        for (int i = 0; i < numberOfStacks; i++) {
            for (int j = 0; j < stackCapacity; j++) {
                this.stacks[i][j] = -1;
            }
        }
    }

    /**
     * Initializes the original item positions.
     *
     * @param itemPositions - the list of item positions
     */
    public void initItemPositions(ArrayList<Position> itemPositions) {
        this.itemPositions = new ArrayList<>();
        for (Position pos : itemPositions) {
            this.itemPositions.add(new Position(pos));
        }
    }

    /**
     * Initializes the stack positions inside the storage area.
     *
     * @param stackPositions - the list of stack positions
     */
    public void initStackPositions(ArrayList<Position> stackPositions) {
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
        for (int i = 0; i < instance.getStacks().length; i++) {
            for (int j = 0; j < instance.getStackCapacity(); j++) {
                this.stacks[i][j] = instance.getStacks()[i][j];
            }
        }
    }

    /**
     * Copies the given instance's item positions.
     *
     * @param instance - the instance the item positions get copied from
     */
    public void copyItemPositions(Instance instance) {
        this.itemPositions = new ArrayList<>();
        for (Position pos : instance.getItemPositions()) {
            itemPositions.add(new Position(pos));
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
        for (int i = 0; i < numberOfStacks; i++) {
            for (int j = 0; j < stackCapacity; j++) {
                this.stacks[i][j] = -1;
            }
        }
    }

    /**
     * Removes the specified list of items from the storage area.
     *
     * @param items - the list of items to be removed
     */
    public void removeItemListFromStorageArea(ArrayList<Integer> items) {
        for (int i = 0; i < this.getStacks().length; i++) {
            for (int j = 0; j < this.getStacks()[i].length; j++) {
                if (items.contains(this.getStacks()[i][j])) {
                    this.getStacks()[i][j] = -1;
                }
            }
        }
    }

    /**
     * Returns the instance's items.
     *
     * @return the items
     */
    public int[] getItems() {
        return this.items;
    }

    /**
     * Returns the instance's stacks.
     *
     * @return the stacks
     */
    public int[][] getStacks() {
        return this.stacks;
    }

    /**
     * Returns the instance's list of item positions.
     *
     * @return the item positions
     */
    public ArrayList<Position> getItemPositions() {
        return this.itemPositions;
    }

    /**
     * Returns the instance's list of stack positions.
     *
     * @return the stack positions
     */
    public ArrayList<Position> getStackPositions() {
        return this.stackPositions;
    }

    /**
     * Returns the instance's matrix of stacking constraints.
     *
     * @return the stacking constraints
     */
    public int[][] getStackingConstraints() {
        return this.stackingConstraints;
    }

    /**
     * Returns the instance's transport costs.
     *
     * @return the transport costs
     */
    public double[][] getCosts() {
        return this.costs;
    }

    /**
     * Returns the instance's stack capacity.
     *
     * @return the stack capacity
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

        for (int item : this.getItems()) {
            str += item + " ";
        }

        str += "\nnumber of stacks: " + this.getStacks().length + "\n";
        str += "stack capacity: " + this.getStackCapacity() + "\n\n";

        str += "item positions:\n";
        str += this.getItemPositions() + "\n";
        str += "stack position:\n";
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
