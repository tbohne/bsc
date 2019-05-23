package SP.representations;

import SP.util.RepresentationUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a solution of a stacking problem.
 *
 * @author Tim Bohne
 */
public class Solution implements Comparable<Solution> {

    private Instance solvedInstance;
    private double timeToSolve;
    private double timeLimit;
    private int[][] filledStacks;
    private boolean empty;

    /**
     * Constructor for empty solutions.
     */
    public Solution() {
        this.empty = true;
        this.filledStacks = new int[0][];
    }

    /**
     * Constructor
     *
     * @param timeToSolve    - time it took to create the solution
     * @param timeLimit      - considered time limit
     * @param solvedInstance - solved instance
     */
    public Solution(double timeToSolve, double timeLimit, Instance solvedInstance) {
        this.timeToSolve = timeToSolve;
        this.empty = false;
        this.timeLimit = timeLimit;
        this.solvedInstance = solvedInstance;
        this.createFilledStacksFromInstance();
    }

    /**
     * Copy-Constructor
     *
     * @param sol - solution to be copied
     */
    public Solution(Solution sol) {
        this.timeToSolve = sol.getTimeToSolveAsDouble();
        this.empty = sol.isEmpty();
        this.solvedInstance = new Instance(sol.getSolvedInstance());
        this.timeLimit = sol.getTimeLimit();

        this.filledStacks = new int[sol.solvedInstance.getStacks().length][];
        for (int i = 0; i < sol.getFilledStacks().length; i++) {
            this.filledStacks[i] = sol.getFilledStacks()[i].clone();
        }
    }

    /**
     * Creates the filled stacks from the solved instance's stack assignments.
     */
    public void createFilledStacksFromInstance() {
        this.filledStacks = new int[this.solvedInstance.getStacks().length][];
        for (int i = 0; i < this.solvedInstance.getStacks().length; i++) {
            this.filledStacks[i] = this.solvedInstance.getStacks()[i].clone();
        }
    }

    /**
     * Returns the solution's stacks filled with the assigned items.
     *
     * @return filled stacks of the solution
     */
    public int[][] getFilledStacks() {
        return this.filledStacks;
    }

    /**
     * Returns the instance the solution was generated for.
     *
     * @return solved instance
     */
    public Instance getSolvedInstance() {
        return this.solvedInstance;
    }

    /**
     * Returns the considered time limit for the solution generation.
     *
     * @return considered time limit
     */
    public double getTimeLimit() {
        return this.timeLimit;
    }

    /**
     * Returns whether the solution is empty which means that the items are not yet assigned to stacks.
     *
     * @return whether the solution is empty
     */
    public boolean isEmpty() {
        return this.empty;
    }

    /**
     * Transforms the given, possibly unordered stack assignments into feasible ones by sorting the items
     * with regard to the transitive stacking constraints.
     */
    public void sortItemsInStacksBasedOnTransitiveStackingConstraints() {

        for (int stack = 0; stack < this.filledStacks.length; stack++) {

            // retrieve indices of items that are assigned to the stack and clear stack positions
            List<Integer> itemIndices = new ArrayList<>();
            for (int level = 0; level < this.solvedInstance.getStackCapacity(); level++) {
                if (this.filledStacks[stack][level] != -1) {
                    itemIndices.add(this.filledStacks[stack][level]);
                    this.filledStacks[stack][level] = -1;
                }
            }
            // retrieve item objects by indices
            List<Item> items = new ArrayList<>();
            for (int itemIdx : itemIndices) {
                // an item's idx should be the same as its position in the array
                if (itemIdx == this.solvedInstance.getItemObjects()[itemIdx].getIdx()) {
                    items.add(this.solvedInstance.getItemObjects()[itemIdx]);
                // if it's not, the item has to be retrieved manually
                } else {
                    for (Item item : this.solvedInstance.getItemObjects()) {
                        if (item.getIdx() == itemIdx) {
                            items.add(item);
                        }
                    }
                }
            }
            // sort items with regard to the transitive stacking constraints
            Collections.sort(items);

            // if the stack isn't completely filled, the offset is used to start at the correct top level
            int offset = this.solvedInstance.getStackCapacity() - items.size();

            int idx = 0;
            for (int level = 0 + offset; level < this.solvedInstance.getStackCapacity(); level++) {
                if (idx < items.size()) {
                    this.filledStacks[stack][level] = items.get(idx++).getIdx();
                }
            }
        }
    }

