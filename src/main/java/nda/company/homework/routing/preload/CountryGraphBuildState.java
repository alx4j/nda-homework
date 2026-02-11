package nda.company.homework.routing.preload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Startup-only mutable builder for {@link CountryGraph}.
 * It assigns ids, stores unique undirected edges, unions components, and creates adjacency arrays.
 */
final class CountryGraphBuildState {
    private final Map<String, Integer> nodeIdByCode = new HashMap<>();
    private final List<String> codeByNodeId = new ArrayList<>();
    private final EdgePairList uniqueEdges = new EdgePairList();
    private final DisjointSetUnion connectedComponents = new DisjointSetUnion();
    private final Set<Long> seenUndirectedEdges = new HashSet<>();

    /**
     * Returns or creates a stable internal id for a country code.
     *
     * @param countryCode normalized CCA3 code
     * @return internal node id
     */
    int idFor(String countryCode) {
        return nodeIdByCode.computeIfAbsent(countryCode, code -> {
            int newId = connectedComponents.addElement();
            codeByNodeId.add(code);
            return newId;
        });
    }

    /**
     * Adds an undirected edge and unions its endpoints.
     * Duplicate edges and self-loops are ignored.
     *
     * @param leftId first node id
     * @param rightId second node id
     */
    void connectUndirected(int leftId, int rightId) {
        if (leftId == rightId) {
            return;
        }

        int lowerNodeId = Math.min(leftId, rightId);
        int higherNodeId = Math.max(leftId, rightId);
        long edgeKey = edgeKey(lowerNodeId, higherNodeId);
        if (!seenUndirectedEdges.add(edgeKey)) {
            return;
        }

        uniqueEdges.add(lowerNodeId, higherNodeId);
        connectedComponents.union(lowerNodeId, higherNodeId);
    }

    /**
     * Finalizes all accumulated structures into an immutable graph instance.
     *
     * @return precomputed country graph
     */
    CountryGraph toGraph() {
        String[] codeByNodeIdArray = codeByNodeId.toArray(String[]::new);
        int[] edgeFromNodeId = uniqueEdges.fromArray();
        int[] edgeToNodeId = uniqueEdges.toArray();
        int[] componentRootByNodeId = connectedComponents.snapshotParents();
        int[][] adjacencyByNodeId = buildAdjacency(codeByNodeIdArray.length, edgeFromNodeId, edgeToNodeId);
        int[] denseComponentByNodeId = buildComponentId(componentRootByNodeId);
        return new CountryGraph(nodeIdByCode, codeByNodeIdArray, adjacencyByNodeId, denseComponentByNodeId);
    }

    /**
     * Builds adjacency in two passes: degree count and fill.
     */
    private static int[][] buildAdjacency(int nodeCount, int[] edgeFromNodeId, int[] edgeToNodeId) {
        int[] degreeByNodeId = new int[nodeCount];
        for (int i = 0; i < edgeFromNodeId.length; i++) {
            degreeByNodeId[edgeFromNodeId[i]]++;
            degreeByNodeId[edgeToNodeId[i]]++;
        }

        int[][] adjacencyByNodeId = new int[nodeCount][];
        for (int nodeId = 0; nodeId < nodeCount; nodeId++) {
            adjacencyByNodeId[nodeId] = new int[degreeByNodeId[nodeId]];
        }

        int[] fillPositionByNodeId = new int[nodeCount];
        for (int i = 0; i < edgeFromNodeId.length; i++) {
            int leftNodeId = edgeFromNodeId[i];
            int rightNodeId = edgeToNodeId[i];
            adjacencyByNodeId[leftNodeId][fillPositionByNodeId[leftNodeId]++] = rightNodeId;
            adjacencyByNodeId[rightNodeId][fillPositionByNodeId[rightNodeId]++] = leftNodeId;
        }
        return adjacencyByNodeId;
    }

    /**
     * Remaps DSU roots to dense ids.
     */
    private static int[] buildComponentId(int[] componentRootByNodeId) {
        Map<Integer, Integer> denseComponentByRoot = new HashMap<>();
        int[] denseComponentByNodeId = new int[componentRootByNodeId.length];
        for (int nodeId = 0; nodeId < componentRootByNodeId.length; nodeId++) {
            int rootNodeId = componentRootByNodeId[nodeId];
            int denseComponentId = denseComponentByRoot.computeIfAbsent(
                    rootNodeId,
                    ignored -> denseComponentByRoot.size()
            );
            denseComponentByNodeId[nodeId] = denseComponentId;
        }
        return denseComponentByNodeId;
    }

    private static long edgeKey(int lowerNodeId, int higherNodeId) {
        return (((long) lowerNodeId) << 32) | (higherNodeId & 0xffffffffL);
    }
}
