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
    private final double optimalObjectiveValue;

    private final PostOptimization.ShortTermStrategies shortTermStrategy;
    private final PostOptimization.StoppingCriteria stoppingCriterion;
    private final int numberOfNeighbors;

    private Queue<Shift> tabuList;
    private int tabuListClears;
    private final int maxTabuListLength;
    private int failCnt;
    private final int unsuccessfulNeighborGenerationAttempts;
    private final int unsuccessfulKSwapAttempts;
    private final int kSwapIntervalUB;
    private final float kSwapProbability;
    private final float swapProbability;

    private int iterationOfLastImprovement;
    private final int numberOfNonImprovingIterations;
    private final int numberOfIterations;
    private final int numberOfTabuListClears;

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
        Solution initialSolution, double timeLimit, double optimalObjectiveValue, int numberOfNeighbors,
        int maxTabuListLengthFactor, PostOptimization.ShortTermStrategies shortTermStrategy,
        int unsuccessfulNeighborGenerationAttempts, int unsuccessfulKSwapAttempts, int numberOfNonImprovingIterations,
        int kSwapIntervalUB, int numberOfIterations, int numberOfTabuListClears,
        PostOptimization.StoppingCriteria stoppingCriterion, float kSwapProbability, float swapProbability
    ) {
        this(
            initialSolution, optimalObjectiveValue, numberOfNeighbors, maxTabuListLengthFactor, shortTermStrategy,
            unsuccessfulNeighborGenerationAttempts, unsuccessfulKSwapAttempts, numberOfNonImprovingIterations,
            kSwapIntervalUB, numberOfIterations, numberOfTabuListClears, stoppingCriterion, kSwapProbability, swapProbability
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
     * @param kSwapProbability                       - probability for applying the k-swap operator
     * @param swapProbability                        - probability for applying the swap operator
     */
    public TabuSearch(
        Solution initialSolution, double optimalObjectiveValue, int numberOfNeighbors, int maxTabuListLengthFactor,
        PostOptimization.ShortTermStrategies shortTermStrategy, int unsuccessfulNeighborGenerationAttempts,
        int unsuccessfulKSwapAttempts, int numberOfNonImprovingIterations, int kSwapIntervalUB, int numberOfIterations,
        int numberOfTabuListClears, PostOptimization.StoppingCriteria stoppingCriterion, float kSwapProbability, float swapProbability
    ) {
        this.currSol = new Solution(initialSolution);
        this.bestSol = new Solution(initialSolution);
        this.tabuList = new LinkedList<>();
        this.tabuListClears = 0;
        this.failCnt = 0;
        this.iterationOfLastImprovement = 0;

        this.numberOfNeighbors = numberOfNeighbors;
        this.shortTermStrategy = shortTermStrategy;
        this.stoppingCriterion = stoppingCriterion;
        this.unsuccessfulNeighborGenerationAttempts = unsuccessfulNeighborGenerationAttempts;
        this.unsuccessfulKSwapAttempts = unsuccessfulKSwapAttempts;
        this.numberOfNonImprovingIterations = numberOfNonImprovingIterations;

        this.kSwapIntervalUB = kSwapIntervalUB;
        this.kSwapProbability = kSwapProbability;
        this.swapProbability = swapProbability;

        this.numberOfIterations = numberOfIterations;
        this.numberOfTabuListClears = numberOfTabuListClears;

        this.maxTabuListLength = this.numberOfNeighbors * maxTabuListLengthFactor;
        this.startTime = System.currentTimeMillis();
        this.timeLimit = 0;
        this.optimalObjectiveValue = optimalObjectiveValue;
    }

    /**
     * Improves a given solution to a stacking problem using a tabu search.
     *
     * @return best solution generated in the tabu search procedure
     */
    public Solution solve() {

        this.startTime = System.currentTimeMillis();

        switch (this.stoppingCriterion) {
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

    /**
     * Clears the entries in the shift tabu list and increments the clear counter.
     */
    private void clearTabuList() {
        this.tabuList = new LinkedList<>();
        this.tabuListClears++;
    }

    /**
     * Returns a random position in the stacks.
     *
     * @param sol - solution for which to retrieve a random stack position
     * @return random position in the stacks
     */
    private StackPosition getRandomStackPosition(Solution sol) {
        int stackIdx = HeuristicUtil.getRandomIntegerInBetween(0, sol.getFilledStacks().length - 1);
        int level = HeuristicUtil.getRandomIntegerInBetween(0, sol.getFilledStacks()[stackIdx].length - 1);
        return new StackPosition(stackIdx, level);
    }

    /**
     * Retrieves the free slots in the stacks.
     *
     * @param sol - solution to retrieve the free slots for
     * @return list of free slots in the stacks
     */
    private List<StackPosition> getFreeSlots(Solution sol) {
        List<StackPosition> freeSlots = new ArrayList<>();
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
     * Returns a random free slot in the stacks of the specified solution.
     *
     * @param sol - specified solution to return a free slot for
     * @return random free slot in the stacks
     */
    private StackPosition getRandomFreeSlot(Solution sol) {
        List<StackPosition> freeSlots = this.getFreeSlots(sol);
        int freeSlotIdx = HeuristicUtil.getRandomIntegerInBetween(0, freeSlots.size() - 1);
        return freeSlots.get(freeSlotIdx);
    }

    /**
     * Exchanges the items in the specified positions in the stacks of the given solution.
     *
     * @param sol    - solution to be altered
     * @param posOne - first position of the exchange
     * @param posTwo - second position of the exchange
     */
    private Swap swapItems(Solution sol, StackPosition posOne, StackPosition posTwo) {
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
     * @param sol         - solution to be updated
     * @param item        - item to be shifted
     * @param shiftTarget - position the item is shifted to
     * @param pos         - the item's original position
     */
    private Shift shiftItem(Solution sol, int item, StackPosition pos, StackPosition shiftTarget) {

        sol.getFilledStacks()[shiftTarget.getStackIdx()][shiftTarget.getLevel()] =
            sol.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];

        sol.getFilledStacks()[pos.getStackIdx()][pos.getLevel()] = -1;
        return new Shift(item, shiftTarget);
    }

    /**
     * Adds the specified shift operation to the tabu list.
     * Replaces the oldest entry if the maximum length of the tabu list is reached.
     *
     * @param shift - shift operation to be added to the tabu list
     */
    private void forbidShift(Shift shift) {
        if (this.tabuList.size() >= this.maxTabuListLength) {
            this.tabuList.poll();
        }
        this.tabuList.add(shift);
    }

    /**
     * Returns a random stack position that is occupied with an item.
     *
     * @param neighbor - neighbor to return an occupied stack position for
     * @return occupied stack position
     */
    private StackPosition getRandomStackPositionFilledWithItem(Solution neighbor) {
        StackPosition pos = this.getRandomStackPosition(neighbor);
        int item = neighbor.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];
        while (item == -1) {
            pos = this.getRandomStackPosition(neighbor);
            item = neighbor.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];
        }
        return pos;
    }

    /**
     * Ensures that the shift target is in a different stack.
     *
     * @param shiftTarget - stack position the item gets shifted to
     * @param pos         - current position of the item
     * @param neighbor    - considered solution
     */
    private void getRandomShiftTargetFromOtherStack(StackPosition shiftTarget, StackPosition pos, Solution neighbor) {
        while (shiftTarget.getStackIdx() == pos.getStackIdx()) {
            shiftTarget = this.getRandomFreeSlot(neighbor);
        }
    }

    /**
     * Generates a neighbor for the current solution using the "shift-neighborhood".
     *
     * @return neighboring solution
     */
    @SuppressWarnings("Duplicates")
    private Solution getNeighborShift() {

        List<Solution> nbrs = new ArrayList<>();
        this.failCnt = 0;

        while (nbrs.size() < this.numberOfNeighbors) {

            Solution neighbor = new Solution(this.currSol);
            StackPosition pos = this.getRandomStackPositionFilledWithItem(neighbor);
            int item = neighbor.getFilledStacks()[pos.getStackIdx()][pos.getLevel()];
            StackPosition shiftTarget = this.getRandomFreeSlot(neighbor);
            this.getRandomShiftTargetFromOtherStack(shiftTarget, pos, neighbor);
            Shift shift = this.shiftItem(neighbor, item, pos, shiftTarget);
            neighbor.lowerItemsThatAreStackedInTheAir();

            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT && !this.tabuList.contains(shift)
                && neighbor.computeCosts() < this.currSol.computeCosts()) {

                    this.forbidShift(shift);
                    return neighbor;

            // BEST-FIT
            } else if (!this.tabuList.contains(shift)) {
                nbrs.add(neighbor);
                this.forbidShift(shift);
            } else {
                this.failCnt++;
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
                if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT) {
                    return neighbor;
                } else {
                    nbrs.add(neighbor);
                }
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }

    /**
     * Checks whether the swap list contains a tabu swap operation.
     *
     * @param swapList - list of swaps to be checked for tabu operations
     * @return whether or not the swap list contains a tabu swap operation
     */
    private boolean containsTabuSwap(List<Swap> swapList) {
        for (Swap swap : swapList) {
            // a swap consists of two shift operations
            if (this.tabuList.contains(swap.getShiftOne()) && this.tabuList.contains(swap.getShiftTwo())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs the specified number of swap operations and stores them in the swap list.
     *
     * @param numberOfSwaps - number of swaps to be performed
     * @param swapList      - list to store the performer swaps
     * @return generated neighbor solution
     */
    private Solution performSwaps(int numberOfSwaps, List<Swap> swapList) {

        Solution neighbor = new Solution(this.currSol);
        int swapCnt = 0;

        while (swapCnt < numberOfSwaps) {
            StackPosition posOne = this.getRandomStackPositionFilledWithItem(neighbor);
            int swapItemOne = neighbor.getFilledStacks()[posOne.getStackIdx()][posOne.getLevel()];
            StackPosition posTwo = this.getRandomStackPositionFilledWithItem(neighbor);
            int swapItemTwo = this.currSol.getFilledStacks()[posTwo.getStackIdx()][posTwo.getLevel()];

            // the swapped items should differ
            while (swapItemTwo == swapItemOne) {
                posTwo = this.getRandomStackPosition(neighbor);
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
        return neighbor;
    }

    /**
     * Generates a neighbor for the current solution using the "k-swap-neighborhood".
     *
     * @param numberOfSwaps - number of swaps to be performed
     * @return a neighboring solution
     */
    @SuppressWarnings("Duplicates")
    private Solution getNeighborKSwap(int numberOfSwaps) {

        List<Solution> nbrs = new ArrayList<>();
        this.failCnt = 0;
        int unsuccessfulKSwapCounter = 0;

        while (nbrs.size() < this.numberOfNeighbors) {

            if (numberOfSwaps > 1 && unsuccessfulKSwapCounter++ == this.unsuccessfulKSwapAttempts) {
                Solution best = HeuristicUtil.getBestSolution(nbrs);
                if (best.isEmpty()) {
                    return this.currSol;
                }
                return best;
            }
            List<Swap> swapList = new ArrayList<>();
            Solution neighbor = this.performSwaps(numberOfSwaps, swapList);

            if (!neighbor.isFeasible()) { continue; }

            // FIRST-FIT
            if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT
                && !this.containsTabuSwap(swapList) && neighbor.computeCosts() < this.currSol.computeCosts()) {

                for (Swap swap : swapList) {
                    this.forbidShift(swap.getShiftOne());
                    this.forbidShift(swap.getShiftTwo());
                }
                return neighbor;

            // BEST-FIT
            } else if (!this.containsTabuSwap(swapList)) {
                nbrs.add(neighbor);
                for (Swap swap : swapList) {
                    this.forbidShift(swap.getShiftOne());
                    this.forbidShift(swap.getShiftTwo());
                }
            } else {
                this.failCnt++;
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
                if (this.shortTermStrategy == PostOptimization.ShortTermStrategies.FIRST_FIT) {
                    return neighbor;
                } else {
                    nbrs.add(neighbor);
                }
            }
        }
        return HeuristicUtil.getBestSolution(nbrs);
    }

    /**
     * Retrieves a neighboring solution by applying the operators with certain probabilities.
     *
     * @return neighboring solution
     */
    private Solution getNeighborBasedOnProbabilities() {

        double rand = Math.random();
        Solution sol;

        if (rand < this.kSwapProbability / 100.0) {
            sol = this.getNeighborKSwap(HeuristicUtil.getRandomIntegerInBetween(2, this.kSwapIntervalUB));
        } else if (rand < (this.swapProbability + this.kSwapProbability) / 100.0) {
            sol =  this.getNeighborKSwap(1);
        } else {
            // shift is only possible if there are free slots
            if (this.currSol.getNumberOfAssignedItems() < this.currSol.getFilledStacks().length * this.currSol.getFilledStacks()[0].length) {
                sol = this.getNeighborShift();
            } else {
                sol =  this.getNeighborKSwap(1);
            }
        }
        if (sol == null) {
            return this.currSol;
        }
        return sol;
    }

    /**
     * Applies the combined shift- and swap-neighborhood to retrieve the best neighbor.
     *
     * @return best generated neighbor
     */
    private Solution getNeighbor() {
        List<Solution> nbrs = new ArrayList<>();
        // shift is only possible if there are free slots
        if (this.currSol.getNumberOfAssignedItems() < this.currSol.getFilledStacks().length * this.currSol.getFilledStacks()[0].length) {
            nbrs.add(this.getNeighborShift());
        }
        nbrs.add(this.getNeighborKSwap(1));
        nbrs.add(this.getNeighborKSwap(HeuristicUtil.getRandomIntegerInBetween(2, this.kSwapIntervalUB)));

        return Collections.min(nbrs);
    }

    /**
     * Updates the current solution with the best neighbor.
     * Additionally, the best solution gets updated if a new best solution is found.
     *
     * @param iteration - current iteration
     */
    private void updateCurrentSolution(int iteration) {
        this.currSol = this.getNeighborBasedOnProbabilities();
        if (this.currSol.computeCosts() < this.bestSol.computeCosts()) {
            this.bestSol = this.currSol;
            this.iterationOfLastImprovement = iteration;
        }
    }

    /**
     * Performs the tabu search with a number of iterations as stop criterion.
     */
    private void solveIterations() {
        for (int i = 0; i < this.numberOfIterations; i++) {
            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }
            this.updateCurrentSolution(i);
        }
    }

    /**
     * Performs the tabu search with a number of tabu list clears as stop criterion.
     */
    private void solveTabuListClears() {
        int iteration = 0;
        while (this.tabuListClears < this.numberOfTabuListClears) {
            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }
            this.updateCurrentSolution(iteration++);
        }
    }

    /**
     * Performs the tabu search with a number of non-improving iterations as stop criterion.
     */
    private void solveIterationsSinceLastImprovement() {
        int iteration = 0;
        while (Math.abs(this.iterationOfLastImprovement - iteration) < this.numberOfNonImprovingIterations) {
            if (this.timeLimit != 0 && (System.currentTimeMillis() - this.startTime) / 1000 > this.timeLimit) { break; }
            if (this.bestSol.computeCosts() == this.optimalObjectiveValue) { break; }
            this.updateCurrentSolution(iteration++);
        }
    }
}


