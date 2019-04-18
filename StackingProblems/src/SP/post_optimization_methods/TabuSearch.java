package SP.post_optimization_methods;

import SP.representations.Solution;
import SP.representations.StorageAreaPosition;
import SP.util.HeuristicUtil;

import java.util.ArrayList;
import java.util.Random;

/**
 * Improvement heuristic that starts with an initial solution of a stacking-problem
 * and tries to improve its quality in terms of cost minimization.
 *
 * @author Tim Bohne
 */
public class TabuSearch {

    // CONFIG
    private final int NUMBER_OF_ITERATIONS = 1000;
    private final int NUMBER_OF_TABU_LIST_CLEARS = 10;
    private final int FAIL_COUNT_BEFORE_CLEAR = 20;
    private final int NUMBER_OF_GENERATED_NEIGHBORS = 100;

    private Solution currSol;
    private Solution bestSol;

    private ArrayList<Exchange> swapTabuList;
    private ArrayList<Integer> shiftTabuList;
    private int tabuListClears;

    /**
     * Constructor
     *
     * @param initialSolution - the solution to be improved in the tabu search
     */
    public TabuSearch(Solution initialSolution) {
        this.currSol = new Solution(initialSolution);
        this.bestSol = new Solution(initialSolution);
        this.swapTabuList = new ArrayList<>();
        this.shiftTabuList = new ArrayList<>();
        this.tabuListClears = 0;
    }

    /**
     * Clears the entries in the swap tabu list and increments the clear counter.
     */
    public void clearSwapTabuList() {
        System.out.println("clearing swap tabu list...");
        this.swapTabuList = new ArrayList<>();
        this.tabuListClears++;
    }

