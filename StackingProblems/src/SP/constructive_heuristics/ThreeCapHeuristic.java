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

    public static MCMEdge getBestPairForItem(
            int[] items, ArrayList<MCMEdge> potentialTargetPairs, int[][] costs, int[][] stacks, int item
    ) {

        MCMEdge bestTargetPair = new MCMEdge(0, 0, 0);
        if (potentialTargetPairs.size() > 0) {
            bestTargetPair = potentialTargetPairs.get(0);
        }
        int bestCostVal = Integer.MAX_VALUE / items.length;
        int invalidCostFactor = Integer.MAX_VALUE / items.length;
        System.out.println("item " + item);

        for (MCMEdge targetPair : potentialTargetPairs) {
            ArrayList<Integer> costValues = new ArrayList<>();

            for (int stack = 0; stack < stacks.length; stack++) {
                if (costs[item][stack] < invalidCostFactor
                        && costs[targetPair.getVertexOne()][stack] < invalidCostFactor
                        && costs[targetPair.getVertexTwo()][stack] < invalidCostFactor
                        ) {

                    costValues.add(costs[item][stack] + costs[targetPair.getVertexOne()][stack] + costs[targetPair.getVertexTwo()][stack]);
                }
            }
            if (Collections.min(costValues) < bestCostVal) {
                bestCostVal = Collections.min(costValues);
                bestTargetPair = targetPair;
            }
        }

        return bestTargetPair;
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
            ArrayList<ArrayList<Integer>> completelyFilledStacks,
            int[][] stackingConstraints,
            int[][] stacks,
            int[][] costs,
            int[] items
    ) {
        for (MCMEdge splitPair : itemPairs) {

            if (itemPairRemovalList.contains(splitPair)) { continue; }

            int itemOne = splitPair.getVertexOne();
            int itemTwo = splitPair.getVertexTwo();
            boolean itemOneAssigned = false;
            boolean itemTwoAssigned = false;
            MCMEdge itemOneEdge;
            MCMEdge itemTwoEdge;

            ArrayList<MCMEdge> potentialItemOneEdges = new ArrayList<>();
            ArrayList<MCMEdge> potentialItemTwoEdges = new ArrayList<>();

            for (MCMEdge potentialTargetPair : itemPairs) {

                if (itemPairRemovalList.contains(potentialTargetPair)) { continue; }
//                if (itemOneAssigned && itemTwoAssigned) { break; }

                if (splitPair != potentialTargetPair) {
                    int potentialTargetPairItemOne = potentialTargetPair.getVertexOne();
                    int potentialTargetPairItemTwo = potentialTargetPair.getVertexTwo();

                    if (/*!itemOneAssigned*/ !potentialItemOneEdges.contains(potentialTargetPair)
                            && !potentialItemTwoEdges.contains(potentialTargetPair) && HeuristicUtil.itemAssignableToPair(
                            itemOne, potentialTargetPairItemOne, potentialTargetPairItemTwo, stackingConstraints)
                            ) {
//                        itemOneAssigned = true;
//                        itemOneEdge = potentialTargetPair;
                        potentialItemOneEdges.add(potentialTargetPair);
                        continue;
                    }
                    if (/*!itemTwoAssigned*/ !potentialItemOneEdges.contains(potentialTargetPair)
                            && !potentialItemTwoEdges.contains(potentialTargetPair) && HeuristicUtil.itemAssignableToPair(
                            itemTwo, potentialTargetPairItemOne, potentialTargetPairItemTwo, stackingConstraints)
                            ) {
//                        itemTwoAssigned = true;
//                        itemTwoEdge = potentialTargetPair;
                        potentialItemTwoEdges.add(potentialTargetPair);
                        continue;
                    }
                }
            }

            /////////////////////////////////////////////////////////////////////////////////////////////
            /////////////////////////////////////////////////////////////////////////////////////////////

            MCMEdge itemOneTargetPair = getBestPairForItem(items, potentialItemOneEdges, costs, stacks, itemOne);
            if (!(itemOneTargetPair.getVertexOne() == 0 && itemOneTargetPair.getVertexTwo() == 0)) {
                itemOneAssigned = true;
            }

            // find item two edge to use (all edges that are usable by two and not used by one should be added to potential two)
            for (MCMEdge pot : potentialItemOneEdges) {
                if (pot != itemOneTargetPair && HeuristicUtil.itemAssignableToPair(
                        itemTwo, pot.getVertexOne(), pot.getVertexTwo(), stackingConstraints)
                        ) {
                    potentialItemTwoEdges.add(pot);
                }
            }

            MCMEdge itemTwoTargetPair = getBestPairForItem(items, potentialItemTwoEdges, costs, stacks, itemTwo);

            if (!(itemTwoTargetPair.getVertexOne() == 0 && itemTwoTargetPair.getVertexTwo() == 0)) {
                itemTwoAssigned = true;
            }
            /////////////////////////////////////////////////////////////////////////////////////////////
            /////////////////////////////////////////////////////////////////////////////////////////////

            if (itemOneAssigned && itemTwoAssigned) {
                HeuristicUtil.updateCompletelyFilledStacks(
                        itemPairRemovalList, itemOneTargetPair, itemTwoTargetPair, splitPair, itemOne, itemTwo, completelyFilledStacks
                );
            }
        }
    }

    /**
     * Tries to merge item pairs by splitting up pairs and assigning both items
     * to other pairs to form completely filled stacks.
     *
     * @param itemPairs - the item pairs to be merged
     */
    public ArrayList<ArrayList<Integer>> mergeItemPairs(ArrayList<MCMEdge> itemPairs) {
        ArrayList<MCMEdge> itemPairRemovalList = new ArrayList<>();
        ArrayList<ArrayList<Integer>> completelyFilledStacks = new ArrayList<>();

        // sort item pairs here based on rating
//        RatingSystem.assignNewRatingToEdges(itemPairs, this.instance.getStackingConstraints());
//        Collections.sort(itemPairs);

        this.generateCompletelyFilledStacks(
            itemPairs, itemPairRemovalList, completelyFilledStacks, this.instance.getStackingConstraints(),
                this.instance.getStacks(), this.instance.getCosts(), this.instance.getItems()
        );
        return completelyFilledStacks;
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
    public Solution solve() {

        Solution sol = new Solution();

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

            // The remaining unmatched items are not assignable to the pairs,
            // therefore pairs are merged together to form triples if possible.
            triples.addAll(this.mergeItemPairs(itemPairs));

            // update unmatched items
            unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriples(triples, this.instance.getItems());

            // items that are stored as pairs
            itemPairs = this.generateItemPairs(HeuristicUtil.getItemArrayFromItemList(unmatchedItems));

            // items that are stored in their own stack
            unmatchedItems = HeuristicUtil.getUnmatchedItemsFromTriplesAndPairs(
                    triples, itemPairs, this.instance.getItems()
            );

            // unable to assign the items feasibly to the given number of stacks
            if (triples.size() + itemPairs.size() + unmatchedItems.size() > this.instance.getStacks().length) {
                return sol;
            }

            BipartiteGraph bipartiteGraph = this.generateBipartiteGraph(triples, itemPairs, unmatchedItems);
            KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
                new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                    bipartiteGraph.getGraph(), bipartiteGraph.getPartitionOne(), bipartiteGraph.getPartitionTwo()
                )
            ;
            GraphUtil.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());

            sol = new Solution((System.currentTimeMillis() - startTime) / 1000.0, this.timeLimit, this.instance);
            sol.transformStackAssignmentsIntoValidSolutionIfPossible();

        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 3.");
        }
        return sol;
    }
}
