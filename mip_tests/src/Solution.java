public class Solution {

    private double timeToSolve;
    private double objectiveValue;

    private int[][] filledStorageArea;
    private boolean empty;

    public Solution() {
        this.empty = true;
    }

    public Solution(double timeToSolve, double objectiveValue, int[][] filledStorageArea) {
        this.timeToSolve = timeToSolve;
        this.objectiveValue = objectiveValue;
        this.empty = false;

        this.filledStorageArea = new int[filledStorageArea.length][];
        for (int i = 0; i < filledStorageArea.length; i++) {
            this.filledStorageArea[i] = filledStorageArea[i].clone();
        }
    }

    public String toString() {

        String str = "";

        if (!this.empty) {
            str += "time to solve: " + String.format("%.2f", this.timeToSolve) + " s\n";
            str += "objective value: " + this.objectiveValue + "\n\n";
            str += "stacks (top to bottom):\n";

            for (int i = 0; i < this.filledStorageArea.length; i++) {
                str += "stack " + i + ":    ";
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
