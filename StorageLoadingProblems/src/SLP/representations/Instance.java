package SLP.representations;

import java.util.ArrayList;

public class Instance {

    private int[] items;
    private int[][] stacks;
    private ArrayList<Coordinates> itemPositions;
    private ArrayList<Coordinates> stackPositions;

    private int[][] stackingConstraints;
    private int[][] costs;
    private int stackCapacity;

    private String name;

    public Instance(
            int numberOfItems,
            int numberOfStacks,
            ArrayList<Coordinates> itemPositions,
            ArrayList<Coordinates> stackPositions,
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

        this.itemPositions = new ArrayList<>();
        for (Coordinates coords : itemPositions) {
            this.itemPositions.add(new Coordinates(coords));
        }

        this.stackPositions = new ArrayList<>();
        for (Coordinates coords : stackPositions) {
            this.stackPositions.add(new Coordinates(coords));
        }

        for (int i = 0; i < numberOfItems; i++) {
            this.items[i] = i;
        }

        this.stackCapacity = stackCapacity;
        this.stackingConstraints = stackingConstraints;
        this.costs = costs;
        this.name = name;
    }

    public Instance(Instance instance) {
        this.stacks = new int[instance.getStacks().length][instance.getStackCapacity()];
        for (int i = 0; i < instance.getStacks().length; i++) {
            for (int j = 0; j < instance.getStackCapacity(); j++) {
                this.stacks[i][j] = instance.getStacks()[i][j];
            }
        }

        this.items = instance.getItems().clone();
        this.stackCapacity = instance.getStackCapacity();

        this.stackingConstraints = new int[instance.getStackingConstraints().length][];
        for (int i = 0; i < instance.getStackingConstraints().length; i++) {
            this.stackingConstraints[i] = instance.getStackingConstraints()[i].clone();
        }

        this.costs = new int[instance.getCosts().length][];
        for (int i = 0; i < instance.getCosts().length; i++) {
            this.costs[i] = instance.getCosts()[i].clone();
        }
        this.name = instance.getName();
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

    public void removeItemListFromStorageArea(ArrayList<Integer> items) {
        for (int i = 0; i < this.getStacks().length; i++) {
            for (int j = 0; j < this.getStacks()[i].length; j++) {
                if (items.contains(this.getStacks()[i][j])) {
                    this.getStacks()[i][j] = -1;
                }
            }
        }
    }

    public int[] getItems() {
        return this.items;
    }

    public int[][] getStacks() {
        return this.stacks;
    }

    public ArrayList<Coordinates> getItemPositions() {
        return this.itemPositions;
    }

    public ArrayList<Coordinates> getStackPositions() {
        return this.stackPositions;
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
