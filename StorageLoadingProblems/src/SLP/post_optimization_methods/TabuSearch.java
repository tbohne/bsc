package SLP.post_optimization_methods;

import SLP.representations.Instance;
import SLP.representations.Solution;
import SLP.util.HeuristicUtil;

import java.util.ArrayList;

public class TabuSearch {

    private Instance instance;
    private Solution currSol;
    private Solution bestSol;

    private ArrayList<TabuListEntry> tabuList;
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

        while (nbrs.size() <= /*this.instance.getItems().length*/ 0) {

            Solution neighbor = new Solution(this.currSol);

            int stackIdxItemOne = HeuristicUtil.getRandomValueInBetween(0, neighbor.getFilledStorageArea().length);
            int stackIdxItemTwo = HeuristicUtil.getRandomValueInBetween(0, neighbor.getFilledStorageArea().length);
            int levelItemOne = HeuristicUtil.getRandomValueInBetween(0, neighbor.getFilledStorageArea()[stackIdxItemOne].length);
            int levelItemTwo = HeuristicUtil.getRandomValueInBetween(0, neighbor.getFilledStorageArea()[stackIdxItemTwo].length);

            // Exchanges the positions of two items from the storage area.
            int itemOne = neighbor.getFilledStorageArea()[stackIdxItemOne][levelItemOne];
            int itemTwo = neighbor.getFilledStorageArea()[stackIdxItemTwo][levelItemTwo];
            neighbor.getFilledStorageArea()[stackIdxItemOne][levelItemOne] = itemTwo;
            neighbor.getFilledStorageArea()[stackIdxItemTwo][levelItemTwo] = itemOne;

            // Only feasible solutions are considered for now.
            if (neighbor.isFeasible()) {
                nbrs.add(neighbor);
            }
        }

        return nbrs.get(0);
    }

    public Solution solveIterations(Instance instance, boolean firstFit, boolean onlyValid) {
        for (int i = 0; i < 100; i++) {
            this.currSol = getNeighbor(firstFit, onlyValid);
            if (this.currSol.getObjectiveValueAsDouble() < this.bestSol.getObjectiveValueAsDouble()) {
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


