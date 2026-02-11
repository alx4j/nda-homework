package nda.company.homework.routing.preload;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable in-memory representation of countries and land borders.
 */
public final class CountryGraph {
    private static final int UNKNOWN_NODE_ID = -1;

    private final Map<String, Integer> nodeIdByCode;
    private final String[] codeByNodeId;
    private final int[][] adjacencyByNodeId;
    private final int[] componentByNodeId;
    private final int edgeCount;
    private final int componentCount;

    /**
     * Creates a graph from precomputed structures.
     * <p>
     * The constructor assumes the inputs are already stable and will not be mutated afterwards.
     *
     * @param nodeIdByCode map from CCA3 code to internal node id
     * @param codeByNodeId array mapping internal node id back to CCA3 code
     * @param adjacencyByNodeId undirected adjacency list by node id
     * @param componentByNodeId connected-component id per node
     */
    public CountryGraph(
            Map<String, Integer> nodeIdByCode,
            String[] codeByNodeId,
            int[][] adjacencyByNodeId,
            int[] componentByNodeId
    ) {
        this.nodeIdByCode = Collections.unmodifiableMap(nodeIdByCode);
        this.codeByNodeId = codeByNodeId;
        this.adjacencyByNodeId = adjacencyByNodeId;
        this.componentByNodeId = componentByNodeId;

        if (this.codeByNodeId.length != this.adjacencyByNodeId.length) {
            throw new IllegalArgumentException("Code and adjacency arrays must have the same length");
        }
        if (this.codeByNodeId.length != this.componentByNodeId.length) {
            throw new IllegalArgumentException("Code and component arrays must have the same length");
        }

        this.edgeCount = computeEdgeCount(this.adjacencyByNodeId);
        this.componentCount = computeComponentCount(this.componentByNodeId);
    }

    /**
     * @return number of countries represented in this graph
     */
    public int nodeCount() {
        return codeByNodeId.length;
    }

    /**
     * @return number of undirected border edges
     */
    public int edgeCount() {
        return edgeCount;
    }

    /**
     * @return number of connected components in the graph
     */
    public int componentCount() {
        return componentCount;
    }

    /**
     * Resolves a CCA3 code to the internal node id.
     *
     * @param code CCA3 country code
     * @return node id, or {@code -1} when code is unknown
     */
    public int idByCode(String code) {
        Integer nodeId = nodeIdByCode.get(code);
        return nodeId == null ? UNKNOWN_NODE_ID : nodeId;
    }

    /**
     * Resolves a node id to its CCA3 code.
     *
     * @param nodeId internal node id
     * @return CCA3 code for the node
     */
    public String codeById(int nodeId) {
        validateNodeId(nodeId);
        return codeByNodeId[nodeId];
    }

    /**
     * Returns precomputed component id for a node.
     *
     * @param nodeId internal node id
     * @return connected-component id
     */
    public int componentOf(int nodeId) {
        validateNodeId(nodeId);
        return componentByNodeId[nodeId];
    }

    /**
     * Returns direct neighbors of the node.
     * <p>
     * The returned array is backed by precomputed graph data and must be treated as read-only.
     *
     * @param nodeId internal node id
     * @return neighboring node ids
     */
    public int[] neighborsOf(int nodeId) {
        validateNodeId(nodeId);
        return adjacencyByNodeId[nodeId];
    }

    /**
     * Returns number of neighbors for a node.
     *
     * @param nodeId internal node id
     * @return node degree in the undirected border graph
     */
    public int degreeOf(int nodeId) {
        return neighborsOf(nodeId).length;
    }

    /**
     * Returns one neighbor by adjacency index.
     *
     * @param nodeId internal node id
     * @param index index in node adjacency array
     * @return neighboring node id
     */
    public int neighborAt(int nodeId, int index) {
        int[] neighbors = neighborsOf(nodeId);
        Objects.checkIndex(index, neighbors.length);
        return neighbors[index];
    }

    private static int computeEdgeCount(int[][] adj) {
        int adjacencyEntries = 0;
        for (int[] neighbors : adj) {
            adjacencyEntries += neighbors.length;
        }
        return adjacencyEntries / 2;
    }

    private static int computeComponentCount(int[] componentId) {
        int maxComponent = -1;
        for (int id : componentId) {
            if (id > maxComponent) {
                maxComponent = id;
            }
        }
        return maxComponent + 1;
    }

    private void validateNodeId(int nodeId) {
        Objects.checkIndex(nodeId, codeByNodeId.length);
    }

}
