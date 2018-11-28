import org.jgrapht.alg.matching.EdmondsMaximumCardinalityMatching;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

public class MatchingTest {

    public static void maximumCardinalityMatchingTest() {

        SimpleGraph<String, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);

        String[] vertices = {"v1", "v2", "v3", "v4", "v5", "v6", "v7"};

        for (String vertex : vertices) {
            graph.addVertex(vertex);
        }

        graph.addEdge(vertices[0], vertices[1]);
        graph.addEdge(vertices[1], vertices[2]);
        graph.addEdge(vertices[0], vertices[4]);
        graph.addEdge(vertices[2], vertices[4]);
        graph.addEdge(vertices[1], vertices[4]);
        graph.addEdge(vertices[0], vertices[3]);
        graph.addEdge(vertices[2], vertices[5]);
        graph.addEdge(vertices[3], vertices[4]);
        graph.addEdge(vertices[4], vertices[6]);
        graph.addEdge(vertices[3], vertices[6]);

        EdmondsMaximumCardinalityMatching<String, DefaultEdge> mcm = new EdmondsMaximumCardinalityMatching<>(graph);
        System.out.println(mcm.getMatching());
    }

    public static void main(String[] args) {
        maximumCardinalityMatchingTest();
    }
}