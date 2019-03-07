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
     * Extends the potential item pairs for the second item of the splitted pair in the merge step
     * with the pairs from the first item that are not used and also compatible with the second item.
     *
     * @param potentialItemOnePairs - the list of potential item pairs the first item can be assigned to
     * @param itemOneTargetPair     - the pair the first item gets assigned to
     * @param itemTwo               - the item whose potential target pairs gets updated
     * @param potentialItemTwoPairs - the list of potential item pairs the second item can be assigned to
     */
    public void extendPotentialItemPairsForSecondItem(
            ArrayList<MCMEdge> potentialItemOnePairs,
            MCMEdge itemOneTargetPair,
            int itemTwo,
            ArrayList<MCMEdge> potentialItemTwoPairs
    ) {
        for (MCMEdge potentialPair : potentialItemOnePairs) {
            if (potentialPair != itemOneTargetPair && HeuristicUtil.itemAssignableToPair(
                    itemTwo,
                    potentialPair.getVertexOne(),
                    potentialPair.getVertexTwo(),
                    this.instance.getStackingConstraints()
            )) {
                potentialItemTwoPairs.add(potentialPair);
            }
        }
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
            ArrayList<MCMEdge> potentialItemOnePairs,
            ArrayList<MCMEdge> potentialItemTwoPairs,
            MCMEdge splitPair,
            ArrayList<ArrayList<Integer>> completelyFilledStacks,
            ArrayList<MCMEdge> itemPairRemovalList,
            int itemOne,
            int itemTwo
    ) {

        boolean itemOneAssigned = false;
        boolean itemTwoAssigned = false;

        MCMEdge itemOneTargetPair = getTargetPairForItemBestFit(
                this.instance.getItems(),
                potentialItemOnePairs,
                this.instance.getCosts(),
                this.instance.getStacks(),
                itemOne
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
     * @param completelyFilledStacks - list to store the completely filled stacks
     */
    public void generateCompletelyFilledStacks(
            ArrayList<MCMEdge> itemPairs,
            ArrayList<MCMEdge> itemPairRemovalList,
            ArrayList<ArrayList<Integer>> completelyFilledStacks
    ) {

//        ArrayList<MCMEdge> splitPairs = new ArrayList<>();
//        ArrayList<MCMEdge> assignPairs = new ArrayList<>();
//        for (int i = 0; i < itemPairs.size(); i++) {
//            if (i < itemPairs.size() / 3) {
//                splitPairs.add(itemPairs.get(i));
//            } else {
//                assignPairs.add(itemPairs.get(i));
//            }
//        }

        for (MCMEdge splitPair : itemPairs) {

            if (itemPairRemovalList.contains(splitPair)) { continue; }

            int itemOne = splitPair.getVertexOne();
            int itemTwo = splitPair.getVertexTwo();
            ArrayList<MCMEdge> potentialItemOnePairs = new ArrayList<>();
            ArrayList<MCMEdge> potentialItemTwoPairs = new ArrayList<>();

            for (MCMEdge potentialTargetPair : itemPairs) {

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

            this.assignItemsToPairs(potentialItemOnePairs, potentialItemTwoPairs, splitPair, completelyFilledStacks,
                    itemPairRemovalList, itemOne, itemTwo);
        }
    }

    /**
     * Tries to merge item pairs by splitting up pairs and assigning both items
     * to other pairs to form completely filled stacks.
     *
     * @param itemPairs - the item pairs to be merged
     */
    public ArrayList<ArrayList<ArrayList<Integer>>> mergeItemPairs(ArrayList<MCMEdge> itemPairs, int numberOfItemPairOrders) {
        ArrayList<ArrayList<MCMEdge>> itemPairRemovalLists = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> listOfCompletelyFilledStacks = new ArrayList<>();

        ArrayList<ArrayList<MCMEdge>> itemPairLists = new ArrayList<>();
        itemPairLists.add(itemPairs);

        for (int i = 0; i < numberOfItemPairOrders; i++) {
            itemPairRemovalLists.add(new ArrayList<>());
            listOfCompletelyFilledStacks.add(new ArrayList<>());
            itemPairLists.add(HeuristicUtil.getCopyOfEdgeList(itemPairs));
        }

        // sort item pairs here based on rating
        RatingSystem.assignNewRatingToEdges(itemPairLists.get(1), this.instance.getStackingConstraints(), this.instance.getCosts(), this.instance.getStacks());
        RatingSystem.assignMinRatingToEdges(itemPairLists.get(2), this.instance.getStackingConstraints());
        RatingSystem.assignMaxRatingToEdges(itemPairLists.get(3), this.instance.getStackingConstraints());
        RatingSystem.assignSumRatingToEdges(itemPairLists.get(4), this.instance.getStackingConstraints());
        RatingSystem.assignColRatingToEdgesNewWay(itemPairLists.get(5), this.instance.getStackingConstraints());
        RatingSystem.assignColRatingToEdges(itemPairLists.get(6), this.instance.getStackingConstraints());
        RatingSystem.assignRowRatingToEdges(itemPairLists.get(7), this.instance.getStackingConstraints());
        RatingSystem.assignNewRatingToEdges(itemPairLists.get(8), this.instance.getStackingConstraints(), this.instance.getCosts(), this.instance.getStacks());
        RatingSystem.assignMinRatingToEdges(itemPairLists.get(9), this.instance.getStackingConstraints());
        RatingSystem.assignMaxRatingToEdges(itemPairLists.get(10), this.instance.getStackingConstraints());
        RatingSystem.assignSumRatingToEdges(itemPairLists.get(11), this.instance.getStackingConstraints());
        RatingSystem.assignColRatingToEdgesNewWay(itemPairLists.get(12), this.instance.getStackingConstraints());
        RatingSystem.assignColRatingToEdges(itemPairLists.get(13), this.instance.getStackingConstraints());
        RatingSystem.assignRowRatingToEdges(itemPairLists.get(14), this.instance.getStackingConstraints());

        for (int i = 1; i < numberOfItemPairOrders; i++) {
            Collections.sort(itemPairLists.get(i));
            if (i >= 8 && i < 15) {
                Collections.reverse(itemPairLists.get(i));
            }
            if (i >= 15) {
                Collections.shuffle(itemPairLists.get(i));
            }
        }

        for (int i = 0; i < numberOfItemPairOrders; i++) {
            this.generateCompletelyFilledStacks(itemPairLists.get(i), itemPairRemovalLists.get(i), listOfCompletelyFilledStacks.get(i));
        }

        return listOfCompletelyFilledStacks;
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
     * Generates compatible item triples that are stackable in at least one order.
     *
     * @param itemPairs - the item pairs to be extended to triples
     * @return the generated item triples
     */
    public ArrayList<ArrayList<Integer>> generateItemTriples(ArrayList<MCMEdge> itemPairs) {
        ArrayList<Integer> unmatchedItems = new ArrayList<>(HeuristicUtil.getUnmatchedItemsFromPairs(
                itemPairs, this.instance.getItems())
        );
        return this.computeCompatibleItemTriples(itemPairs, unmatchedItems);
    }

    /**
     * Solves the given instance of the stacking problem. The objective is to minimize the transport costs
     * which is achieved by computing a min-cost-perfect-matching in the end.
     *
     * Basic idea:
     *      - generate item pairs via MCM
     *      - generate item triples via MCM
     *      - merge remaining pairs to triples
     *      - generate pairs again via MCM
     *      - consider remaining items to be unmatched
     *      - generate bipartite graph (items, stacks)
     *      - compute MWPM and interpret edges as stack assignments
     *      - fix order of items inside the stacks
     *
     * @return the solution generated by the heuristic
     */
    public Solution solve(int numberOfItemPairOrdersInMergeStep) {

        Solution sol = new Solution();
        Solution bestSol = new Solution();

        if (this.instance.getStackCapacity() == 3) {

            this.startTime = System.currentTimeMillis();

            ArrayList<MCMEdge> itemPairs = this.generateItemPairs(this.instance.getItems());

            // TODO: check whether triples is always empty here! are there cases with unmatched items at all?
            ArrayList<ArrayList<Integer>> triples = this.generateItemTriples(itemPairs);

            // items that are not part of a triple
            ArrayList<Integer> unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(
                    triples, this.instance.getItems()
            );
            // build pairs again from the unmatched items
            itemPairs = this.generateItemPairs(HeuristicUtil.getItemArrayFromItemList(unmatchedItems));

            ArrayList<ArrayList<ArrayList<Integer>>> listOfTriples = new ArrayList<>();
            for (int i = 0; i < numberOfItemPairOrdersInMergeStep; i++) {
                listOfTriples.add(new ArrayList<>());
            }
            for (int i = 0; i < listOfTriples.size(); i++) {
                listOfTriples.get(i).addAll(triples);
            }

            // The remaining unmatched items are not assignable to the pairs,
            // therefore pairs are merged together to form triples if possible.

            for (int i = 0; i < listOfTriples.size(); i++) {
                listOfTriples.get(i).addAll(this.mergeItemPairs(itemPairs, numberOfItemPairOrdersInMergeStep).get(i));
            }

            ArrayList<Solution> solutions = new ArrayList<>();

            for (ArrayList<ArrayList<Integer>> tri : listOfTriples) {

                this.instance.resetStacks();

                // update unmatched items
                unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(tri, this.instance.getItems());

                // items that are stored as pairs
                itemPairs = this.generateItemPairs(HeuristicUtil.getItemArrayFromItemList(unmatchedItems));

                // items that are stored in their own stack
                unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriplesAndPairs(
                        tri, itemPairs, this.instance.getItems()
                );

                // unable to assign the items feasibly to the given number of stacks
                if (tri.size() + itemPairs.size() + unmatchedItems.size() > this.instance.getStacks().length) {
                    return sol;
                }

                BipartiteGraph bipartiteGraph = this.generateBipartiteGraph(tri, itemPairs, unmatchedItems);
                KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
                        new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                                bipartiteGraph.getGraph(), bipartiteGraph.getPartitionOne(), bipartiteGraph.getPartitionTwo()
                        )
                        ;
                GraphUtil.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());

                sol = new Solution((System.currentTimeMillis() - startTime) / 1000.0, this.timeLimit, this.instance);
                sol.transformStackAssignmentsIntoValidSolutionIfPossible();
                solutions.add(new Solution(sol));
            }

            bestSol = new Solution(sol);
            for (Solution s : solutions) {
                if (s.computeCosts() < bestSol.computeCosts()) {
                    bestSol = s;
                }
            }
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 3.");
        }
        return bestSol;
    }
}
