package SP.post_optimization_methods;

import SP.representations.Instance;
import SP.representations.Solution;
import SP.representations.StorageAreaPosition;
import SP.util.HeuristicUtil;

import java.util.ArrayList;

public class TabuSearch {

    private Instance instance;
    private Solution currSol;
    private Solution bestSol;

    private ArrayList<Exchange> tabuList;
    private int tabuListCleared;

    public TabuSearch(Instance instance, Solution initialSolution) {
        this.instance = instance;
        this.currSol = new Solution(initialSolution);
        this.bestSol = new Solution(initialSolution);
        this.tabuList = new ArrayList<>();
        this.tabuListCleared = 0;
    }

    public void clearTabuList() {
        this.tabuList = new ArrayList<>();
        this.tabuListCleared++;
    }

    public Solution getNeighbor(boolean firstFit, boolean onlyValid) {

        ArrayList<Solution> nbrs = new ArrayList<>();

        int cnt = 0;

        while (nbrs.size() <= /*this.instance.getItems().length*/ 20) {

            Solution neighbor = new Solution(this.currSol);

            // determine two random positions to be exchanged
            int stackIdxItemOne = HeuristicUtil.getRandomIntegerInBetween(0, neighbor.getFilledStorageArea().length - 1);
            int stackIdxItemTwo = HeuristicUtil.getRandomIntegerInBetween(0, neighbor.getFilledStorageArea().length - 1);
            int levelItemOne = HeuristicUtil.getRandomIntegerInBetween(0, neighbor.getFilledStorageArea()[stackIdxItemOne].length - 1);
            int levelItemTwo = HeuristicUtil.getRandomIntegerInBetween(0, neighbor.getFilledStorageArea()[stackIdxItemTwo].length - 1);

            StorageAreaPosition posOne = new StorageAreaPosition(stackIdxItemOne, levelItemOne);
            StorageAreaPosition posTwo = new StorageAreaPosition(stackIdxItemTwo, levelItemTwo);

            // Exchanges the positions of two items from the storage area.
            int itemOne = neighbor.getFilledStorageArea()[stackIdxItemOne][levelItemOne];
            int itemTwo = neighbor.getFilledStorageArea()[stackIdxItemTwo][levelItemTwo];
            neighbor.getFilledStorageArea()[stackIdxItemOne][levelItemOne] = itemTwo;
            neighbor.getFilledStorageArea()[stackIdxItemTwo][levelItemTwo] = itemOne;

            Exchange exchange = new Exchange(posOne, posTwo);

//            System.out.println(exchange);

            // Only feasible solutions are considered for now.
            if (neighbor.isFeasible() && !this.tabuList.contains(exchange)) {
                nbrs.add(neighbor);
                this.tabuList.add(exchange);

            } else {
                if (cnt == 5) {
                    this.clearTabuList();
                }
                cnt++;
                System.out.println(this.tabuList.contains(exchange));
//                System.out.println(this.tabuList.size());
//                System.out.println(this.tabuList.contains(exchange));
            }
        }

        return HeuristicUtil.getBestSolution(nbrs);
    }

    public Solution solveIterations(Instance instance, boolean firstFit, boolean onlyValid) {
        for (int i = 0; i < 1000; i++) {
            System.out.println(i);
            this.currSol = getNeighbor(firstFit, onlyValid);
            if (this.currSol.computeCosts() < this.bestSol.computeCosts()) {
                this.bestSol = this.currSol;
            }
        }
        return this.bestSol;
    }

    public Solution solve(Instance instance) {

        boolean firstFit = false;
        boolean onlyValid = true;

        Solution res = this.solveIterations(instance, firstFit, onlyValid);
        System.out.println("val: " + res.getObjectiveValue());

        return res;
    }
}


