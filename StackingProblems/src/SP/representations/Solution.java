package SP.representations;

import SP.util.RepresentationUtil;

import java.util.ArrayList;

/**
 * Represents a solution to a stacking problem.
 *
 * @author Tim Bohne
 */
public class Solution {

    private String nameOfSolvedInstance;
    private Instance solvedInstance;
    private double timeToSolve;
    private int timeLimit;
    private double objectiveValue;
    private int[][] filledStorageArea;
    private boolean empty;

    /**
     * Constructor for empty solutions.
     */
    public Solution() {
        this.empty = true;
    }

    /**
     * Constructor receiving the objective value.
     *
     * @param timeToSolve    - the time it took to create the solution
     * @param objectiveValue - the solution's objective value
     * @param timeLimit      - the considered time limit
     * @param solvedInstance - the solved instance
     */
    public Solution(double timeToSolve, double objectiveValue, int timeLimit, Instance solvedInstance) {
        this.timeToSolve = timeToSolve;
        this.objectiveValue = objectiveValue;
        this.empty = false;
        this.nameOfSolvedInstance = solvedInstance.getName();
        this.timeLimit = timeLimit;
        this.solvedInstance = solvedInstance;
        this.createFilledStorageAreaFromInstance();
    }

    /**
     * Constructor not receiving the objective value.
     *
     * @param timeToSolve    - the time it took to create the solution
     * @param timeLimit      - the considered time limit
     * @param solvedInstance - the solved instance
     */
    public Solution(double timeToSolve, int timeLimit, Instance solvedInstance) {
        this.timeToSolve = timeToSolve;
        this.empty = false;
        this.nameOfSolvedInstance = solvedInstance.getName();
        this.timeLimit = timeLimit;
        this.solvedInstance = solvedInstance;
        this.createFilledStorageAreaFromInstance();
        this.objectiveValue = this.computeCosts();
    }

    /**
     * Copy-Constructor
     *
     * @param sol - the solution to be copied
     */
    public Solution(Solution sol) {
        this.timeToSolve = sol.getTimeToSolveAsDouble();
        this.objectiveValue = sol.getObjectiveValueAsDouble();
        this.empty = sol.isEmpty();
        this.solvedInstance = new Instance(sol.solvedInstance);
        this.nameOfSolvedInstance = sol.getNameOfSolvedInstance();
        this.timeLimit = sol.getTimeLimit();

        this.filledStorageArea = new int[sol.solvedInstance.getStacks().length][];
        for (int i = 0; i < sol.getFilledStorageArea().length; i++) {
            this.filledStorageArea[i] = sol.getFilledStorageArea()[i].clone();
        }
    }

    /**
     * Creates the filled storage area from the instance's stack assignments.
     */
    public void createFilledStorageAreaFromInstance() {
        this.filledStorageArea = new int[this.solvedInstance.getStacks().length][];
        for (int i = 0; i < this.solvedInstance.getStacks().length; i++) {
            this.filledStorageArea[i] = this.solvedInstance.getStacks()[i].clone();
        }
    }

    /**
     * Returns the filled storage area.
     *
     * @return the filled storage area
     */
    public int[][] getFilledStorageArea() {
        return this.filledStorageArea;
    }

    /**
     * Returns the considered time limit.
     *
     * @return the considered time limit
     */
    public int getTimeLimit() {
        return this.timeLimit;
    }

    /**
     * Returns whether the solution is empty.
     *
     * @return whether the solution is empty
     */
    public boolean isEmpty() {
        return this.empty;
    }

