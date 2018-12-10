package SLP;

public class Solution {

    private double timeToSolve;
    private double objectiveValue;

    private int[][] filledStorageArea;
    private boolean empty;

    private String nameOfSolvedInstance;
    private boolean timeLimitExceeded;

    private Instance solvedInstance;

    private int numberOfItems;

    public Solution() {
        this.empty = true;
    }

    public Solution(double timeToSolve, double objectiveValue, boolean timeLimitExceeded, Instance solvedInstance) {
        this.timeToSolve = timeToSolve;
        this.objectiveValue = objectiveValue;
        this.empty = false;
        this.nameOfSolvedInstance = solvedInstance.getName();
        this.timeLimitExceeded = timeLimitExceeded;
        this.numberOfItems = solvedInstance.getItems().length;

        this.solvedInstance = solvedInstance;

        this.filledStorageArea = new int[solvedInstance.getStacks().length][];
        for (int i = 0; i < solvedInstance.getStacks().length; i++) {
            this.filledStorageArea[i] = solvedInstance.getStacks()[i].clone();
        }
    }

    public int[][] getFilledStorageArea() {
        return this.filledStorageArea;
    }

    public boolean isEmpty() {
        return this.empty;
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

        for (int i = 0; i < this.filledStorageArea.length; i++) {
            for (int j = 1; j < this.filledStorageArea[i].length; j++) {

                int itemAbove = this.filledStorageArea[i][j];
                int itemBelow = this.filledStorageArea[i][j - 1];

                if (itemAbove != -1 && itemBelow != -1) {
                    if (this.solvedInstance.getStackingConstraints()[itemAbove][itemBelow] != 1) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean isFeasible() {

        // TODO:
        //      - all Items assigned to a stack
        //      - stacking constraints respected

        return !this.empty && this.allItemsAssigned() && this.stackingConstraintsRespected();
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

    public String getTimeToSolve() {
        return String.format("%.02f", this.timeToSolve);
    }

    public String getObjectiveValue() {
        return String.format("%.02f", this.objectiveValue);
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
            str += "time to solve: " + String.format("%.2f", this.timeToSolve) + " s";
            str += timeLimitExceeded ? " (time limit exceeded)\n" : "\n";
            str += "objective value: " + this.objectiveValue;
            str += timeLimitExceeded ? " (not optimal)\n\n" : "\n\n";
            str += "stacks (top to bottom):\n";

            int maxStringOffset = getMaximumStringOffset(this.filledStorageArea.length);

            for (int i = 0; i < this.filledStorageArea.length; i++) {

                String space = getCurrentSpace(i, maxStringOffset);

                str += "stack " + space + i + ":    ";
                for (int j = this.filledStorageArea[i].length - 1; j >= 0; j--) {
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
