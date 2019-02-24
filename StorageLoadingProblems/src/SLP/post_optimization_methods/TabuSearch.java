package SLP.post_optimization_methods;

import SLP.representations.Instance;
import SLP.representations.Solution;
import SLP.util.HeuristicUtil;

import java.util.ArrayList;

public class TabuSearch {

    private Instance instance;
    private Solution currSol;
    private Solution bestSol;

    private ArrayList<Integer> tabuList;
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

    public Solution getNeighbor(boolean firstFir, boolean onlyValid) {

        ArrayList<Solution> nbrs = new ArrayList<>();

        while (nbrs.size() <= /*this.instance.getItems().length*/ 50) {

            Solution neighbor = new Solution(this.currSol);

            int minOuter = 0;
            int maxOuter = neighbor.getFilledStorageArea().length;

            int outerOne = HeuristicUtil.getRandomValueInBetween(minOuter, maxOuter);
            int outerTwo = HeuristicUtil.getRandomValueInBetween(minOuter, maxOuter);

            int minInner = 0;
            int maxInnerOne = neighbor.getFilledStorageArea()[outerOne].length;
            int maxInnerTwo = neighbor.getFilledStorageArea()[outerTwo].length;

            int innerOne = HeuristicUtil.getRandomValueInBetween(minInner, maxInnerOne);
            int innerTwo = HeuristicUtil.getRandomValueInBetween(minInner, maxInnerTwo);

            System.out.println(neighbor.getNumberOfAssignedItems());

            int itemOne = neighbor.getFilledStorageArea()[outerOne][innerOne];
            int itemTwo = neighbor.getFilledStorageArea()[outerTwo][innerTwo];
            neighbor.getFilledStorageArea()[outerOne][innerOne] = itemTwo;
            neighbor.getFilledStorageArea()[outerTwo][innerTwo] = itemOne;

            System.out.println(neighbor.getNumberOfAssignedItems());

            // only feasible solutions are considered for now
            if (neighbor.isFeasible()) {
                nbrs.add(neighbor);
            }
            System.out.println(nbrs.size());
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