    /**
     * Transforms the given stack assignments into a feasible solution by reorganizing
     * the items in a way that respects the stacking constraints.
     */
    public void transformStackAssignmentsIntoValidSolutionIfPossible() {

        // TODO: revise

        for (int stack = 0; stack < this.filledStorageArea.length; stack++) {
            boolean somethingChanged = true;

            while (somethingChanged) {
                somethingChanged = false;

                for (int item = 0; item < this.solvedInstance.getStackCapacity() - 1; item++) {
                    if (this.filledStorageArea[stack][item] != -1 && this.filledStorageArea[stack][item + 1] != -1) {
                        if (this.solvedInstance.getStackingConstraints()[this.filledStorageArea[stack][item]][this.filledStorageArea[stack][item + 1]] == 0) {
                            int tmp = this.filledStorageArea[stack][item];
                            this.filledStorageArea[stack][item] = this.filledStorageArea[stack][item + 1];
                            this.filledStorageArea[stack][item + 1] = tmp;
                            somethingChanged = true;
                            break;
                        }
                    }
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
        for (int i = 0; i < this.filledStorageArea.length; i++) {
            for (int j = 0; j < this.filledStorageArea[i].length; j++) {
                if (this.filledStorageArea[i][j] != -1) {
                    allItemsAssigned[this.filledStorageArea[i][j]] = true;
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
        for (int stack = 0; stack < this.filledStorageArea.length; stack++) {
            for (int level = 1; level < this.filledStorageArea[stack].length; level++) {
                int itemBelow = this.filledStorageArea[stack][level];
                int itemAbove = this.filledStorageArea[stack][level - 1];
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
        for (int stack = 0; stack < this.filledStorageArea.length; stack++) {
            for (int level = 0; level < this.filledStorageArea[stack].length; level++) {
                int item = this.filledStorageArea[stack][level];
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
        for (int i = 0; i < this.filledStorageArea.length; i++) {
            for (int j = 0; j < this.filledStorageArea[i].length; j++) {
                if (this.filledStorageArea[i][j] != -1) {
                    if (assignedItems.contains(this.filledStorageArea[i][j])) {
                        return true;
                    }
                    assignedItems.add(this.filledStorageArea[i][j]);
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

    /**
     * Returns whether the solution is feasible which means that all items have been
     * assigned to a position in a stack in a way that respects the stacking- and
     * placement-constraints. Additionally no item have been assigned to more than one position.
     *
     * @return whether the solution is feasible
     */
    public boolean isFeasible() {
        if (!this.empty) {
            System.out.println("all items assigned: " + this.allItemsAssigned());
            System.out.println("stacking constraints resp.: " + this.stackingConstraintsRespected());
            System.out.println("placement constraints resp.: " + this.placementConstraintsRespected());
            System.out.println("items assigned: " + this.getNumberOfAssignedItems());
            System.out.println("contains duplicates: " + this.containsDuplicates());
        }
        return !this.empty && this.allItemsAssigned() && this.stackingConstraintsRespected()
            && this.placementConstraintsRespected() && !this.containsDuplicates();
    }

    /**
     * Prints the solution's storage area.
     */
    public void printStorageArea() {
        for (int i = 0; i < this.getFilledStorageArea().length; i++) {
            for (int j = 0; j < this.getFilledStorageArea()[i].length; j++) {
                System.out.print(this.getFilledStorageArea()[i][j] + " ");
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
        if (this.empty) {
            return 0;
        }
        int numberOfAssignedItems = 0;
        for (int i = 0; i < this.filledStorageArea.length; i++) {
            for (int j = 0; j < this.filledStorageArea[i].length; j++) {
                if (this.filledStorageArea[i][j] != -1) {
                    numberOfAssignedItems++;
                }
            }
        }
        return numberOfAssignedItems;
    }

    /**
     * Computes the solution's costs.
     *
     * @return the solution's costs
     */
    public int computeCosts() {
        if (this.empty) {
            return 9999999;
        }
        int costs = 0;
        for (int stack = 0; stack < this.filledStorageArea.length; stack++) {
            for (int level = 0; level < this.filledStorageArea[stack].length; level++) {
                int item = this.filledStorageArea[stack][level];
                if (item != -1) {
                    costs += this.solvedInstance.getCosts()[item][stack];
                }
            }
        }
        return costs;
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
        return String.format("%.02f", this.objectiveValue).replace(",", ".");
    }

    /**
     * Returns the solution's objective value.
     *
     * @return the solution's objective value
     */
    public double getObjectiveValueAsDouble() {
        return this.objectiveValue;
    }

    /**
     * Returns the name of the solved instance.
     *
     * @return the name of the solved instance
     */
    public String getNameOfSolvedInstance() {
        return this.nameOfSolvedInstance;
    }

    /**
     * Returns a string visualizing the solution.
     *
     * @return a string visualizing the solution
     */
    public String toString() {

        String str = "";

        if (this.isFeasible()) {
            str += "time limit: " + this.timeLimit + " s\n";
            str += "time to solve: " + String.format("%.2f", this.timeToSolve) + " s";
            str += this.timeToSolve > this.timeLimit ? " (time limit exceeded)\n" : "\n";
            str += "objective value: " + this.objectiveValue;
            str += this.timeToSolve > this.timeLimit ? " (not optimal)\n\n" : "\n\n";
            str += "stacks (top to bottom):\n";

            int maxStringOffset = RepresentationUtil.getMaximumStringOffset(this.filledStorageArea.length);

            for (int i = 0; i < this.filledStorageArea.length; i++) {
                String space = RepresentationUtil.getCurrentSpace(i, maxStringOffset);
                str += "stack " + space + i + ":    ";
                for (int j = 0; j < this.filledStorageArea[i].length; j++) {
                    if (this.filledStorageArea[i][j] != -1) {
                        str += this.filledStorageArea[i][j] + " ";
                    }
                }
                str += "\n";
            }
        } else {
            str += "Problem not solved.\n";
        }
        return str;
    }
}
