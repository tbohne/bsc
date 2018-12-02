package SLP;

public class Solution {

    private double timeToSolve;
    private double objectiveValue;

    private int[][] filledStorageArea;
    private boolean empty;

    private String nameOfSolvedInstance;
    private boolean timeLimitExceeded;

    private int numberOfItems;

    public Solution() {
        this.empty = true;
    }

    public Solution(double timeToSolve, double objectiveValue, int[][] filledStorageArea, String nameOfSolvedInstance, boolean timeLimitExceeded, int numberOfItems) {
        this.timeToSolve = timeToSolve;
        this.objectiveValue = objectiveValue;
        this.empty = false;
        this.nameOfSolvedInstance = nameOfSolvedInstance;
        this.timeLimitExceeded = timeLimitExceeded;
        this.numberOfItems = numberOfItems;

        this.filledStorageArea = new int[filledStorageArea.length][];
        for (int i = 0; i < filledStorageArea.length; i++) {
            this.filledStorageArea[i] = filledStorageArea[i].clone();
        }
    }

    public int[][] getFilledStorageArea() {
        return this.filledStorageArea;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public boolean isFeasible() {

        // TODO:
        //      - all Items assigned to a stack
        //      - stacking constraints respected

        // for now just checking whether all items are assigned to a stack

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

        if (!this.empty) {
            str += "time to solve: " + String.format("%.2f", this.timeToSolve) + " s";
            str += timeLimitExceeded ? " (time limit exceeded)\n" : "\n";
            str += "objective value: " + this.objectiveValue;
            str += timeLimitExceeded ? " (not optimal)\n\n" : "\n\n";
            str += "stacks (top to bottom):\n";

            int maxStringOffset = getMaximumStringOffset(this.filledStorageArea.length);

            for (int i = 0; i < this.filledStorageArea.length; i++) {

                String space = getCurrentSpace(i, maxStringOffset);

                str += "stack " + space + i + ":    ";
                for (int item : this.filledStorageArea[i]) {
                    if (item != -1) {
                        str += item + " ";
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
