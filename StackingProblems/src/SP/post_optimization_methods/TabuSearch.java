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

        while (nbrs.size() <= this.instance.getItems().length) {

            Solution neighbor = new Solution(this.currSol);

            int stackIdxItemOne = HeuristicUtil.getRandomIntegerInBetween(0, neighbor.getFilledStorageArea().length);
            int stackIdxItemTwo = HeuristicUtil.getRandomIntegerInBetween(0, neighbor.getFilledStorageArea().length);
            int levelItemOne = HeuristicUtil.getRandomIntegerInBetween(0, neighbor.getFilledStorageArea()[stackIdxItemOne].length);
            int levelItemTwo = HeuristicUtil.getRandomIntegerInBetween(0, neighbor.getFilledStorageArea()[stackIdxItemTwo].length);

            StorageAreaPosition posOne = new StorageAreaPosition(stackIdxItemOne, levelItemOne);
            StorageAreaPosition posTwo = new StorageAreaPosition(stackIdxItemTwo, levelItemTwo);

            // Exchanges the positions of two items from the storage area.
            int itemOne = neighbor.getFilledStorageArea()[stackIdxItemOne][levelItemOne];
            int itemTwo = neighbor.getFilledStorageArea()[stackIdxItemTwo][levelItemTwo];
            neighbor.getFilledStorageArea()[stackIdxItemOne][levelItemOne] = itemTwo;
            neighbor.getFilledStorageArea()[stackIdxItemTwo][levelItemTwo] = itemOne;

            Exchange exchange = new Exchange(posOne, posTwo);

            // Only feasible solutions are considered for now.
            if (/*neighbor.isFeasible() &&*/ !this.tabuListContainsExchange(exchange)) {
                nbrs.add(neighbor);
                this.tabuList.add(exchange);
            }
        }

        System.out.println("size: " + nbrs.size());

        return nbrs.get(0);
    }

    public boolean tabuListContainsExchange(Exchange exchange) {

        for (Exchange e : this.tabuList) {
            if (e.getPosOne().getStackIdx() == exchange.getPosOne().getStackIdx()
                && e.getPosOne().getLevel() == exchange.getPosOne().getLevel()
                && e.getPosTwo().getStackIdx() == exchange.getPosTwo().getStackIdx()
                && exchange.getPosTwo().getLevel() == exchange.getPosTwo().getLevel()) {

                    return true;
            } else if (e.getPosOne().getStackIdx() == exchange.getPosTwo().getStackIdx()
                    && e.getPosOne().getLevel() == exchange.getPosTwo().getLevel()
                    && e.getPosTwo().getStackIdx() == exchange.getPosOne().getStackIdx()
                    && e.getPosTwo().getLevel() == exchange.getPosOne().getLevel()) {
                        return true;
            }
        }

        return false;
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


