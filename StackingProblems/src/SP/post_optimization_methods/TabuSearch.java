package SP.post_optimization_methods;

import SP.representations.Instance;
import SP.representations.Solution;
import SP.representations.StorageAreaPosition;
import SP.util.HeuristicUtil;

import java.util.ArrayList;

public class TabuSearch {

    private Solution currSol;
    private Solution bestSol;

    private ArrayList<Exchange> tabuList;
    private int tabuListCleared;

    public TabuSearch(Solution initialSolution) {
        this.currSol = new Solution(initialSolution);
        this.bestSol = new Solution(initialSolution);
        this.tabuList = new ArrayList<>();
        this.tabuListCleared = 0;
    }

    public void clearTabuList() {
        this.tabuList = new ArrayList<>();
        this.tabuListCleared++;
    }

    public StorageAreaPosition getRandomPositionInStorageArea(Solution neighbor) {
        int stackIdx = HeuristicUtil.getRandomIntegerInBetween(0, neighbor.getFilledStorageArea().length - 1);
        int level = HeuristicUtil.getRandomIntegerInBetween(0, neighbor.getFilledStorageArea()[stackIdx].length - 1);
        return new StorageAreaPosition(stackIdx, level);
    }

    public void exchangeItems(Solution neighbor, StorageAreaPosition posOne, StorageAreaPosition posTwo) {
        int itemOne = neighbor.getFilledStorageArea()[posOne.getStackIdx()][posOne.getLevel()];
        int itemTwo = neighbor.getFilledStorageArea()[posTwo.getStackIdx()][posTwo.getLevel()];
        neighbor.getFilledStorageArea()[posOne.getStackIdx()][posOne.getLevel()] = itemTwo;
        neighbor.getFilledStorageArea()[posTwo.getStackIdx()][posTwo.getLevel()] = itemOne;
    }

    public Solution getNeighbor(boolean firstFit, boolean onlyValid) {

        ArrayList<Solution> nbrs = new ArrayList<>();

        int cnt = 0;

        while (nbrs.size() <= /*this.instance.getItems().length*/ 20) {

            Solution neighbor = new Solution(this.currSol);
            StorageAreaPosition posOne = this.getRandomPositionInStorageArea(neighbor);
            StorageAreaPosition posTwo = this.getRandomPositionInStorageArea(neighbor);

            this.exchangeItems(neighbor, posOne, posTwo);
            Exchange exchange = new Exchange(posOne, posTwo);

            // Only feasible solutions are considered for now.
            if (neighbor.isFeasible() && !this.tabuList.contains(exchange)) {
                nbrs.add(neighbor);
                this.tabuList.add(exchange);
            } else {
                if (this.tabuList.contains(exchange)) {
                    cnt++;
                }
                if (cnt == 5) {
                    System.out.println("CLEAR TABULIST");
                    this.clearTabuList();
                    cnt = 0;
                }
            }
        }

        return HeuristicUtil.getBestSolution(nbrs);
    }

    public Solution solveIterations(boolean firstFit, boolean onlyValid) {
        for (int i = 0; i < 1000; i++) {
            System.out.println(i);
            this.currSol = getNeighbor(firstFit, onlyValid);
            if (this.currSol.computeCosts() < this.bestSol.computeCosts()) {
                this.bestSol = this.currSol;
            }
        }
        return this.bestSol;
    }

    public Solution solve() {

        boolean firstFit = false;
        boolean onlyValid = true;

        Solution res = this.solveIterations(firstFit, onlyValid);
        System.out.println("val: " + res.getObjectiveValue());

        return res;
    }
}


