package SP.post_optimization_methods;

import SP.representations.Solution;
import SP.representations.StorageAreaPosition;
import SP.util.HeuristicUtil;

import java.util.ArrayList;

/**
 * Improvement heuristic that starts with an initial solution of a stacking-problem
 * and tries to improve its quality in terms of cost minimization.
 *
 * @author Tim Bohne
 */
public class TabuSearch {

    private final int NUMBER_OF_ITERATIONS = 1000;
    private final int NUMBER_OF_TABU_LIST_CLEARS = 100;

    private Solution currSol;
    private Solution bestSol;

    private ArrayList<Exchange> tabuList;
    private int tabuListClears;

    /**
     * Constructor
     *
     * @param initialSolution - the solution to be improved in the tabu search
     */
    public TabuSearch(Solution initialSolution) {
        this.currSol = new Solution(initialSolution);
        this.bestSol = new Solution(initialSolution);
        this.tabuList = new ArrayList<>();
        this.tabuListClears = 0;
    }

    /**
     * Clears the entries in the tabu list and increments the clear counter.
     */
    public void clearTabuList() {
        this.tabuList = new ArrayList<>();
        this.tabuListClears++;
    }

    /**
     * Returns a random position in the storage area.
     *
     * @param sol - the solution the storage area is based on
     * @return random position in the storage area
     */
    public StorageAreaPosition getRandomPositionInStorageArea(Solution sol) {
        int stackIdx = HeuristicUtil.getRandomIntegerInBetween(0, sol.getFilledStorageArea().length - 1);
        int level = HeuristicUtil.getRandomIntegerInBetween(0, sol.getFilledStorageArea()[stackIdx].length - 1);
        return new StorageAreaPosition(stackIdx, level);
    }

    /**
     * Exchanges the items in the specified positions in the storage area of the given solution.
     * If only one position is occupied with an item, the item simply gets moved to the other position.
     *
     * @param sol    - the solution to be altered
     * @param posOne - the first position of the exchange
     * @param posTwo - the second position of the exchange
     */
    public void exchangeItems(Solution sol, StorageAreaPosition posOne, StorageAreaPosition posTwo) {
        int itemOne = sol.getFilledStorageArea()[posOne.getStackIdx()][posOne.getLevel()];
        int itemTwo = sol.getFilledStorageArea()[posTwo.getStackIdx()][posTwo.getLevel()];
        sol.getFilledStorageArea()[posOne.getStackIdx()][posOne.getLevel()] = itemTwo;
        sol.getFilledStorageArea()[posTwo.getStackIdx()][posTwo.getLevel()] = itemOne;
    }

    /**
     * Generates a neighbor for the current solution.
     *
     * @param firstFit     - determines whether the first improving neighbor gets returned (best-fit otherwise)
     * @param onlyFeasible - determines whether only feasible neighbors are considered
     * @return a neighboring solution
     */
    public Solution getNeighbor(boolean firstFit, boolean onlyFeasible) {

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

    /**
     * Performs the tabu search with a number of iterations as stop criterion.
     *
     * @param firstFit     - determines the strategy of the neighbor retrieval (true --> first-fit, false --> best-fit)
     * @param onlyFeasible - determines whether only feasible neighboring solutions are considered during the search
     */
    public void solveIterations(boolean firstFit, boolean onlyFeasible) {
        for (int i = 0; i < this.NUMBER_OF_ITERATIONS; i++) {
            this.currSol = getNeighbor(firstFit, onlyFeasible);
            if (this.currSol.computeCosts() < this.bestSol.computeCosts()) {
                this.bestSol = this.currSol;
            }
        }
    }

    /**
     * Performs the tabu search.
     *
     * @param iterationsCrit - determines the stop criterion (true --> iterations, false --> number of TL clears)
     * @param firstFit       - determines the strategy of the neighbor retrieval (true --> first-fit, false --> best-fit)
     * @param onlyFeasible   - determines whether only feasible neighboring solutions are considered during the search
     * @return the best solution generated in the tabu search procedure
     */
    public Solution solve(boolean iterationsCrit, boolean firstFit, boolean onlyFeasible) {
        if (iterationsCrit) {
            this.solveIterations(firstFit, onlyFeasible);
        }
        return this.bestSol;
    }
}


