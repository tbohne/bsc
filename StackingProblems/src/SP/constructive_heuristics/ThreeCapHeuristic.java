package SP.constructive_heuristics;

import SP.representations.BipartiteGraph;
import SP.representations.Instance;
import SP.representations.MCMEdge;
import SP.representations.Solution;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import SP.util.RatingSystem;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

/**
 * Constructive heuristic to efficiently generate feasible solutions to stacking
 * problems with a stack capacity of 3. The goal is to minimize the transport costs
 * while respecting all given constraints.
 *
 * @author Tim Bohne
 */
public class ThreeCapHeuristic {

    private Instance instance;
    private double startTime;
    private int timeLimit;

    /**
     * Constructor
     *
     * @param instance  - the instance of the stacking problem to be solved
     * @param timeLimit - the time limit for the solving procedure
     */
    public ThreeCapHeuristic(Instance instance, int timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
    }

    /**
     * Adds the vertices for item triples, item pairs, unmatched items and stacks to the specified graph
     * and fills the partitions that define the bipartite graph.
     *
     * @param itemTriples    - the list of item triples
     * @param itemPairs      - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @param graph          - the graph to be created
     * @param partitionOne   - the first partition of the bipartite graph
     * @param partitionTwo   - the second partition of the bipartite graph
     */
    public void addVertices(ArrayList<ArrayList<Integer>> itemTriples, ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph, Set<String> partitionOne, Set<String> partitionTwo) {

            GraphUtil.addVerticesForItemTriples(itemTriples, graph, partitionOne);
            GraphUtil.addVerticesForItemPairs(itemPairs, graph, partitionOne);
            GraphUtil.addVerticesForUnmatchedItems(unmatchedItems, graph, partitionOne);
            GraphUtil.addVerticesForStacks(this.instance.getStacks(), graph, partitionTwo);
    }

