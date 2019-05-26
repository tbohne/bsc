package SP.post_optimization_methods;

import SP.experiments.PostOptimization;
import SP.representations.Solution;
import SP.representations.StackPosition;
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
    private double timeLimit;
    private double startTime;
    private double optimalObjectiveValue;

    private PostOptimization.ShortTermStrategies shortTermStrategy;
    private PostOptimization.StoppingCriteria stoppingCriterion;
    private int numberOfNeighbors;

    private Queue<Shift> tabuList;
    private int tabuListClears;
    private int maxTabuListLengthFactor;
    private int maxTabuListLength;
    private int failCnt;
    private int unsuccessfulNeighborGenerationAttempts;
    private int unsuccessfulKSwapAttempts;
    private int kSwapIntervalUB;

    private int iterationOfLastImprovement;
    private int numberOfNonImprovingIterations;
    private int numberOfIterations;
    private int numberOfTabuListClears;

    /**
     * Constructor
     *
     * @param initialSolution                        - initial solution to be improved
     * @param timeLimit                              - time limit for the improvement procedure
     * @param optimalObjectiveValue                  - optimal objective value for the solution (generated by CPLEX)
     * @param numberOfNeighbors                      - number of neighbors considered in short-term strategy
     * @param maxTabuListLengthFactor                - factor used to compute max length of tabu list
     * @param shortTermStrategy                      - strategy for neighborhood exploration
     * @param unsuccessfulNeighborGenerationAttempts - number of unsuccessful neighbor generation attempts before TL reset
     * @param unsuccessfulKSwapAttempts              - number of unsuccessful k swap attempts before skip
     * @param numberOfNonImprovingIterations         - number of non improving iterations before termination
     * @param kSwapIntervalUB                        - upper bound for interval of k-swap
     * @param numberOfIterations                     - number of iterations before termination
     * @param numberOfTabuListClears                 - number of tabu list clears before termination
     * @param stoppingCriterion                      - stopping criterion to be used
     */
    public TabuSearch(
            Solution initialSolution, double timeLimit, double optimalObjectiveValue,
            int numberOfNeighbors, int maxTabuListLengthFactor, PostOptimization.ShortTermStrategies shortTermStrategy,
            int unsuccessfulNeighborGenerationAttempts, int unsuccessfulKSwapAttempts, int numberOfNonImprovingIterations,
            int kSwapIntervalUB, int numberOfIterations, int numberOfTabuListClears, PostOptimization.StoppingCriteria stoppingCriterion
    ) {
        this(
                initialSolution, optimalObjectiveValue, numberOfNeighbors, maxTabuListLengthFactor, shortTermStrategy,
                unsuccessfulNeighborGenerationAttempts, unsuccessfulKSwapAttempts, numberOfNonImprovingIterations,
                kSwapIntervalUB, numberOfIterations, numberOfTabuListClears, stoppingCriterion
        );
        this.timeLimit = timeLimit;
    }

    /**
     * Constructor
     *
     * @param initialSolution                        - initial solution to be improved
     * @param optimalObjectiveValue                  - optimal objective value for the solution (generated by CPLEX)
     * @param numberOfNeighbors                      - number of neighbors considered in short-term strategy
     * @param maxTabuListLengthFactor                - factor used to compute max length of tabu list
     * @param shortTermStrategy                      - strategy for neighborhood exploration
     * @param unsuccessfulNeighborGenerationAttempts - number of unsuccessful neighbor generation attempts before TL reset
     * @param unsuccessfulKSwapAttempts              - number of unsuccessful k swap attempts before skip
     * @param numberOfNonImprovingIterations         - number of non improving iterations before termination
     * @param kSwapIntervalUB                        - upper bound for interval of k-swap
     * @param numberOfIterations                     - number of iterations before termination
     * @param numberOfTabuListClears                 - number of tabu list clears before termination
     * @param stoppingCriterion                      - stopping criterion to be used
     */
    public TabuSearch(
            Solution initialSolution, double optimalObjectiveValue, int numberOfNeighbors, int maxTabuListLengthFactor,
            PostOptimization.ShortTermStrategies shortTermStrategy, int unsuccessfulNeighborGenerationAttempts,
            int unsuccessfulKSwapAttempts, int numberOfNonImprovingIterations, int kSwapIntervalUB, int numberOfIterations,
            int numberOfTabuListClears, PostOptimization.StoppingCriteria stoppingCriterion
    ) {
        this.currSol = new Solution(initialSolution);
        this.bestSol = new Solution(initialSolution);
        this.tabuList = new LinkedList<>();
        this.tabuListClears = 0;
        this.failCnt = 0;
        this.iterationOfLastImprovement = 0;

        this.numberOfNeighbors = numberOfNeighbors;
        this.maxTabuListLengthFactor = maxTabuListLengthFactor;
        this.shortTermStrategy = shortTermStrategy;
        this.stoppingCriterion = stoppingCriterion;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.unsuccessfulKSwapAttempts = unsuccessfulKSwapAttempts;
        this.numberOfNonImprovingIterations = numberOfNonImprovingIterations;

        this.kSwapIntervalUB = kSwapIntervalUB;

        this.numberOfIterations = numberOfIterations;
        this.numberOfTabuListClears = numberOfTabuListClears;

        this.maxTabuListLength = this.numberOfNeighbors * this.maxTabuListLengthFactor;
        this.startTime = System.currentTimeMillis();
        this.timeLimit = 0;
        this.optimalObjectiveValue = optimalObjectiveValue;
    }

    /**
     * Clears the entries in the shift tabu list and increments the clear counter.
     */
    public void clearTabuList() {
//        System.out.println("clearing tabu list...");
        this.tabuList = new LinkedList<>();
        this.tabuListClears++;
    }

    /**
     * Returns a random position in the storage area.
     *
     * @param sol - the solution the storage area is based on
     * @return random position in the storage area
     */
    public StackPosition getRandomPositionInStorageArea(Solution sol) {
        int stackIdx = HeuristicUtil.getRandomIntegerInBetween(0, sol.getFilledStacks().length - 1);
        int level = HeuristicUtil.getRandomIntegerInBetween(0, sol.getFilledStacks()[stackIdx].length - 1);
        return new StackPosition(stackIdx, level);
    }

    /**
     * Retrieves the free slots in the storage area.
     *
     * @param sol - the solution retrieve the free slots for
     * @return list of free slots in the storage area
     */
    public ArrayList<StackPosition> getFreeSlots(Solution sol) {
        ArrayList<StackPosition> freeSlots = new ArrayList<>();
        for (int stack = 0; stack < sol.getFilledStacks().length; stack++) {
            for (int level = 0; level < sol.getFilledStacks()[stack].length; level++) {
                if (sol.getFilledStacks()[stack][level] == -1) {
                    freeSlots.add(new StackPosition(stack, level));
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
    public StackPosition getRandomFreeSlot(Solution sol) {
        ArrayList<StackPosition> freeSlots = this.getFreeSlots(sol);
        int freeSlotIdx = HeuristicUtil.getRandomIntegerInBetween(0, freeSlots.size() - 1);
        return freeSlots.get(freeSlotIdx);
    }

    /**
     * Exchanges the items in the specified positions in the storage area of the given solution.
     *
     * @param sol    - the solution to be altered
     * @param posOne - the first position of the exchange
     * @param posTwo - the second position of the exchange
     */
    public Swap swapItems(Solution sol, StackPosition posOne, StackPosition posTwo) {
        int itemOne = sol.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];
        int itemTwo = sol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];

        sol.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()] = itemTwo;
        sol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()] = itemOne;

        // the swap operations consists of two shift operations
        return new Swap(new Shift(itemOne, posTwo), new Shift(itemTwo, posOne));
    }

    /**
     * Shifts the item stored in pos to the shift target.
     *
     * @param sol         - the solution to be updated
     * @param item        - the item to be shifted
     * @param shiftTarget - the position the item is shifted to
     * @param pos         - the item's original position
     */
    public Shift shiftItem(Solution sol, int item, StackPosition pos, StackPosition shiftTarget) {
        sol.getFilledStacks()[shiftTarget.getStackIdx()][shiftTarget.getLevel()] =
                sol.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];
        sol.getFilledStacks()[pos.getStackIdx()][pos.getLevel()] = -1;
        return new Shift(item, shiftTarget);
    }

    /**
     * Adds the specified shift operation to the tabu list.
     * Replaces the oldest entry if the maximum length of the tabu list is reached.
     *
     * @param shift - the shift operation to be added to the tabu list
     */
    public void forbidShift(Shift shift) {
        if (this.tabuList.size() >= this.maxTabuListLength) {
            this.tabuList.poll();
        }
        this.tabuList.add(shift);
    }

    /**
     * Counts the shift failure and resets the shift tabu list if
     * the number of specified unsuccessful shift attempts is reached.
     */
    public void neighborGenerationFailure() {
        this.failCnt++;
//        if (this.failCnt == TabuSearchConfig.UNSUCCESSFUL_NEIGHBOR_GENERATION_ATTEMPTS) {
////            this.clearTabuList();
//            this.failCnt = 0;
//        }
    }

    /**
     * Generates a neighbor for the current solution using the "shift-neighborhood".
     *
     * @param shortTermStrategy - determines the strategy for the neighbor retrieval
     * @return a neighboring solution
     */
    public Solution getNeighborShift(PostOptimization.ShortTermStrategies shortTermStrategy) {
        ArrayList<Solution> nbrs = new ArrayList<>();

        this.failCnt = 0;

        while (nbrs.size() < this.numberOfNeighbors) {

            Solution neighbor = new Solution(this.currSol);
            StackPosition pos = this.getRandomPositionInStorageArea(neighbor);
            int item = neighbor.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];

            while (item == -1) {
                pos = this.getRandomPositionInStorageArea(neighbor);
                item = neighbor.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];
            }

            StackPosition shiftTarget = this.getRandomFreeSlot(neighbor);
            // the shift target shouldn't be in the same stack
            while (shiftTarget.getStackIdx() == pos.getStackIdx()) {
                shiftTarget = this.getRandomFreeSlot(neighbor);
            }

            Shift shift = this.shiftItem(neighbor, item, pos, shiftTarget);
            neighbor.lowerItemsThatAreStackedInTheAir();

            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT
                    && !this.tabuList.contains(shift)
                    && neighbor.computeCosts() < this.currSol.computeCosts()) {
                this.forbidShift(shift);
                return neighbor;
                // BEST-FIT
            } else if (!this.tabuList.contains(shift)) {
                nbrs.add(neighbor);
                this.forbidShift(shift);
            } else {
                this.neighborGenerationFailure();
                if (this.failCnt == this.unsuccessfulNeighborGenerationAttempts) {
                    this.failCnt = 0;
                    if (nbrs.size() == 0) {
                        this.clearTabuList();
                    } else {
                        return HeuristicUtil.getBestSolution(nbrs);
                    }
                }
            }

            // ASPIRATION CRITERION
            if (neighbor.computeCosts() < this.bestSol.computeCosts()) {
                if (shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT) {
                    return neighbor;
                } else {
                    nbrs.add(neighbor);
                }
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }

    /**
     * Generates a neighbor for the current solution using the "x-swap-neighborhood".
     *
     * @param shortTermStrategy - determines the strategy for the neighbor retrieval
     * @param numberOfSwaps     - number of swaps to be performed
     * @return a neighboring solution
     */
    public Solution getNeighborXSwap(PostOptimization.ShortTermStrategies shortTermStrategy, int numberOfSwaps) {

        this.failCnt = 0;
//        System.out.println(numberOfSwaps + " - swap");

        ArrayList<Solution> nbrs = new ArrayList<>();

        int cnt = 0;

        while (nbrs.size() < this.numberOfNeighbors) {

            if (numberOfSwaps > 1 && cnt++ == PostOptimization.UNSUCCESSFUL_X_SWAP_ATTEMPTS) {
                Solution best = HeuristicUtil.getBestSolution(nbrs);
                if (best.isEmpty()) {
                    return this.currSol;
                }
                return best;
            }

            Solution neighbor = new Solution(this.currSol);

            ArrayList<Swap> swapList = new ArrayList<>();

            int swapCnt = 0;

            while (swapCnt < numberOfSwaps) {

                StackPosition posOne = this.getRandomPositionInStorageArea(neighbor);
                StackPosition posTwo = this.getRandomPositionInStorageArea(neighbor);

                int swapItemOne = this.currSol.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];
                int swapItemTwo = this.currSol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];

                while (swapItemOne == -1) {
                    posOne = this.getRandomPositionInStorageArea(neighbor);
                    swapItemOne = this.currSol.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];
                }
                // the swapped items should differ
                while (swapItemTwo == -1 || swapItemTwo == swapItemOne) {
                    posTwo = this.getRandomPositionInStorageArea(neighbor);
                    swapItemTwo = this.currSol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];
                }

                Solution tmpSol = new Solution(neighbor);
                Swap swap = this.swapItems(tmpSol, posOne, posTwo);
                if (!swapList.contains(swap)) {
                    swapList.add(swap);
                    neighbor = tmpSol;
                    swapCnt++;
                    neighbor.lowerItemsThatAreStackedInTheAir();
                }
            }

            if (!neighbor.isFeasible()) { continue; }

            boolean containsTabuSwap = false;
            for (Swap swap : swapList) {
                // a swap consists of two shift operations
                if (this.tabuList.contains(swap.getShiftOne()) && this.tabuList.contains(swap.getShiftTwo())) {
                    containsTabuSwap = true;
                }
            }

            // FIRST-FIT
            if (shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT
                    && !containsTabuSwap && neighbor.computeCosts() < this.currSol.computeCosts()) {

                for (Swap swap : swapList) {
                    this.forbidShift(swap.getShiftOne());
                    this.forbidShift(swap.getShiftTwo());
                }
                return neighbor;

                // BEST-FIT
            } else if (!containsTabuSwap) {
                nbrs.add(neighbor);
                for (Swap swap : swapList) {
                    this.forbidShift(swap.getShiftOne());
                    this.forbidShift(swap.getShiftTwo());
                }
            } else {
                this.neighborGenerationFailure();
                if (this.failCnt == this.unsuccessfulNeighborGenerationAttempts) {
                    this.failCnt = 0;
                    if (nbrs.size() == 0) {
                        this.clearTabuList();
                    } else {
                        return HeuristicUtil.getBestSolution(nbrs);
                    }
                }
            }

            // ASPIRATION CRITERION
            if (neighbor.computeCosts() < this.bestSol.computeCosts()) {
                if (shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT) {
                    return neighbor;
                } else {
                    nbrs.add(neighbor);
                }
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }

    public Solution getNeighborBasedOnProbabilities(PostOptimization.ShortTermStrategies shortTermStrategy) {

        double rand = Math.random();
        Solution sol;

//        double startTime = System.currentTimeMillis();

        if (rand < 0.05) {
            sol = this.getNeighborXSwap(shortTermStrategy, HeuristicUtil.getRandomIntegerInBetween(2, this.kSwapIntervalUB));
            // shift is only possible if there are free slots
        } else if (rand < 0.5 && this.currSol.getNumberOfAssignedItems() < this.currSol.getFilledStacks().length * this.currSol.getFilledStacks()[0].length) {
            sol = this.getNeighborShift(shortTermStrategy);
        } else {
            sol =  this.getNeighborXSwap(shortTermStrategy, 1);
        }
        if (sol == null) {
            return this.currSol;
        }

//        System.out.println("time: " + (System.currentTimeMillis() - startTime) / 1000);

        return sol;
    }

    /**
     * Applies the combined shift- and swap-neighborhood to retrieve the best neighbor.
     *
     * @param shortTermStrategy - determines the strategy for the neighbor retrieval
     * @return best generated neighbor
     */
    public Solution getNeighbor(PostOptimization.ShortTermStrategies shortTermStrategy) {

        List<Solution> nbrs = new ArrayList<>();

        // shift is only possible if there are free slots
        if (this.currSol.getNumberOfAssignedItems() < this.currSol.getFilledStacks().length * this.currSol.getFilledStacks()[0].length) {
            nbrs.add(this.getNeighborShift(shortTermStrategy));
        }
        nbrs.add(this.getNeighborXSwap(shortTermStrategy, 1));
        nbrs.add(this.getNeighborXSwap(shortTermStrategy, HeuristicUtil.getRandomIntegerInBetween(2, this.kSwapIntervalUB)));

        return Collections.min(nbrs);
    }

    /**
     * Updates the current solution with the best neighbor.
     * Additionally, the best solution gets updated if a new best solution is found.
     *
     * @param shortTermStrategy - determines the strategy for the neighbor retrieval
     * @param iteration         - the current iteration
     */
    public void updateCurrentSolution(PostOptimization.ShortTermStrategies shortTermStrategy, int iteration) {
//        this.currSol = this.getNeighbor(shortTermStrategy);
        this.currSol = this.getNeighborBasedOnProbabilities(shortTermStrategy);
        if (this.currSol.computeCosts() < this.bestSol.computeCosts()) {
            this.bestSol = this.currSol;
            this.iterationOfLastImprovement = iteration;
        }
    }

    /**
     * Performs the tabu search with a number of iterations as stop criterion.
     */
    public void solveIterations() {
        for (int i = 0; i < this.numberOfIterations; i++) {
//            System.out.println(this.tabuList.size());

            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }

            this.updateCurrentSolution(PostOptimization.SHORT_TERM_STRATEGY, i);
        }
    }

    /**
     * Performs the tabu search with a number of tabu list clears as stop criterion.
     */
    public void solveTabuListClears() {
        int iteration = 0;
        while (this.tabuListClears < PostOptimization.NUMBER_OF_TABU_LIST_CLEARS) {

            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }

            this.updateCurrentSolution(PostOptimization.SHORT_TERM_STRATEGY, iteration++);
        }
    }

    /**
     * Performs the tabu search with a number of non-improving iterations as stop criterion.
     */
    public void solveIterationsSinceLastImprovement() {
        int iteration = 0;
        while (Math.abs(this.iterationOfLastImprovement - iteration) < this.numberOfNonImprovingIterations) {

            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }

            System.out.println(this.tabuList.size());
            System.out.println(this.bestSol.computeCosts());
            System.out.println("diff: " + Math.abs(this.iterationOfLastImprovement - iteration));
            this.updateCurrentSolution(PostOptimization.SHORT_TERM_STRATEGY, iteration++);
        }
    }

    /**
     * Improves a given solution to a stacking problem using a tabu search.
     *
     * @return the best solution generated in the tabu search procedure
     */
    public Solution solve() {

        this.startTime = System.currentTimeMillis();

        switch (PostOptimization.STOPPING_CRITERION) {
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


