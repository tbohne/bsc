package SP.constructive_heuristics;

import SP.representations.*;
import SP.util.GraphUtil;
import SP.util.HeuristicUtil;
import SP.util.RatingSystem;
import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
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
    private double timeLimit;

    private int thresholdLB = 20;
    private int thresholdUB = 75;

    /**
     * Constructor
     *
     * @param instance  - the instance of the stacking problem to be solved
     * @param timeLimit - the time limit for the solving procedure
     */
    public ThreeCapHeuristic(Instance instance, double timeLimit) {
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
     * Parses the given minimum weight perfect matching and assigns it to the given stacks.
     *
     * @param mwpm   - the minimum weight perfect matching to be parsed
     * @param stacks - the stacks the parsed items are going to be assigned to
     */
    public void parseAndAssignMinCostPerfectMatching(KuhnMunkresMinimalWeightBipartitePerfectMatching mwpm, int[][] stacks) {
        for (Object edge : mwpm.getMatching().getEdges()) {
            if (edge.toString().contains("triple")) {
                int stack = GraphUtil.parseStackForTriple((DefaultWeightedEdge) edge);
                stacks[stack][2] = GraphUtil.parseItemOneOfTriple((DefaultWeightedEdge) edge);
                stacks[stack][1] = GraphUtil.parseItemTwoOfTriple((DefaultWeightedEdge) edge);
                stacks[stack][0] = GraphUtil.parseItemThreeOfTriple((DefaultWeightedEdge) edge);
            } else if (edge.toString().contains("pair")) {
                int stack = GraphUtil.parseStackForPair((DefaultWeightedEdge) edge);
                stacks[stack][2] = GraphUtil.parseItemOneOfPairBasedOnMatching((DefaultWeightedEdge) edge);
                stacks[stack][1] = GraphUtil.parseItemTwoOfPairBasedOnMatching((DefaultWeightedEdge) edge);
            } else if (edge.toString().contains("item")) {
                int stack = GraphUtil.parseStack((DefaultWeightedEdge) edge);
                stacks[stack][2] = GraphUtil.parseItem((DefaultWeightedEdge) edge);
            }
        }
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
     * Generates item triples based on the given list of item pairs.
     * The first x item pairs get splitted and the resulting items are assigned to the remaining
     * itemPairs.size() - x pairs to build up item triples.
     * The maximum number of item pairs that could be splitted and theoretically reassigned to the remaining pairs is
     * itemPairs.size() / 3. However, because of the stacking constraints there should be more options to assign
     * the items to. Therefore x = itemPairs.size() / 3.4 is used. The 3.4 is the result of an experimental comparison
     * of the results for different x-values.
     *
     * @param itemPairs   - the pairs of items to be merged
     * @param itemTriples - the list of triples to be generated
     */
    public void mergeItemPairs(ArrayList<MCMEdge> itemPairs, ArrayList<ArrayList<Integer>> itemTriples) {
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
        itemTriples.addAll(this.computeCompatibleItemTriples(assignPairs, splittedItems));
    }

    /**
     * Applies the 3Cap rating system to the lists of item pairs using different deviation thresholds.
     * The rating systems are used to sort the lists.
     *
     * @param itemPairLists - the lists of item pairs to be rated
     * @param itemPairs     - the initial list of item pairs
     * @return the number of applied rating systems
     */
    public void applyRatingSystems(ArrayList<ArrayList<MCMEdge>> itemPairLists, ArrayList<MCMEdge> itemPairs) {

        // the first list (idx 0) should be unrated and unsorted
        int ratingSystemIdx = 1;

        for (int deviationThreshold = this.thresholdLB; deviationThreshold < this.thresholdUB; deviationThreshold += 5) {

            if (itemPairLists.size() == ratingSystemIdx) {
                itemPairLists.add(HeuristicUtil.getCopyOfEdgeList(itemPairs));
            }
            RatingSystem.assignThreeCapPairRating(
                itemPairLists.get(ratingSystemIdx++),
                this.instance.getStackingConstraints(),
                this.instance.getCosts(),
                this.instance.getStacks(),
                deviationThreshold
            );
        }
    }

    /**
     * Sorts the lists of item pairs based on their ratings.
     *
     * @param itemPairLists - the lists of items pairs to be sorted
     */
    public void sortItemPairListsBasedOnRatings(ArrayList<ArrayList<MCMEdge>> itemPairLists) {
        for (int i = 0; i < itemPairLists.size(); i++) {
            // The first list is supposed to be unsorted.
            if (i != 0) {
                Collections.sort(itemPairLists.get(i));
            }
            Collections.reverse(itemPairLists.get(i));
        }
    }

    /**
     * Generates lists of completely filled stacks by merging the given item pairs to triples.
     * The given item pairs are sorted based on different rating systems which leads to a number
     * of different lists of item triples.
     *
     * @param itemPairs         - the list of item pairs to be merged in different ways
     * @param prioritizeRuntime - determines whether runtime is prioritized instead of solution quality
     */
    public ArrayList<ArrayList<ArrayList<Integer>>> mergePairsToTriples(ArrayList<MCMEdge> itemPairs, boolean prioritizeRuntime) {
        ArrayList<ArrayList<ArrayList<Integer>>> listsOfItemTriples = new ArrayList<>();
        ArrayList<ArrayList<MCMEdge>> listsOfItemPairs = new ArrayList<>();
        listsOfItemPairs.add(itemPairs);

        // If the runtime is prioritized, the rating system, which in some cases can increases
        // the solution quality at the cost of a longer runtime, will not be used.
        if (!prioritizeRuntime) {
            this.applyRatingSystems(listsOfItemPairs, itemPairs);
        }
        this.sortItemPairListsBasedOnRatings(listsOfItemPairs);

        for (int i = 0; i < listsOfItemPairs.size(); i++) {
            if (i >= listsOfItemTriples.size()) {
                listsOfItemTriples.add(new ArrayList<>());
            }
            this.mergeItemPairs(listsOfItemPairs.get(i), listsOfItemTriples.get(i));
        }
        return listsOfItemTriples;
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
        return GraphUtil.parseItemPairsFromMCM(itemMatching);
    }

    /**
     * Generates solutions to the stacking problem based on the different lists of item triples.
     *
     * Basic idea:
     *      - generate pairs via MCM
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
            this.parseAndAssignMinCostPerfectMatching(minCostPerfectMatching, this.instance.getStacks());

            Solution sol = new Solution((System.currentTimeMillis() - startTime) / 1000.0, this.timeLimit, this.instance);
            solutions.add(new Solution(sol));
        }
        return solutions;
    }

    /**
     * Returns a list of the remaining levels in a stack based on the specified level.
     *
     * @param level - level used to determine the remaining levels
     * @return list of remaining levels in the stack
     */
    public ArrayList<Integer> getLevelsOfOtherSlots(int level) {
        ArrayList<Integer> levelsOfOtherSlots = new ArrayList<>();
        for (int i = 0; i < this.instance.getStackCapacity(); i++) {
            if (i != level) {
                levelsOfOtherSlots.add(i);
            }
        }
        return levelsOfOtherSlots;
    }

    /**
     * Adds edges to the post-processing graph for stacks containing a single item.
     *
     * @param sol                 - the solution to be processed
     * @param emptyPos            - empty positions the compatible items get connected with
     * @param levelsOfOtherSlots  - the levels the items of the pair are positioned at
     * @param item                - the item potentially to be added to the stack
     * @param postProcessingGraph - graph the edges are added to
     * @param originalCosts       - the current costs for each item assignment
     */
    public void addEdgesForStacksWithSingleItem(
        Solution sol,
        StackPosition emptyPos,
        ArrayList<Integer> levelsOfOtherSlots,
        int item,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
        HashMap<Integer, Double> originalCosts
    ) {

        int levelOfOtherSlot;
        if (sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1) {
            levelOfOtherSlot = levelsOfOtherSlots.get(0);
        } else {
            levelOfOtherSlot = levelsOfOtherSlots.get(1);
        }
        int otherItem = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelOfOtherSlot];

        // check pair for compatibility
        if (this.instance.getStackingConstraints()[item][otherItem] == 1
            || this.instance.getStackingConstraints()[otherItem][item] == 1) {
                GraphUtil.addEdgesToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
        }
    }

    /**
     * Adds edges to the post-processing graph for stacks already containing a pair of items.
     *
     * @param levelsOfOtherSlots  - the levels the items of the pair are positioned at
     * @param sol                 - the solution to be processed
     * @param emptyPos            - empty positions the compatible items get connected with
     * @param postProcessingGraph - graph the edges are added to
     * @param item                - the item potentially to be added to the stack
     * @param originalCosts       - the current costs for each item assignment
     */
    public void addEdgesForStacksFilledWithPairs(
        ArrayList<Integer> levelsOfOtherSlots,
        Solution sol,
        StackPosition emptyPos,
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
        int item,
        HashMap<Integer, Double> originalCosts
    ) {

        int lowerItemOfPair;
        int upperItemOfPair;

        if (levelsOfOtherSlots.get(0) < levelsOfOtherSlots.get(1)) {
            upperItemOfPair = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)];
            lowerItemOfPair = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)];
        } else {
            upperItemOfPair = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)];
            lowerItemOfPair = sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)];
        }

        // pair stackable in both directions
        if (this.instance.getStackingConstraints()[lowerItemOfPair][upperItemOfPair] == 1
            && this.instance.getStackingConstraints()[upperItemOfPair][lowerItemOfPair] == 1) {

            if (GraphUtil.checkWhetherItemCanBeAssignedToPairStackableInBothDirections(
                this.instance.getStackingConstraints(), lowerItemOfPair, upperItemOfPair, item
            )) {
                GraphUtil.addEdgesToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
            }
        // pair stackable in one direction
        } else {
            if (GraphUtil.checkWhetherItemCanBeAssignedToPairStackableInOneDirection(
                this.instance.getStackingConstraints(), lowerItemOfPair, upperItemOfPair, item
            )) {
                GraphUtil.addEdgesToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
            }
        }
    }

    /**
     * Finds compatible empty positions for the items in the storage area and
     * initiates the edge addition for these items and empty positions
     * to the post-processing graph.
     *
     * @param postProcessingGraph - graph the edges are added to
     * @param items               - items to be connected to compatible empty positions
     * @param emptyPositions      - empty positions the compatible items get connected with
     * @param originalCosts       - the current costs for each item assignment
     * @param sol                 - the solution to be processed
     */
    public void findCompatibleEmptyPositionsForItemsAndAddEdges(
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph,
        ArrayList<Integer> items,
        ArrayList<StackPosition> emptyPositions,
        HashMap<Integer, Double> originalCosts,
        Solution sol
    ) {

        for (int item : items) {
            for (StackPosition emptyPos : emptyPositions) {

                // item compatible with stack
                if (this.instance.getCosts()[item][emptyPos.getStackIdx()] < Integer.MAX_VALUE / this.instance.getItems().length) {

                    // has always two entries
                    ArrayList<Integer> levelsOfOtherSlots = this.getLevelsOfOtherSlots(emptyPos.getLevel());

                    // two other items in stack
                    if (sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1
                        && sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)] != -1) {
                            this.addEdgesForStacksFilledWithPairs(
                                levelsOfOtherSlots, sol, emptyPos, postProcessingGraph, item, originalCosts
                            );
                    // one other item in stack
                    } else if (sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(0)] != -1
                        || sol.getFilledStorageArea()[emptyPos.getStackIdx()][levelsOfOtherSlots.get(1)] != -1) {
                            this.addEdgesForStacksWithSingleItem(
                                sol, emptyPos, levelsOfOtherSlots, item, postProcessingGraph, originalCosts
                            );
                    // no other item in stack
                    } else {
                        GraphUtil.addEdgesToPostProcessingGraph(postProcessingGraph, item, emptyPos, originalCosts, this.instance);
                    }
                }
            }
        }
    }

    /**
     * Generates the bipartite graph for the post-processing step.
     * The graph's first partition consists of items and the second one consists of empty positions in stacks.
     * An edge between the partitions means that an item is compatible to the empty position.
     * The costs of the edges correspond to the savings that are generated if the item is assigned to the empty position.
     *
     * @param items          - list of items in the storage area
     * @param emptyPositions - list of empty positions in the storage area
     * @param originalCosts  - original costs for each item assignment
     * @param sol            - solution to be processed
     * @return the generated bipartite graph
     */
    public BipartiteGraph generatePostProcessingGraph(
        ArrayList<Integer> items,
        ArrayList<StackPosition> emptyPositions,
        HashMap<Integer, Double> originalCosts,
        Solution sol
    ) {

        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> postProcessingGraph = new DefaultUndirectedWeightedGraph<>(
            DefaultWeightedEdge.class
        );
        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        GraphUtil.addVerticesForUnmatchedItems(items, postProcessingGraph, partitionOne);
        GraphUtil.addVerticesForEmptyPositions(emptyPositions, postProcessingGraph, partitionTwo);
        this.findCompatibleEmptyPositionsForItemsAndAddEdges(postProcessingGraph, items, emptyPositions, originalCosts, sol);

        return new BipartiteGraph(partitionOne, partitionTwo, postProcessingGraph);
    }

    /**
     * Retrieves all the tuples of items from the storage area of the original solution.
     *
     * @param sol - the original solution
     * @param tupleSize - the size of the tuples to be retrieved
     * @return the list of item pairs
     */
    public ArrayList<ArrayList<Integer>> retrieveItemTuples(Solution sol, int tupleSize) {
        ArrayList<ArrayList<Integer>> itemTuples = new ArrayList<>();
        for (int stack = 0; stack < sol.getFilledStorageArea().length; stack++) {
            ArrayList<Integer> stackEntries = new ArrayList<>();
            for (int entry : sol.getFilledStorageArea()[stack]) {
                stackEntries.add(entry);
            }
            ArrayList<Integer> itemTuple = new ArrayList<>();
            if (Collections.frequency(stackEntries, -1) == this.instance.getStackCapacity() - tupleSize) {
                for (int entry : stackEntries) {
                    if (entry != -1) {
                        itemTuple.add(entry);
                    }
                }
            }
            if (itemTuple.size() == tupleSize) {
                itemTuples.add(itemTuple);
            }
        }
        return itemTuples;
    }

    /**
     * Restores the storage area of the specified solution.
     * Since the last generated solution is not necessarily the best one,
     * in many cases the storage area in the instance for the best original solution has to be restored.
     *
     * @param sol - the solution to restore the storage area from
     */
    public void restoreStorageAreaForBestOriginalSolution(Solution sol) {
        for (int i = 0; i < sol.getFilledStorageArea().length; i++) {
            this.instance.getStacks()[i] = sol.getFilledStorageArea()[i].clone();
        }
    }

    /**
     * This approach should be used in situations where the number of used stacks is irrelevant
     * and only the minimization of transport costs matters.
     *
     * Takes the generated solution and moves items to empty positions if it's possible to
     * reduce the total costs by doing so.
     * Initially, a bipartite graph between items and empty positions is generated.
     * An edge between the two partitions indicates that an item is assignable to the empty position.
     * The costs of the edge correspond to the resulting savings if the item is assigned to the empty position.
     * A maximum weight matching is computed on that graph to retrieve an assignment that leads
     * to a maximum cost reduction.
     *
     * @param sol - the original solution
     * @return the result of the post-processing procedure
     */
    public Solution postProcessing(Solution sol) {
//        System.out.println("costs before post processing: " + sol.getObjectiveValue());

        ArrayList<StackPosition> emptyPositions = HeuristicUtil.retrieveEmptyPositions(sol);
        ArrayList<Integer> items = sol.getAssignedItems();
        HashMap<Integer, Double> originalCosts = HeuristicUtil.getOriginalCosts(sol, this.instance.getCosts());
        BipartiteGraph postProcessingGraph = this.generatePostProcessingGraph(items, emptyPositions, originalCosts, sol);
        MaximumWeightBipartiteMatching<String, DefaultWeightedEdge> maxSavingsMatching = new MaximumWeightBipartiteMatching<>(
            postProcessingGraph.getGraph(), postProcessingGraph.getPartitionOne(), postProcessingGraph.getPartitionTwo()
        );

//        System.out.println("moved items: " + maxSavingsMatching.getMatching().getEdges().size());
        HeuristicUtil.updateStackAssignments(maxSavingsMatching, postProcessingGraph, this.instance);
        sol = new Solution((System.currentTimeMillis() - this.startTime) / 1000.0, this.timeLimit, this.instance);
        sol.lowerItemsThatAreStackedInTheAir();
        sol.transformStackAssignmentsIntoValidSolutionIfPossible();
//        System.out.println("costs after post processing: " + sol.getObjectiveValue() + " still feasible ? " + sol.isFeasible());
        return sol;
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
     *      - post-processing
     *
     * @param prioritizeRuntime - determines whether runtime is prioritized instead of solution quality
     * @param postProcessing - determines whether or not the post-processing step should be executed
     * @return the solution generated by the heuristic
     */
    public Solution solve(boolean prioritizeRuntime, boolean postProcessing) {

        Solution bestSol = new Solution();

        if (this.instance.getStackCapacity() == 3) {
            this.startTime = System.currentTimeMillis();
            ArrayList<MCMEdge> itemPairs = this.generateItemPairs(this.instance.getItems());

            // TODO: remove dbg logs
            ///////////////////////////////////////////////////////////////////////////////
            ///////////////////////////////////////////////////////////////////////////////
            int cnt = 0;
            for (MCMEdge e : itemPairs) {
                if (this.instance.getStackingConstraints()[e.getVertexOne()][e.getVertexTwo()] == 1
                        && this.instance.getStackingConstraints()[e.getVertexTwo()][e.getVertexOne()] == 1) {
                    cnt++;
                }
            }
//            System.out.println("pairs: " + itemPairs.size() + " both dir: " + cnt);
            ///////////////////////////////////////////////////////////////////////////////
            ///////////////////////////////////////////////////////////////////////////////

            ArrayList<ArrayList<ArrayList<Integer>>> itemTripleLists = this.mergePairsToTriples(itemPairs, prioritizeRuntime);
            ArrayList<Solution> solutions = this.generateSolutionsBasedOnListsOfTriples(itemTripleLists);
            bestSol = HeuristicUtil.getBestSolution(solutions);

            bestSol.transformStackAssignmentsIntoValidSolutionIfPossible();
            bestSol.setTimeToSolve((System.currentTimeMillis() - this.startTime) / 1000.0);
            bestSol.lowerItemsThatAreStackedInTheAir();

            if (postProcessing) {
                // Since the last generated solution is not necessarily the best one,
                // the stack assignments for the best solution have to be restored before post-processing.
                this.instance.resetStacks();
                this.restoreStorageAreaForBestOriginalSolution(bestSol);

                double bestSolutionCost = bestSol.computeCosts();
                bestSol = this.postProcessing(bestSol);
                while (bestSol.computeCosts() < bestSolutionCost) {
                    this.instance.resetStacks();
                    this.restoreStorageAreaForBestOriginalSolution(bestSol);
                    bestSolutionCost = bestSol.computeCosts();
                    bestSol = this.postProcessing(bestSol);
                }
            }
            System.out.println("final costs: " + bestSol.computeCosts() + " still feasible? " + bestSol.isFeasible());
        } else {
            System.out.println("This heuristic is designed to solve SP with a stack capacity of 3.");
        }
        bestSol.setTimeToSolve((System.currentTimeMillis() - this.startTime) / 1000.0);
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
            int[] items, ArrayList<MCMEdge> potentialTargetPairs, double[][] costs, int[][] stacks, int item
    ) {
        MCMEdge bestTargetPair = new MCMEdge(0, 0, 0);
        if (potentialTargetPairs.size() > 0) {
            bestTargetPair = potentialTargetPairs.get(0);
        }
        double minCost = Integer.MAX_VALUE / items.length;
        int costsForIncompatibleStack = Integer.MAX_VALUE / items.length;

        for (MCMEdge targetPair : potentialTargetPairs) {
            ArrayList<Double> costValues = new ArrayList<>();

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

        this.applyRatingSystems(itemPairLists, itemPairs);
        this.sortItemPairListsBasedOnRatings(itemPairLists);

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