    /**
     * Generates the complete bipartite graph consisting of the item partition and the stack partition.
     * Since the two partitions aren't necessarily equally sized and the used algorithm to compute the
     * MinCostPerfectMatching expects a complete bipartite graph, there are dummy items that are used to make
     * the graph complete bipartite. These items have no influence on the costs and are ignored in later steps.
     *
     * @param itemTriples    - the list of item triples
     * @param itemPairs      - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @return MinCostPerfectMatching between items and stacks
     */
    public BipartiteGraph generateBipartiteGraph(ArrayList<ArrayList<Integer>> itemTriples,
        ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {

            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(
                DefaultWeightedEdge.class
            );
            Set<String> partitionOne = new HashSet<>();
            Set<String> partitionTwo = new HashSet<>();
            this.addVertices(itemTriples, itemPairs, unmatchedItems, graph, partitionOne, partitionTwo);

            ArrayList<Integer> dummyItems = GraphUtil.introduceDummyVertices(graph, partitionOne, partitionTwo);
            GraphUtil.addEdgesForItemTriples(graph, itemTriples, this.instance.getStacks(), this.instance.getCosts());
            GraphUtil.addEdgesForItemPairs(graph, itemPairs, this.instance.getStacks(), this.instance.getCosts());
            GraphUtil.addEdgesForUnmatchedItems(graph, unmatchedItems, this.instance.getStacks(), this.instance.getCosts());
            GraphUtil.addEdgesForDummyItems(graph, dummyItems, this.instance.getStacks());

            return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    /**
     * Computes item triples from the list of item pairs and the list of unmatched items.
     *
     * @param itemPairs      - the list of item pairs
     * @param unmatchedItems - the list of unmatched items
     * @return list of compatible item triples
     */
    public ArrayList<ArrayList<Integer>> computeCompatibleItemTriples(ArrayList<MCMEdge> itemPairs, ArrayList<Integer> unmatchedItems) {
        DefaultUndirectedGraph<String, DefaultEdge> graph = GraphUtil.generateBipartiteGraphBetweenPairsOfItemsAndUnmatchedItems(
            itemPairs, unmatchedItems, this.instance.getStackingConstraints()
        );
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        ArrayList<ArrayList<Integer>> itemTriples = GraphUtil.parseItemTripleFromMCM(mcm);

        return itemTriples;
    }

    /**
     * Generates completely filled stacks based on the given list of item pairs.
     * The first x item pairs get splitted and the resulting items are assigned to the remaining
     * itemPairs.size() - x pairs to build up item triples.
     * The maximum number of items that could be splitted and theoretically reassigned to the remaining pairs is
     * itemPairs.size() / 3. However, because of the stacking constraints there should be more options to assign
     * the items to. Therefore x = itemPairs.size() / 3.4 is used. The 3.4 is the result of an experimental comparison
     * of the results for different x-values.
     *
     * @param itemPairs              - the pairs of items to be merged
     * @param completelyFilledStacks - the list of completely filled stacks to be generated
     */
    public void mergeItemPairs(ArrayList<MCMEdge> itemPairs, ArrayList<ArrayList<Integer>> completelyFilledStacks) {
        ArrayList<MCMEdge> splitPairs = new ArrayList<>();
        ArrayList<MCMEdge> assignPairs = new ArrayList<>();
        for (int i = 0; i < itemPairs.size(); i++) {
            if (i < Math.floor(itemPairs.size() / 3.4)) {
                splitPairs.add(itemPairs.get(i));
            } else {
                assignPairs.add(itemPairs.get(i));
            }
        }
        ArrayList<Integer> splittedItems = new ArrayList<>();
        for (MCMEdge splitPair : splitPairs) {
            splittedItems.add(splitPair.getVertexOne());
            splittedItems.add(splitPair.getVertexTwo());
        }
        completelyFilledStacks.addAll(this.computeCompatibleItemTriples(assignPairs, splittedItems));
    }

    /**
     * Initializes the item pair lists with the number of applied rating systems.
     *
     * @param itemPairLists - the lists of item pairs to be initialized
     */
    public void introduceMinimumNumberOfItemPairPermutations(
        ArrayList<ArrayList<MCMEdge>> itemPairLists, int minimumNumberOfItemPairPermutations
    ) {
        while (itemPairLists.size() < minimumNumberOfItemPairPermutations) {
            itemPairLists.add(HeuristicUtil.getCopyOfEdgeList(itemPairLists.get(0)));
        }
    }

    /**
     * Applies the different rating systems to the lists of item pairs.
     * The rating systems are used to sort the lists.
     *
     * @param itemPairLists - the lists of item pairs to be rated
     * @return the number of applied rating systems
     */
    public int applyRatingSystems(ArrayList<ArrayList<MCMEdge>> itemPairLists) {
        int minimumNumberOfItemPairPermutations = 15;
        if (itemPairLists.size() < minimumNumberOfItemPairPermutations) {
            this.introduceMinimumNumberOfItemPairPermutations(itemPairLists, minimumNumberOfItemPairPermutations);
        }
        // the first list (idx 0) should be unrated and unsorted
        int ratingSystemIdx = 1;

        RatingSystem.assignNewRatingToEdges(itemPairLists.get(
            ratingSystemIdx++), this.instance.getStackingConstraints(), this.instance.getCosts(), this.instance.getStacks()
        );
        RatingSystem.assignMinRatingToEdges(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignMaxRatingToEdges(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignSumRatingToEdges(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignColRatingToEdgesNewWay(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignColRatingToEdges(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignRowRatingToEdges(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignNewRatingToEdges(
            itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints(), this.instance.getCosts(), this.instance.getStacks()
        );
        RatingSystem.assignMinRatingToEdges(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignMaxRatingToEdges(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignSumRatingToEdges(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignColRatingToEdgesNewWay(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignColRatingToEdges(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());
        RatingSystem.assignRowRatingToEdges(itemPairLists.get(ratingSystemIdx++), this.instance.getStackingConstraints());

        // -1 because the list with the idx 0 has no rating system
        return ratingSystemIdx - 1;
    }

    /**
     * Sorts the lists of item pairs based on their ratings.
     * Additionally, a number of random shuffles of item pairs could be added.
     *
     * @param itemPairLists - the lists of items pairs to be sorted
     */
    public void sortItemPairListsBasedOnRatings(ArrayList<ArrayList<MCMEdge>> itemPairLists, int numberOfUsedRatingSystems) {
        for (int i = 1; i < itemPairLists.size(); i++) {
            if (i <= numberOfUsedRatingSystems) {
                Collections.sort(itemPairLists.get(i));
                if (i >= (numberOfUsedRatingSystems / 2) + 1) {
                    Collections.reverse(itemPairLists.get(i));
                }
            } else {
                Collections.shuffle(itemPairLists.get(i));
            }
        }
    }

    /**
     * Initializes the copies of the lists of completely filled stacks and item pairs.
     *
     * @param itemPairs                     - the pairs of items
     * @param itemPairLists                 - the lists of item pairs (different permutations)
     * @param listsOfCompletelyFilledStacks - the lists of completely filled stacks to be initialized
     * @param numberOfItemPairPermutations  - the number of item pair permutations used in the merge step
     */
    public void initCopiesForDifferentItemPairPermutations(
        ArrayList<MCMEdge> itemPairs,
        ArrayList<ArrayList<MCMEdge>> itemPairLists,
        ArrayList<ArrayList<ArrayList<Integer>>> listsOfCompletelyFilledStacks,
        int numberOfItemPairPermutations
    ) {
        for (int i = 0; i < numberOfItemPairPermutations; i++) {
            listsOfCompletelyFilledStacks.add(new ArrayList<>());
            itemPairLists.add(HeuristicUtil.getCopyOfEdgeList(itemPairs));
        }
    }

    /**
     * Generates lists of completely filled stacks by merging the given item pairs to triples.
     * The given item pairs are sorted based on different rating systems which leads to a number
     * of different lists of item triples or completely filled stacks.
     *
     * @param itemPairs - the item pairs to be merged
     */
    public ArrayList<ArrayList<ArrayList<Integer>>> generateCompletelyFilledStacks(ArrayList<MCMEdge> itemPairs, int numberOfItemPairPermutations) {
        ArrayList<ArrayList<ArrayList<Integer>>> listsOfCompletelyFilledStacks = new ArrayList<>();
        ArrayList<ArrayList<MCMEdge>> itemPairLists = new ArrayList<>();
        this.initCopiesForDifferentItemPairPermutations(
            itemPairs, itemPairLists, listsOfCompletelyFilledStacks, numberOfItemPairPermutations
        );
        if (numberOfItemPairPermutations > 1) {
            int numberOfUsedRatingSystems = this.applyRatingSystems(itemPairLists);
            this.sortItemPairListsBasedOnRatings(itemPairLists, numberOfUsedRatingSystems);
        }
        for (int i = 0; i < numberOfItemPairPermutations; i++) {
            this.mergeItemPairs(itemPairLists.get(i), listsOfCompletelyFilledStacks.get(i));
        }
        return listsOfCompletelyFilledStacks;
    }

    /**
     * Encapsulates the stacking constraint graph generation.
     *
     * @return the generated stacking constraint graph
     */
    public DefaultUndirectedGraph<String, DefaultEdge> generateStackingConstraintGraph(int[] items) {
        return GraphUtil.generateStackingConstraintGraph(
            items,
            this.instance.getStackingConstraints(),
            this.instance.getCosts(),
            Integer.MAX_VALUE / this.instance.getItems().length,
            this.instance.getStacks()
        );
    }

    /**
     * Generates compatible item pairs that are stackable in at least one direction.
     *
     * @param items - the items to build up pairs from
     * @return the generated item pairs
     */
    public ArrayList<MCMEdge> generateItemPairs(int[] items) {
        DefaultUndirectedGraph<String, DefaultEdge> stackingConstraintGraph = this.generateStackingConstraintGraph(items);
        EdmondsMaximumCardinalityMatching<String, DefaultEdge> itemMatching = new EdmondsMaximumCardinalityMatching<>(
            stackingConstraintGraph
        );
        return GraphUtil.parseItemPairFromMCM(itemMatching);
    }

    /**
     * Merges the item pairs to triples. There is a number of differently ordered item pair lists
     * used in the merge step which results in a number of different resulting list of triples.
     *
     * @param numberOfItemPairPermutations - determines the number of different item pair orders
     * @param itemPairs                    - the list of item pairs to be merged in different ways
     * @return the list of the generated item triple lists
     */
    public ArrayList<ArrayList<ArrayList<Integer>>> mergePairsToTriples(int numberOfItemPairPermutations, ArrayList<MCMEdge> itemPairs) {
        ArrayList<ArrayList<ArrayList<Integer>>> itemTripleLists = new ArrayList<>();
        for (int i = 0; i < numberOfItemPairPermutations; i++) {
            itemTripleLists.add(new ArrayList<>());
        }
        for (int i = 0; i < itemTripleLists.size(); i++) {
            itemTripleLists.get(i).addAll(this.generateCompletelyFilledStacks(itemPairs, numberOfItemPairPermutations).get(i));
        }
        return itemTripleLists;
    }

    public String getUsedRatingSystem(int idx) {
        switch (idx) {
            case 0:
                return "unrated";
            case 1:
                return "new";
            case 2:
                return "min";
            case 3:
                return "max";
            case 4:
                return "sum";
            case 5:
                return "colNew";
            case 6:
                return "col";
            case 7:
                return "row";
            case 8:
                return "newRev";
            case 9:
                return "minRev";
            case 10:
                return "maxRev";
            case 11:
                return "sumRev";
            case 12:
                return "colNewRev";
            case 13:
                return "colRev";
            case 14:
                return "rowRev";
            default:
                return "unknown";
        }
    }

    /**
     * Generates solutions to the stacking problem based on the different lists of item triples.
     * Basic idea:
     *      - generate pairs again via MCM
     *      - consider remaining items to be unmatched
     *      - generate bipartite graph (items, stacks)
     *      - compute MWPM and interpret edges as stack assignments
     *      - fix order of items inside the stacks
     *
     * @param itemTripleLists - the different lists of item triples
     * @return the generated solutions based on the given lists of triples
     */
    public ArrayList<Solution> generateSolutionsBasedOnListsOfTriples(ArrayList<ArrayList<ArrayList<Integer>>> itemTripleLists) {

        ArrayList<Solution> solutions = new ArrayList<>();

        int cnt = 0;

        for (ArrayList<ArrayList<Integer>> itemTriples : itemTripleLists) {
            this.instance.resetStacks();
            ArrayList<Integer> unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(itemTriples, this.instance.getItems());
            // items that are stored as pairs
            ArrayList<MCMEdge> itemPairs = this.generateItemPairs(HeuristicUtil.getItemArrayFromItemList(unmatchedItems));
            // items that are stored in their own stack
            unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriplesAndPairs(itemTriples, itemPairs, this.instance.getItems());
            // unable to assign the items feasibly to the given number of stacks
            if (itemTriples.size() + itemPairs.size() + unmatchedItems.size() > this.instance.getStacks().length) {
                continue;
            }
            BipartiteGraph bipartiteGraph = this.generateBipartiteGraph(itemTriples, itemPairs, unmatchedItems);
            KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
                new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                    bipartiteGraph.getGraph(), bipartiteGraph.getPartitionOne(), bipartiteGraph.getPartitionTwo()
                )
            ;
            GraphUtil.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());

            Solution sol = new Solution((System.currentTimeMillis() - startTime) / 1000.0, this.timeLimit, this.instance);

            System.out.println(this.getUsedRatingSystem(cnt++) + ": " + sol.computeCosts());

            sol.transformStackAssignmentsIntoValidSolutionIfPossible();
            solutions.add(new Solution(sol));
        }
        return solutions;
    }

    /**
     * Returns the best solution from the list of generated solutions.
     *
     * @param solutions - the list of generated solutions
     * @return the best solution based on the costs
     */
    public Solution getBestSolution(ArrayList<Solution> solutions) {
        Solution bestSol = new Solution();
        for (Solution sol : solutions) {
            if (sol.computeCosts() < bestSol.computeCosts()) {
                bestSol = sol;
            }
        }
        return bestSol;
    }

    /**
     * Solves the given instance of the stacking problem. The objective is to minimize the transport costs.
     *
     * Basic idea:
     *      - generate item pairs via MCM
     *      - merge item pairs to triples based on different rating systems
     *      - compute unmatched items based on triples
     *      - generate pairs again via MCM
     *      - consider remaining items to be unmatched
     *      - generate bipartite graph (items, stacks) for each rating system
     *      - compute MWPM and interpret edges as stack assignments for each rating system
     *      - generate corresponding solutions
     *      - determine best solution based on costs
     *      - fix order of items inside the stacks
     *
     * @return the solution generated by the heuristic
     */
    public Solution solve(int numberOfItemPairPermutations) {

        Solution bestSol = new Solution();

        if (this.instance.getStackCapacity() == 3) {
            this.startTime = System.currentTimeMillis();
            ArrayList<MCMEdge> itemPairs = this.generateItemPairs(this.instance.getItems());
            ArrayList<ArrayList<ArrayList<Integer>>> itemTripleLists = this.mergePairsToTriples(
                numberOfItemPairPermutations, itemPairs
            );
            ArrayList<Solution> solutions = this.generateSolutionsBasedOnListsOfTriples(itemTripleLists);
            bestSol = this.getBestSolution(solutions);
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 3.");
        }
        return bestSol;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////// DEPRECATED //////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Returns an item pair the given item can be assigned to based on a best-fit strategy.
     * For each triple that results from assigning the given item to a potential target pair,
     * the cost value for the cheapest feasible stack assignment is used to update the overall
     * minimum cost value. Finally, the target pair that leads to the cheapest assignment is chosen.
     *
     * @param items                - the list of items that are part of the instance
     * @param potentialTargetPairs - the list of item pairs the item could be assigned to
     * @param costs                - the matrix containing the costs of item-stack assignments
     * @param stacks               - the available stacks
     * @param item                 - the item to be assigned to a pair
     * @return
     */
    public static MCMEdge getTargetPairForItemBestFit(
            int[] items, ArrayList<MCMEdge> potentialTargetPairs, int[][] costs, int[][] stacks, int item
    ) {
        MCMEdge bestTargetPair = new MCMEdge(0, 0, 0);
        if (potentialTargetPairs.size() > 0) {
            bestTargetPair = potentialTargetPairs.get(0);
        }
        int minCost = Integer.MAX_VALUE / items.length;
        int costsForIncompatibleStack = Integer.MAX_VALUE / items.length;

        for (MCMEdge targetPair : potentialTargetPairs) {
            ArrayList<Integer> costValues = new ArrayList<>();

            for (int stack = 0; stack < stacks.length; stack++) {
                if (costs[item][stack] < costsForIncompatibleStack
                        && costs[targetPair.getVertexOne()][stack] < costsForIncompatibleStack
                        && costs[targetPair.getVertexTwo()][stack] < costsForIncompatibleStack
                        ) {
                    costValues.add(costs[item][stack]
                            + costs[targetPair.getVertexOne()][stack]
                            + costs[targetPair.getVertexTwo()][stack]
                    );
                }
            }
            if (Collections.min(costValues) < minCost) {
                minCost = Collections.min(costValues);
                bestTargetPair = targetPair;
            }
        }
        return bestTargetPair;
    }

    /**
     * Assigns the items of the splitted pair to their most promising target pairs.
     *
     * @param potentialItemOnePairs  - list of pairs item one is assignable to
     * @param potentialItemTwoPairs  - list of pairs item two is assignable to
     * @param splitPair              - the pair to be splitted
     * @param completelyFilledStacks - the list of item triples
     * @param itemPairRemovalList    - the list of item pairs to be removed
     * @param itemOne                - the first item of the splitted pair
     * @param itemTwo                - the second item of the splitted pair
     */
    public void assignItemsToPairs(
            ArrayList<MCMEdge> potentialItemOnePairs, ArrayList<MCMEdge> potentialItemTwoPairs, MCMEdge splitPair,
            ArrayList<ArrayList<Integer>> completelyFilledStacks, ArrayList<MCMEdge> itemPairRemovalList, int itemOne, int itemTwo
    ) {
        boolean itemOneAssigned = false;
        boolean itemTwoAssigned = false;

        MCMEdge itemOneTargetPair = getTargetPairForItemBestFit(
                this.instance.getItems(), potentialItemOnePairs, this.instance.getCosts(), this.instance.getStacks(), itemOne
        );
        if (!(itemOneTargetPair.getVertexOne() == 0 && itemOneTargetPair.getVertexTwo() == 0)) {
            itemOneAssigned = true;
        }
        this.extendPotentialItemPairsForSecondItem(potentialItemOnePairs, itemOneTargetPair, itemTwo, potentialItemTwoPairs);
        MCMEdge itemTwoTargetPair = getTargetPairForItemBestFit(
                this.instance.getItems(), potentialItemTwoPairs, this.instance.getCosts(), this.instance.getStacks(), itemTwo
        );
        if (!(itemTwoTargetPair.getVertexOne() == 0 && itemTwoTargetPair.getVertexTwo() == 0)) {
            itemTwoAssigned = true;
        }
        if (itemOneAssigned && itemTwoAssigned) {
            HeuristicUtil.updateCompletelyFilledStacks(
                    itemPairRemovalList, itemOneTargetPair, itemTwoTargetPair, splitPair, itemOne, itemTwo, completelyFilledStacks
            );
        }
    }

    /**
     * Generates completely filled stacks from the list of item pairs.
     * Breaks up a pair and tries to assign both items two new pairs to build up completely filled stacks.
     *
     * @param itemPairs              - the list of item pairs
     * @param itemPairRemovalList    - the list of edges (item pairs) to be removed

     */
    public ArrayList<ArrayList<ArrayList<Integer>>> generateCompletelyFilledStacks(
            ArrayList<MCMEdge> itemPairs, ArrayList<MCMEdge> itemPairRemovalList, int numberOfItemPairPermutations
    ) {

        ArrayList<ArrayList<ArrayList<Integer>>> listsOfCompletelyFilledStacks = new ArrayList<>();
        ArrayList<ArrayList<MCMEdge>> itemPairLists = new ArrayList<>();
        itemPairLists.add(itemPairs);
        this.initCopiesForDifferentItemPairPermutations(
                itemPairs, itemPairLists, listsOfCompletelyFilledStacks, numberOfItemPairPermutations
        );
        if (numberOfItemPairPermutations > 1) {
            int numberOfUsedRatingSystems = this.applyRatingSystems(itemPairLists);
            this.sortItemPairListsBasedOnRatings(itemPairLists, numberOfUsedRatingSystems);
        }

        for (int i = 0; i < numberOfItemPairPermutations; i++) {

            for (MCMEdge splitPair : itemPairLists.get(i)) {

                if (itemPairRemovalList.contains(splitPair)) { continue; }

                int itemOne = splitPair.getVertexOne();
                int itemTwo = splitPair.getVertexTwo();
                ArrayList<MCMEdge> potentialItemOnePairs = new ArrayList<>();
                ArrayList<MCMEdge> potentialItemTwoPairs = new ArrayList<>();

                for (MCMEdge potentialTargetPair : itemPairLists.get(i)) {

                    if (itemPairRemovalList.contains(potentialTargetPair)) { continue; }

                    if (splitPair != potentialTargetPair) {
                        int potentialTargetPairItemOne = potentialTargetPair.getVertexOne();
                        int potentialTargetPairItemTwo = potentialTargetPair.getVertexTwo();

                        if (HeuristicUtil.itemAssignableToPair(
                                itemOne, potentialTargetPairItemOne, potentialTargetPairItemTwo, this.instance.getStackingConstraints()
                        )) {
                            potentialItemOnePairs.add(potentialTargetPair);
                            continue;
                        }
                        if (HeuristicUtil.itemAssignableToPair(
                                itemTwo, potentialTargetPairItemOne, potentialTargetPairItemTwo, this.instance.getStackingConstraints()
                        )) {
                            potentialItemTwoPairs.add(potentialTargetPair);
                            continue;
                        }
                    }
                }
                this.assignItemsToPairs(
                    potentialItemOnePairs, potentialItemTwoPairs, splitPair, listsOfCompletelyFilledStacks.get(i), itemPairRemovalList, itemOne, itemTwo
                );
            }
        }
        return listsOfCompletelyFilledStacks;
    }

    /**
     * Extends the potential item pairs for the second item of the splitted pair in the merge step
     * with the pairs from the first item that are not used and also compatible with the second item.
     *
     * @param potentialItemOnePairs - the list of potential item pairs the first item can be assigned to
     * @param itemOneTargetPair     - the pair the first item gets assigned to
     * @param itemTwo               - the item whose potential target pairs gets updated
     * @param potentialItemTwoPairs - the list of potential item pairs the second item can be assigned to
     */
    public void extendPotentialItemPairsForSecondItem(
            ArrayList<MCMEdge> potentialItemOnePairs, MCMEdge itemOneTargetPair, int itemTwo, ArrayList<MCMEdge> potentialItemTwoPairs
    ) {
        for (MCMEdge potentialPair : potentialItemOnePairs) {
            if (potentialPair != itemOneTargetPair && HeuristicUtil.itemAssignableToPair(
                    itemTwo, potentialPair.getVertexOne(), potentialPair.getVertexTwo(), this.instance.getStackingConstraints()
            )) {
                potentialItemTwoPairs.add(potentialPair);
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