    /**
     * Returns whether all of the instance's items have been assigned to a stack.
     *
     * @return whether all items have been assigned to a stack
     */
    public boolean allItemsAssigned() {
        boolean[] allItemsAssigned = new boolean[this.solvedInstance.getItems().length];
        for (int i = 0; i < this.filledStacks.length; i++) {
            for (int j = 0; j < this.filledStacks[i].length; j++) {
                if (this.filledStacks[i][j] != -1) {
                    allItemsAssigned[this.filledStacks[i][j]] = true;
                }
            }
        }
        for (boolean itemAssigned : allItemsAssigned) {
            if (!itemAssigned) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns whether the stacking constraints are respected by the solution's stack assignments.
     *
     * @return whether the solution is respecting the stacking constraints
     */
    public boolean stackingConstraintsRespected() {
        for (int stack = 0; stack < this.filledStacks.length; stack++) {
            for (int level = 1; level < this.filledStacks[stack].length; level++) {
                int itemBelow = this.filledStacks[stack][level];
                int itemAbove = this.filledStacks[stack][level - 1];
                if (itemAbove != -1 && itemBelow != -1) {
                    if (this.solvedInstance.getStackingConstraints()[itemAbove][itemBelow] != 1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Returns whether the placement constraints are respected by the solution's stack assignments.
     *
     * @return whether the solution is respecting the placement constraints
     */
    public boolean placementConstraintsRespected() {
        for (int stack = 0; stack < this.filledStacks.length; stack++) {
            for (int level = 0; level < this.filledStacks[stack].length; level++) {
                int item = this.filledStacks[stack][level];
                if (item != -1) {
                    if (this.solvedInstance.getCosts()[item][stack] >= Integer.MAX_VALUE / this.solvedInstance.getItems().length) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Returns whether the solution contains duplicates (items occurring more than once).
     *
     * @return whether the solution contains duplicate items
     */
    public boolean containsDuplicates() {
        ArrayList<Integer> assignedItems = new ArrayList<>();
        for (int i = 0; i < this.filledStacks.length; i++) {
            for (int j = 0; j < this.filledStacks[i].length; j++) {
                if (this.filledStacks[i][j] != -1) {
                    if (assignedItems.contains(this.filledStacks[i][j])) {
                        return true;
                    }
                    assignedItems.add(this.filledStacks[i][j]);
                }
            }
        }
        return false;
    }

    /**
     * Sets the solution's time to solve the instance.
     *
     * @param duration - the duration it took to solve the instance
     */
    public void setTimeToSolve(double duration) {
        this.timeToSolve = duration;
    }

    public void lowerItemsThatAreStackedInTheAir() {
        for (int stack = 0; stack < this.filledStacks.length; stack++) {
            boolean loweredItem = true;
            while (loweredItem) {
                loweredItem = false;
                for (int level = this.filledStacks[stack].length - 1; level > 0; level--) {
                    if (this.filledStacks[stack][level] == -1 && this.filledStacks[stack][level - 1] != -1) {
                        this.filledStacks[stack][level] = this.filledStacks[stack][level - 1];
                        this.filledStacks[stack][level - 1] = -1;
                        loweredItem = true;
                        break;
                    }
                }
            }
        }
    }

    public boolean containsGaps() {
        for (int stack = 0; stack < this.filledStacks.length; stack++) {
            for (int level = this.filledStacks[stack].length - 1; level > 0; level--) {
                if (this.filledStacks[stack][level] == -1 && this.filledStacks[stack][level - 1] != -1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns whether the solution is feasible which means that all items have been
     * assigned to a position in a stack in a way that respects the stacking- and
     * placement-constraints. Additionally no item have been assigned to more than one position.
     *
     * @return whether the solution is feasible
     */
    public boolean isFeasible() {
        if (!this.empty) {
//            System.out.println("all items assigned: " + this.allItemsAssigned());
//            System.out.println("stacking constraints resp.: " + this.stackingConstraintsRespected());
//            System.out.println("placement constraints resp.: " + this.placementConstraintsRespected());
//            System.out.println("items assigned: " + this.getNumberOfAssignedItems());
//            System.out.println("contains duplicates: " + this.containsDuplicates());
//            System.out.println("contains gaps: " + this.containsGaps());
        }
        return !this.empty && this.allItemsAssigned() && this.stackingConstraintsRespected()
            && this.placementConstraintsRespected() && !this.containsDuplicates() && !this.containsGaps();
    }

    /**
     * Prints the solution's storage area.
     */
    public void printStorageArea() {
        for (int i = 0; i < this.getFilledStacks().length; i++) {
            for (int j = 0; j < this.getFilledStacks()[i].length; j++) {
                System.out.print(this.getFilledStacks()[i][j] + " ");
            }
            System.out.println();
        }
    }

    /**
     * Returns the solution's number of assigned items.
     *
     * @return the solution's number of assigned items
     */
    public int getNumberOfAssignedItems() {
        return this.getAssignedItems().size();
    }

    /**
     * Returns the solution's list of assigned items.
     *
     * @return the solution's list of assigned items
     */
    public ArrayList<Integer> getAssignedItems() {
        if (this.empty) {
            return new ArrayList<>();
        }
        ArrayList<Integer> assignedItems = new ArrayList<>();
        for (int i = 0; i < this.filledStacks.length; i++) {
            for (int j = 0; j < this.filledStacks[i].length; j++) {
                if (this.filledStacks[i][j] != -1) {
                    assignedItems.add(this.filledStacks[i][j]);
                }
            }
        }
        return assignedItems;
    }

    /**
     * Computes the solution's costs.
     *
     * @return the solution's costs
     */
    public double computeCosts() {
        if (this.empty) {
            return Double.MAX_VALUE;
        }
        double costs = 0.0;
        for (int stack = 0; stack < this.filledStacks.length; stack++) {
            for (int level = 0; level < this.filledStacks[stack].length; level++) {
                int item = this.filledStacks[stack][level];
                if (item != -1) {
                    costs += this.solvedInstance.getCosts()[item][stack];
                }
            }
        }
        return Math.round(costs * 100.0) / 100.0;
    }

    /**
     * Returns a string representation of the solutions time to solve the instance.
     *
     * @return the solution's time to solve the instance
     */
    public String getTimeToSolve() {
        return String.format("%.02f", this.timeToSolve).replace(",", ".");
    }

    /**
     * Returns the solution's time to solve the instance.
     *
     * @return the solution's time to solve the instance
     */
    public double getTimeToSolveAsDouble() {
        return this.timeToSolve;
    }

    /**
     * Returns the string representation of the solution's objective value.
     *
     * @return the solution's objective value
     */
    public String getObjectiveValue() {
        return String.format("%.02f", this.computeCosts()).replace(",", ".");
    }

    /**
     * Returns the solution's objective value.
     *
     * @return the solution's objective value
     */
    public double getObjectiveValueAsDouble() {
        return this.computeCosts();
    }

    /**
     * Returns the name of the solved instance.
     *
     * @return the name of the solved instance
     */
    public String getNameOfSolvedInstance() {
        return this.solvedInstance.getName();
    }

    /**
     * Returns a string visualizing the solution.
     *
     * @return a string visualizing the solution
     */
    @Override
    public String toString() {

        String str = "";

        if (this.isFeasible()) {
            str += "time limit: " + this.timeLimit + " s\n";
            str += "time to solve: " + String.format("%.2f", this.timeToSolve) + " s";
            str += this.timeToSolve > this.timeLimit ? " (time limit exceeded)\n" : "\n";
            str += "objective value: " + this.computeCosts();
            str += this.timeToSolve > this.timeLimit ? " (not optimal)\n\n" : "\n\n";
            str += "stacks (top to bottom):\n";

            int maxStringOffset = RepresentationUtil.getMaximumStringOffset(this.filledStacks.length);

            for (int i = 0; i < this.filledStacks.length; i++) {
                String space = RepresentationUtil.getCurrentSpace(i, maxStringOffset);
                str += "stack " + space + i + ":    ";
                for (int j = 0; j < this.filledStacks[i].length; j++) {
                    if (this.filledStacks[i][j] != -1) {
                        str += this.filledStacks[i][j] + " ";
                    }
                }
                str += "\n";
            }
        } else {
            str += "Problem not solved.\n";
        }
        return str;
    }

    public int compareTo(Solution other) {
        if (this.computeCosts() < other.computeCosts()) {
            return -1;
        } else if (other.computeCosts() < this.computeCosts()) {
            return 1;
        }
        return 0;
    }
}