    /**
     * Clears the entries in the shift tabu list and increments the clear counter.
     */
    public void clearShiftTabuList() {
        System.out.println("clearing shift tabu list...");
        this.shiftTabuList = new ArrayList<>();
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
     * Retrieves the free slots in the storage area.
     *
     * @param sol - the solution retrieve the free slots for
     * @return list of free slots in the storage area
     */
    public ArrayList<StorageAreaPosition> getFreeSlots(Solution sol) {
        ArrayList<StorageAreaPosition> freeSlots = new ArrayList<>();
        for (int stack = 0; stack < sol.getFilledStorageArea().length; stack++) {
            for (int level = 0; level < sol.getFilledStorageArea()[stack].length; level++) {
                if (sol.getFilledStorageArea()[stack][level] == -1) {
                    freeSlots.add(new StorageAreaPosition(stack, level));
                }
            }
        }
        return freeSlots;
    }

    /**
     * Returns a random free slot in the storage area of the specified solution.
     *
     * @param sol - the specified solution to return a free slot for
     * @return a random free slot in the storage area
     */
    public StorageAreaPosition getRandomFreeSlot(Solution sol) {
        ArrayList<StorageAreaPosition> freeSlots = this.getFreeSlots(sol);
        int freeSlotIdx = HeuristicUtil.getRandomIntegerInBetween(0, freeSlots.size() - 1);
        return freeSlots.get(freeSlotIdx);
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
     * Shifts the item stored in pos to the shift target.
     *
     * @param sol         - the solution to be updated
     * @param shiftTarget - the position the item is shifted to
     * @param pos         - the item's original position
     */
    public void shiftItem(Solution sol, StorageAreaPosition pos, StorageAreaPosition shiftTarget) {
        sol.getFilledStorageArea()[shiftTarget.getStackIdx()][shiftTarget.getLevel()] =
            sol.getFilledStorageArea()[pos.getStackIdx()][pos.getLevel()];
        sol.getFilledStorageArea()[pos.getStackIdx()][pos.getLevel()] = -1;
    }

    /**
     * Generates a neighbor for the current solution using the "shift-neighborhood".
     *
     * @param firstFit     - determines whether the first improving neighbor gets returned (best-fit otherwise)
     * @param onlyFeasible - determines whether only feasible neighbors are considered
     * @return a neighboring solution
     */
    public Solution getNeighborShift(boolean firstFit, boolean onlyFeasible) {
        ArrayList<Solution> nbrs = new ArrayList<>();

        int failCnt = 0;

        while (nbrs.size() <= this.NUMBER_OF_GENERATED_NEIGHBORS) {
            Solution neighbor = new Solution(this.currSol);
            StorageAreaPosition pos = this.getRandomPositionInStorageArea(neighbor);
            int item = neighbor.getFilledStorageArea()[pos.getStackIdx()][pos.getLevel()];
            if (item == -1) { continue; }
            StorageAreaPosition shiftTarget = this.getRandomFreeSlot(neighbor);
            this.shiftItem(neighbor, pos, shiftTarget);

            if (onlyFeasible) {
                if (!neighbor.isFeasible()) { continue; }

                // FIRST-FIT
                if (firstFit && !this.shiftTabuList.contains(item) && neighbor.computeCosts() < this.currSol.computeCosts()) {
                    this.shiftTabuList.add(item);
                    return neighbor;

                    // BEST-FIT
                } else if (!this.shiftTabuList.contains(item)) {
                    nbrs.add(neighbor);
                    this.shiftTabuList.add(item);

                } else {
                    failCnt++;
                    if (failCnt == this.FAIL_COUNT_BEFORE_CLEAR) {
                        this.clearShiftTabuList();
                        failCnt = 0;
                    }
                }

                // ASPIRATION CRITERION
                if (neighbor.computeCosts() > this.bestSol.computeCosts()) {
                    if (firstFit) {
                        return neighbor;
                    } else {
                        nbrs.add(neighbor);
                    }
                }
            } else {

                // TODO: implement infeasible part
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }

    /**
     * Generates a neighbor for the current solution using the "swap-neighborhood".
     *
     * @param firstFit     - determines whether the first improving neighbor gets returned (best-fit otherwise)
     * @param onlyFeasible - determines whether only feasible neighbors are considered
     * @return a neighboring solution
     */
    public Solution getNeighborSwap(boolean firstFit, boolean onlyFeasible) {

        ArrayList<Solution> nbrs = new ArrayList<>();

        int failCnt = 0;

        while (nbrs.size() <= this.NUMBER_OF_GENERATED_NEIGHBORS) {

            Solution neighbor = new Solution(this.currSol);
            StorageAreaPosition posOne = this.getRandomPositionInStorageArea(neighbor);
            StorageAreaPosition posTwo = this.getRandomPositionInStorageArea(neighbor);
            this.exchangeItems(neighbor, posOne, posTwo);
            Exchange exchange = new Exchange(posOne, posTwo);

            if (onlyFeasible) {
                if (!neighbor.isFeasible()) { continue; }

                // FIRST-FIT
                if (firstFit && !this.swapTabuList.contains(exchange) && neighbor.computeCosts() < this.currSol.computeCosts()) {
                    this.swapTabuList.add(exchange);
                    return neighbor;

                // BEST-FIT
                } else if (!this.swapTabuList.contains(exchange)) {
                    nbrs.add(neighbor);
                    this.swapTabuList.add(exchange);

                } else {
                    failCnt++;
                    if (failCnt == this.FAIL_COUNT_BEFORE_CLEAR) {
                        this.clearSwapTabuList();
                        failCnt = 0;
                    }
                }

                // ASPIRATION CRITERION
                if (neighbor.computeCosts() > this.bestSol.computeCosts()) {
                    if (firstFit) {
                        return neighbor;
                    } else {
                        nbrs.add(neighbor);
                    }
                }

            } else {

                // TODO: implement infeasible part
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
            System.out.println(i);
            Random r = new Random();
            if (r.nextBoolean()) {
                this.currSol = getNeighborShift(firstFit, onlyFeasible);
            } else {
                this.currSol = getNeighborSwap(firstFit, onlyFeasible);
            }
            if (this.currSol.computeCosts() < this.bestSol.computeCosts()) {
                this.bestSol = this.currSol;
            }
        }
    }

    /**
     * Performs the tabu search with a number of tabu list clears as stop criterion.
     *
     * @param firstFit     - determines the strategy of the neighbor retrieval (true --> first-fit, false --> best-fit)
     * @param onlyFeasible - determines whether only feasible neighboring solutions are considered during the search
     */
    public void solveTabuListClears(boolean firstFit, boolean onlyFeasible) {
        while (this.tabuListClears < this.NUMBER_OF_TABU_LIST_CLEARS) {
            this.currSol = getNeighborSwap(firstFit, onlyFeasible);
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
        } else {
            this.solveTabuListClears(firstFit, onlyFeasible);
        }
        return this.bestSol;
    }
}


