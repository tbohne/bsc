package SP.post_optimization_methods;

import SP.representations.Solution;
import SP.representations.StorageAreaPosition;
import SP.util.HeuristicUtil;

import java.util.*;

/**
 * Improvement heuristic that starts with an initial solution of a stacking-problem
 * and tries to improve its quality in terms of cost minimization by performing a tabu-search.
 *
 * @author Tim Bohne
 */
public class TabuSearch {

    private Solution currSol;
    private Solution bestSol;

    private Queue<Swap> swapTabuList;
    private Queue<Shift> shiftTabuList;
    private int tabuListClears;
    private int maxTabuListLength;

    private int shiftFailCnt;
    private int swapFailCnt;

    private int iterationOfLastImprovement;

    /**
     * Constructor
     *
     * @param initialSolution - the solution to be improved in the tabu search
     */
    public TabuSearch(Solution initialSolution) {
        this.currSol = new Solution(initialSolution);
        this.bestSol = new Solution(initialSolution);
        this.swapTabuList = new LinkedList<>();
        this.shiftTabuList = new LinkedList<>();
        this.tabuListClears = 0;
        this.shiftFailCnt = 0;
        this.swapFailCnt = 0;
        this.iterationOfLastImprovement = 0;
        this.maxTabuListLength = TabuSearchConfig.MAX_TABU_LIST_LENGTH_FACTOR * initialSolution.getNumberOfAssignedItems();
    }

    /**
     * Clears the entries in the swap tabu list and increments the clear counter.
     */
    public void clearSwapTabuList() {
        System.out.println("clearing swap tabu list...");
        this.swapTabuList = new LinkedList<>();
        this.tabuListClears++;
    }

