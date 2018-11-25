public class Solution {

    private double timeToSolve;
    private double objectiveValue;

    private int[][] filledStorageArea;

    public Solution(double timeToSolve, double objectiveValue, int[][] filledStorageArea) {
        this.timeToSolve = timeToSolve;
        this.objectiveValue = objectiveValue;

        this.filledStorageArea = new int[filledStorageArea.length][];
        for (int i = 0; i < filledStorageArea.length; i++) {
            this.filledStorageArea[i] = filledStorageArea[i].clone();
        }
    }

    public String toString() {

        String str = "";

        str += "Stacks (top to bottom):\n";

        for (int i = 0; i < this.filledStorageArea.length; i++) {
            str += "stack " + i + ": ";
            for (int item : this.filledStorageArea[i]) {
                if (item != -1) {
                    str += item + " ";
                }
            }
            str += "\n";
        }

        return str;
    }
}
