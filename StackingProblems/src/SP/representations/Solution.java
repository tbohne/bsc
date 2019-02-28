package SP.representations;

import java.util.ArrayList;

public class Solution {

    private double timeToSolve;
    private int timeLimit;
    private double objectiveValue;

    private int[][] filledStorageArea;
    private boolean empty;

    private String nameOfSolvedInstance;

    private Instance solvedInstance;

    private int numberOfItems;

    public Solution() {
        this.empty = true;
    }

    public Solution(double timeToSolve, double objectiveValue, int timeLimit, Instance solvedInstance) {
        this.timeToSolve = timeToSolve;
        this.objectiveValue = objectiveValue;
        this.empty = false;
        this.nameOfSolvedInstance = solvedInstance.getName();
        this.timeLimit = timeLimit;
        this.numberOfItems = solvedInstance.getItems().length;

        this.solvedInstance = solvedInstance;

        this.filledStorageArea = new int[solvedInstance.getStacks().length][];
        for (int i = 0; i < solvedInstance.getStacks().length; i++) {
            this.filledStorageArea[i] = solvedInstance.getStacks()[i].clone();
        }
    }

    public Solution(double timeToSolve, int timeLimit, Instance solvedInstance) {
        this.timeToSolve = timeToSolve;
        this.empty = false;
        this.nameOfSolvedInstance = solvedInstance.getName();
        this.timeLimit = timeLimit;
        this.numberOfItems = solvedInstance.getItems().length;

        this.solvedInstance = solvedInstance;

        this.filledStorageArea = new int[solvedInstance.getStacks().length][];
        for (int i = 0; i < solvedInstance.getStacks().length; i++) {
            this.filledStorageArea[i] = solvedInstance.getStacks()[i].clone();
        }

        this.objectiveValue = this.getCost();
    }

    public Solution(Solution sol) {
        this.timeToSolve = sol.timeToSolve;
        this.objectiveValue = sol.objectiveValue;
        this.empty = sol.empty;
        this.numberOfItems = sol.numberOfItems;
        this.solvedInstance = new Instance(sol.solvedInstance);
        this.nameOfSolvedInstance = sol.nameOfSolvedInstance;
        this.timeLimit = sol.timeLimit;
        this.filledStorageArea = new int[sol.solvedInstance.getStacks().length][];
        for (int i = 0; i < sol.getFilledStorageArea().length; i++) {
            this.filledStorageArea[i] = sol.getFilledStorageArea()[i].clone();
        }
    }

    public int[][] getFilledStorageArea() {
        return this.filledStorageArea;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    // TODO: fix infinite loop for invalid stack assignments
    public void transformStackAssignmentIntoValidSolutionIfPossible() {
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

    public boolean allItemsAssigned() {

        boolean[] allItemsAssigned = new boolean[this.numberOfItems];

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

    public boolean stackConstraintsRespected() {
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

    public void setTimeToSolve(double duration) {
        this.timeToSolve = duration;
    }

    public boolean isFeasible() {

        if (!this.empty) {
            System.out.println("all items: " + this.allItemsAssigned());
            System.out.println("stacking: " + this.stackingConstraintsRespected());
            System.out.println("stack: " + this.stackConstraintsRespected());
            System.out.println("items assigned: " + this.getNumberOfAssignedItems());
            System.out.println("contains duplicates: " + this.containsDuplicates());
        }

        return !this.empty && this.allItemsAssigned() && this.stackingConstraintsRespected()
            && this.stackConstraintsRespected() && !this.containsDuplicates();
    }

    public void printStorageArea() {
        for (int i = 0; i < this.getFilledStorageArea().length; i++) {
            for (int j = 0; j < this.getFilledStorageArea()[i].length; j++) {
                System.out.print(this.getFilledStorageArea()[i][j] + " ");
            }
            System.out.println();
        }
    }

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

    public int getCost() {

        if (this.empty) {
            return 9999999;
        }

        int cost = 0;

        for (int stack = 0; stack < this.filledStorageArea.length; stack++) {
            for (int level = 0; level < this.filledStorageArea[stack].length; level++) {
                int item = this.filledStorageArea[stack][level];
                if (item != -1) {
                    cost += this.solvedInstance.getCosts()[item][stack];
                }
            }
        }
        return cost;
    }

    public String getTimeToSolve() {
        return String.format("%.02f", this.timeToSolve).replace(",", ".");
    }

    public String getObjectiveValue() {
        return String.format("%.02f", this.objectiveValue).replace(",", ".");
    }

    public double getObjectiveValueAsDouble() {
        return this.objectiveValue;
    }

    public String getNameOfSolvedInstance() {
        return this.nameOfSolvedInstance;
    }

    public int getMaximumStringOffset(int numberOfStacks) {
        int cnt = 0;
        while ((numberOfStacks / 10) > 0) {
            numberOfStacks /= 10;
            cnt++;
        }
        return cnt;
    }

    public String getCurrentSpace(int idx, int maxOffset) {
        String space = "";
        if (idx < 10) {
            for (int i = 0; i < maxOffset; i++) {
                space += " ";
            }
            return space;
        } else if (idx < 100) {
            for (int i = 0; i < maxOffset - 1; i++) {
                space += " ";
            }
            return space;
        } else if (idx < 1000) {
            for (int i = 0; i < maxOffset - 2; i++) {
                space += " ";
            }
            return space;
        } else {
            for (int i = 0; i < maxOffset - 3; i++) {
                space += " ";
            }
            return space;
        }
    }

    public String toString() {

        String str = "";

        if (this.isFeasible()) {
            str += "time limit: " + this.timeLimit + " s\n";
            str += "time to solve: " + String.format("%.2f", this.timeToSolve) + " s";
            str += this.timeToSolve > this.timeLimit ? " (time limit exceeded)\n" : "\n";
            str += "objective value: " + this.objectiveValue;
            str += this.timeToSolve > this.timeLimit ? " (not optimal)\n\n" : "\n\n";
            str += "stacks (top to bottom):\n";

            int maxStringOffset = getMaximumStringOffset(this.filledStorageArea.length);

            for (int i = 0; i < this.filledStorageArea.length; i++) {

                String space = getCurrentSpace(i, maxStringOffset);

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