    /**
     * Clears the entries in the shift tabu list and increments the clear counter.
     */
    public void clearShiftTabuList() {
        System.out.println("clearing shift tabu list...");
        this.shiftTabuList = new LinkedList<>();
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
    public Swap swapItems(Solution sol, StorageAreaPosition posOne, StorageAreaPosition posTwo) {
        int itemOne = sol.getFilledStorageArea()[posOne.getStackIdx()][posOne.getLevel()];
        int itemTwo = sol.getFilledStorageArea()[posTwo.getStackIdx()][posTwo.getLevel()];
        sol.getFilledStorageArea()[posOne.getStackIdx()][posOne.getLevel()] = itemTwo;
        sol.getFilledStorageArea()[posTwo.getStackIdx()][posTwo.getLevel()] = itemOne;
        return new Swap(posOne, posTwo, itemOne, itemTwo);
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
     * Adds the specified swap operation to the swap tabu list.
     * Replaces the oldest entry if the maximum length of the tabu list is reached.
     *
     * @param swap - the swap operation to be added to the tabu list
     */
    public void forbidSwap(Swap swap) {
        if (this.swapTabuList.size() >= this.maxTabuListLength) {
            this.swapTabuList.poll();
        }
        this.swapTabuList.add(swap);
    }

    /**
     * Adds the specified shift operation to the shift tabu list.
     * Replaces the oldest entry if the maximum length of the tabu list is reached.
     *
     * @param shift - the shift operation to be added to the tabu list
     */
    public void forbidShift(Shift shift) {
        if (this.shiftTabuList.size() >= this.maxTabuListLength) {
            this.shiftTabuList.poll();
        }
        this.shiftTabuList.add(shift);
    }

    public void shiftFailure() {
        this.shiftFailCnt++;
        if (this.shiftFailCnt == TabuSearchConfig.UNSUCCESSFUL_SHIFT_ATTEMPTS) {
            this.clearShiftTabuList();
            this.shiftFailCnt = 0;
        }
    }

    /**
     * Generates a neighbor for the current solution using the "shift-neighborhood".
     *
     * @param shortTermStrategy - determines the strategy for the neighbor retrieval
     * @param iteration         - the current iteration in the tabu search
     * @return a neighboring solution
     */
    public Solution getNeighborShift(TabuSearchConfig.ShortTermStrategies shortTermStrategy, int iteration) {
        ArrayList<Solution> nbrs = new ArrayList<>();

        while (nbrs.size() < TabuSearchConfig.NUMBER_OF_NEIGHBORS) {

            Solution neighbor = new Solution(this.currSol);
            StorageAreaPosition pos = this.getRandomPositionInStorageArea(neighbor);
            int item = neighbor.getFilledStorageArea()[pos.getStackIdx()][pos.getLevel()];
            if (item == -1) { continue; }
            StorageAreaPosition shiftTarget = this.getRandomFreeSlot(neighbor);
            this.shiftItem(neighbor, pos, shiftTarget);
            Shift shift = new Shift(item, shiftTarget);

            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (shortTermStrategy == TabuSearchConfig.ShortTermStrategies.FIRST_FIT
                && !this.shiftTabuList.contains(shift)
                && neighbor.computeCosts() < this.currSol.computeCosts()) {
                    this.forbidShift(shift);
                    return neighbor;
            // BEST-FIT
            } else if (!this.shiftTabuList.contains(shift)) {
                nbrs.add(neighbor);
                this.forbidShift(shift);
            } else {
                this.shiftFailure();
            }

            // ASPIRATION CRITERION
            if (neighbor.computeCosts() < this.bestSol.computeCosts()) {
                if (shortTermStrategy == TabuSearchConfig.ShortTermStrategies.FIRST_FIT) {
                    return neighbor;
                } else {
                    nbrs.add(neighbor);
                }
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }

    /**
     * Generates a neighbor for the current solution using the "swap-neighborhood".
     *
     * @param shortTermStrategy - determines the strategy for the neighbor retrieval
     * @param onlyFeasible      - determines whether only feasible neighbors are considered
     * @param iteration         - the current iteration in the tabu search
     * @return a neighboring solution
     */
    public Solution getNeighborSwap(TabuSearchConfig.ShortTermStrategies shortTermStrategy, boolean onlyFeasible, int iteration) {

        ArrayList<Solution> nbrs = new ArrayList<>();

        int failCnt = 0;

        while (nbrs.size() <= TabuSearchConfig.NUMBER_OF_NEIGHBORS) {

            Solution neighbor = new Solution(this.currSol);
            StorageAreaPosition posOne = this.getRandomPositionInStorageArea(neighbor);
            StorageAreaPosition posTwo = this.getRandomPositionInStorageArea(neighbor);
            Swap swap = this.swapItems(neighbor, posOne, posTwo);

            if (onlyFeasible) {
                if (!neighbor.isFeasible()) { continue; }

                // FIRST-FIT
                if (shortTermStrategy == TabuSearchConfig.ShortTermStrategies.FIRST_FIT && !this.swapTabuList.contains(swap) && neighbor.computeCosts() < this.currSol.computeCosts()) {
                    this.forbidSwap(swap);
                    return neighbor;

                // BEST-FIT
                } else if (!this.swapTabuList.contains(swap)) {
                    nbrs.add(neighbor);
                    this.forbidSwap(swap);

                } else {
                    failCnt++;
                    if (failCnt == TabuSearchConfig.UNSUCCESSFUL_SWAP_ATTEMPTS) {
                        this.clearSwapTabuList();
                        failCnt = 0;
                    }
                }

                // ASPIRATION CRITERION
                if (neighbor.computeCosts() < this.bestSol.computeCosts()) {
                    if (shortTermStrategy == TabuSearchConfig.ShortTermStrategies.FIRST_FIT) {
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

    public Solution getNeighborDoubleSwap(TabuSearchConfig.ShortTermStrategies shortTermStrategy, boolean onlyFeasible, int iteration) {

        ArrayList<Solution> nbrs = new ArrayList<>();
        int failCnt = 0;

        while (nbrs.size() <= TabuSearchConfig.NUMBER_OF_NEIGHBORS) {
            Solution neighbor = new Solution(this.currSol);
            StorageAreaPosition posOne = this.getRandomPositionInStorageArea(neighbor);
            StorageAreaPosition posTwo = this.getRandomPositionInStorageArea(neighbor);
            Swap swapOne = this.swapItems(neighbor, posOne, posTwo);
            StorageAreaPosition posOneS = new StorageAreaPosition(
                posOne.getStackIdx(),
                HeuristicUtil.getRandomIntegerInBetweenWithException(0, neighbor.getFilledStorageArea()[0].length - 1, posOne.getLevel())
            );
            StorageAreaPosition posTwoS = new StorageAreaPosition(
                posTwo.getStackIdx(),
                HeuristicUtil.getRandomIntegerInBetweenWithException(0, neighbor.getFilledStorageArea()[0].length - 1, posTwo.getLevel())
            );
            Swap swapTwo = this.swapItems(neighbor, posOneS, posTwoS);

            if (onlyFeasible) {
                if (!neighbor.isFeasible()) { continue; }

                // FIRST-FIT
                if (shortTermStrategy == TabuSearchConfig.ShortTermStrategies.FIRST_FIT
                    && !this.swapTabuList.contains(swapOne)
                    && !this.swapTabuList.contains(swapTwo)
                    && neighbor.computeCosts() < this.currSol.computeCosts()) {

                    this.forbidSwap(swapOne);
                    this.forbidSwap(swapTwo);

                    return neighbor;

                // BEST-FIT
                } else if (!this.swapTabuList.contains(swapOne) && !this.swapTabuList.contains(swapTwo)) {
                    nbrs.add(neighbor);
                    this.forbidSwap(swapOne);
                    this.forbidSwap(swapTwo);

                } else {
                    failCnt++;
                    if (failCnt == TabuSearchConfig.UNSUCCESSFUL_SWAP_ATTEMPTS) {
                        this.clearSwapTabuList();
                        failCnt = 0;
                    }
                }

                // ASPIRATION CRITERION
                if (neighbor.computeCosts() < this.bestSol.computeCosts()) {
                    if (shortTermStrategy == TabuSearchConfig.ShortTermStrategies.FIRST_FIT) {
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

    public Solution getNeighborRowSwap(TabuSearchConfig.ShortTermStrategies shortTermStrategy, boolean onlyFeasible, int iteration) {

        ArrayList<Solution> nbrs = new ArrayList<>();
        int failCnt = 0;

        while (nbrs.size() <= TabuSearchConfig.NUMBER_OF_NEIGHBORS * 0.1) {

            Solution neighbor = new Solution(this.currSol);
            StorageAreaPosition posOne = this.getRandomPositionInStorageArea(neighbor);
            int swapItem = this.currSol.getFilledStorageArea()[posOne.getStackIdx()][posOne.getLevel()];
            while (swapItem == -1) {
                posOne = this.getRandomPositionInStorageArea(neighbor);
                swapItem = this.currSol.getFilledStorageArea()[posOne.getStackIdx()][posOne.getLevel()];
            }

            // max number of swaps in a row is m-1
            int numberOfSwaps = HeuristicUtil.getRandomIntegerInBetween(1, this.currSol.getFilledStorageArea().length - 1);

            ArrayList<Swap> swapList = new ArrayList<>();

            ArrayList<StorageAreaPosition> positionsTheSwapItemHasBeenAt = new ArrayList<>();
            positionsTheSwapItemHasBeenAt.add(posOne);

            for (int swapCnt = 0; swapCnt < numberOfSwaps; swapCnt++) {
                StorageAreaPosition posTwo = this.getRandomPositionInStorageArea(neighbor);
                posTwo.setLevel(posOne.getLevel());
                while (positionsTheSwapItemHasBeenAt.contains(posTwo)) {
                    posTwo = this.getRandomPositionInStorageArea(neighbor);
                    posTwo.setLevel(posOne.getLevel());
                }
                Swap swap = this.swapItems(neighbor, posOne, posTwo);
                swapList.add(swap);
                positionsTheSwapItemHasBeenAt.add(posTwo);
                posOne = posTwo;
            }

            if (onlyFeasible) {
                if (!neighbor.isFeasible()) { continue; }

                boolean containsTabuSwap = false;
                for (Swap swap : swapList) {
                    if (this.swapTabuList.contains(swap)) {
                        containsTabuSwap = true;
                    }
                }

                // FIRST-FIT
                if (shortTermStrategy == TabuSearchConfig.ShortTermStrategies.FIRST_FIT
                        && !containsTabuSwap
                        && neighbor.computeCosts() < this.currSol.computeCosts()) {


                    for (Swap swap : swapList) {
                        this.forbidSwap(swap);
                    }

                    return neighbor;

                // BEST-FIT
                } else if (!containsTabuSwap) {
                    nbrs.add(neighbor);

                    for (Swap swap : swapList) {
                        this.forbidSwap(swap);
                    }

                } else {
                    failCnt++;
                    if (failCnt == TabuSearchConfig.UNSUCCESSFUL_SWAP_ATTEMPTS) {
                        this.clearSwapTabuList();
                        failCnt = 0;
                    }
                }

                // ASPIRATION CRITERION
                if (neighbor.computeCosts() < this.bestSol.computeCosts()) {
                    if (shortTermStrategy == TabuSearchConfig.ShortTermStrategies.FIRST_FIT) {
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
     * Applies the combined shift- and swap-neighborhood to retrieve the best neighbor.
     *
     * @param shortTermStrategy - determines the strategy for the neighbor retrieval
     * @param onlyFeasible      - determines whether only feasible neighboring solutions are considered during the search
     * @param iteration         - the current iteration
     * @return best generated neighbor
     */
    public Solution getNeighbor(TabuSearchConfig.ShortTermStrategies shortTermStrategy, boolean onlyFeasible, int iteration) {

        // TODO: find reasonable chances for operator applications

//        double rand = Math.random();

//        if (rand < 0.05) {
//            System.out.println("row swap");
//            return this.getNeighborRowSwap(shortTermStrategy, onlyFeasible, iteration);
//        } else if (rand < 0.5) {
//            System.out.println("swap");
//            return this.getNeighborSwap(shortTermStrategy, onlyFeasible, iteration);
//        } else {
//            System.out.println("shift");
//            return this.getNeighborShift(shortTermStrategy, onlyFeasible, iteration);
//        }

        Solution shiftNeighbor = new Solution(this.currSol);
        // shift is only possible if there are free slots
        if (this.currSol.getNumberOfAssignedItems() < this.currSol.getFilledStorageArea().length * this.currSol.getFilledStorageArea()[0].length) {
            shiftNeighbor = this.getNeighborShift(shortTermStrategy, iteration);
        }

        Solution swapNeighbor = this.getNeighborSwap(shortTermStrategy, onlyFeasible, iteration);
        Solution rowSwapNeighbor = this.getNeighborRowSwap(shortTermStrategy, onlyFeasible, iteration);


        // if b=3 --> perform double-swap additionally
        if (swapNeighbor.getFilledStorageArea()[0].length == 3) {
            Solution doubleSwapNeighbor = this.getNeighborDoubleSwap(shortTermStrategy, onlyFeasible, iteration);
            if (doubleSwapNeighbor.computeCosts() < swapNeighbor.computeCosts()
                && doubleSwapNeighbor.computeCosts() < shiftNeighbor.computeCosts()
                && doubleSwapNeighbor.computeCosts() < rowSwapNeighbor.computeCosts()) {
                    return doubleSwapNeighbor;
            }
        }

        double bestCost = shiftNeighbor.computeCosts();
        Solution bestNbr = shiftNeighbor;

        double swapCosts = swapNeighbor.computeCosts();
        double shiftCosts = shiftNeighbor.computeCosts();
        double rowSwapCosts = rowSwapNeighbor.computeCosts();

        if (rowSwapNeighbor.computeCosts() < bestCost) {
            bestCost = rowSwapNeighbor.computeCosts();
            bestNbr = rowSwapNeighbor;
        }

        if (swapNeighbor.computeCosts() < bestCost) {
            bestCost = swapNeighbor.computeCosts();
            bestNbr = swapNeighbor;
        }

        if (bestCost == rowSwapCosts) {
            System.out.println("ROW SWAP");
        } else if (bestCost == shiftCosts) {
            System.out.println("SHIFT");
        } else if (bestCost == swapCosts) {
            System.out.println("SWAP");
        }

        return bestNbr;

//        return shiftNeighbor.computeCosts() < swapNeighbor.computeCosts() ? shiftNeighbor : swapNeighbor;
    }

    /**
     * Updates the current solution with the best neighbor.
     * Additionally, the best solution gets updated if a new best solution is found.
     *
     * @param shortTermStrategy - determines the strategy for the neighbor retrieval
     * @param onlyFeasible      - determines whether only feasible neighboring solutions are considered during the search
     * @param iteration         - the current iteration
     */
    public void updateCurrentSolution(TabuSearchConfig.ShortTermStrategies shortTermStrategy, boolean onlyFeasible, int iteration) {
        this.currSol = this.getNeighbor(shortTermStrategy, onlyFeasible, iteration);
        if (this.currSol.computeCosts() < this.bestSol.computeCosts()) {
            this.bestSol = this.currSol;
            this.iterationOfLastImprovement = iteration;
        }
    }

    /**
     * Performs the tabu search with a number of iterations as stop criterion.
     */
    public void solveIterations() {
        for (int i = 0; i < TabuSearchConfig.NUMBER_OF_ITERATIONS; i++) {
            this.updateCurrentSolution(TabuSearchConfig.SHORT_TERM_STRATEGY, TabuSearchConfig.FEASIBLE_ONLY, i);
        }
    }

    /**
     * Performs the tabu search with a number of tabu list clears as stop criterion.
     */
    public void solveTabuListClears() {
        int iteration = 0;
        while (this.tabuListClears < TabuSearchConfig.NUMBER_OF_TABU_LIST_CLEARS) {
            this.updateCurrentSolution(TabuSearchConfig.SHORT_TERM_STRATEGY, TabuSearchConfig.FEASIBLE_ONLY, iteration++);
        }
    }

    /**
     * Performs the tabu search with a number of non-improving iterations as stop criterion.
     */
    public void solveIterationsSinceLastImprovement() {
        int iteration = 0;
        while (Math.abs(this.iterationOfLastImprovement - iteration) < TabuSearchConfig.NUMBER_OF_NON_IMPROVING_ITERATIONS) {
            System.out.println(this.bestSol.computeCosts());
            System.out.println("diff: " + Math.abs(this.iterationOfLastImprovement - iteration));
            this.updateCurrentSolution(TabuSearchConfig.SHORT_TERM_STRATEGY, TabuSearchConfig.FEASIBLE_ONLY, iteration++);
        }
    }

    /**
     * Improves a given solution to a stacking problem using a tabu search.
     *
     * @return the best solution generated in the tabu search procedure
     */
    public Solution solve() {

        switch (TabuSearchConfig.STOPPING_CRITERION) {
            case ITERATIONS:
                this.solveIterations();
                break;
            case TABU_LIST_CLEARS:
                this.solveTabuListClears();
                break;
            case NON_IMPROVING_ITERATIONS:
                this.solveIterationsSinceLastImprovement();
                break;
            default:
                this.solveIterationsSinceLastImprovement();
        }
        return this.bestSol;
    }
}


