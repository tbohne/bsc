package SP.constructive_heuristics;

import SP.representations.BipartiteGraph;
import SP.representations.Instance;
import SP.representations.StorageAreaPosition;
import SP.util.GraphUtil;
import org.jgrapht.alg.matching.KuhnMunkresMinimalWeightBipartitePerfectMatching;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class LowerBoundsSolver {

    private Instance instance;

    /**
     * Constructor
     *
     * @param instance  - the instance of the stacking problem to be solved
     */
    public LowerBoundsSolver(Instance instance) {
        this.instance = instance;
    }

    public void addEdges(
        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph,
        ArrayList<StorageAreaPosition> positions,
        ArrayList<Integer> itemList
    ) {
        for (int item : itemList) {
            for (StorageAreaPosition pos : positions) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("item" + item, "pos" + pos);
                double costs = this.instance.getCosts()[item][pos.getStackIdx()];
                bipartiteGraph.setEdgeWeight(edge, costs);
            }
        }
    }

    public void addEdgesForDummyItems(
            DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> bipartiteGraph,
            ArrayList<Integer> dummyItems,
            ArrayList<StorageAreaPosition> positions
    ) {
        for (int item : dummyItems) {
            for (StorageAreaPosition pos : positions) {
                DefaultWeightedEdge edge = bipartiteGraph.addEdge("dummy" + item, "pos" + pos);
                bipartiteGraph.setEdgeWeight(edge, 0);
            }
        }
    }

    public BipartiteGraph generateBipartiteGraph() {

        DefaultUndirectedWeightedGraph<String, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(
            DefaultWeightedEdge.class
        );

        Set<String> partitionOne = new HashSet<>();
        Set<String> partitionTwo = new HashSet<>();

        ArrayList<StorageAreaPosition> positions = new ArrayList<>();
        for (int stack = 0; stack < this.instance.getStacks().length; stack++) {
            for (int level = 0; level < this.instance.getStacks()[stack].length; level++) {
                positions.add(new StorageAreaPosition(stack, level));
            }
        }
        ArrayList<Integer> itemList = new ArrayList<>();
        for (int item : this.instance.getItems()) {
            itemList.add(item);
        }
        GraphUtil.addVerticesForUnmatchedItems(itemList, graph, partitionOne);
        GraphUtil.addVerticesForEmptyPositions(positions, graph, partitionTwo);
        ArrayList<Integer> dummyItems = GraphUtil.introduceDummyVertices(graph, partitionOne, partitionTwo);
        this.addEdgesForDummyItems(graph, dummyItems, positions);

        this.addEdges(graph, positions, itemList);

        return new BipartiteGraph(partitionOne, partitionTwo, graph);
    }

    public void computeLowerBound() {
        BipartiteGraph bipartiteGraph = this.generateBipartiteGraph();
        KuhnMunkresMinimalWeightBipartitePerfectMatching<String, DefaultWeightedEdge> minCostPerfectMatching =
            new KuhnMunkresMinimalWeightBipartitePerfectMatching<>(
                bipartiteGraph.getGraph(), bipartiteGraph.getPartitionOne(), bipartiteGraph.getPartitionTwo()
            )
        ;
        System.out.println("LB (with relaxed s_ij): " + minCostPerfectMatching.getMatching().getWeight());
    }
}
