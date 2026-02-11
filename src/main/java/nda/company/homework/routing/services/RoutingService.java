package nda.company.homework.routing.services;

import nda.company.homework.routing.preload.CountryGraph;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Finds shortest land-border routes on the precomputed country graph.
 * Uses bidirectional BFS on an unweighted graph.
 */
@Service
public class RoutingService {
    private static final int UNKNOWN_ID = -1;

    private final CountryGraph countryGraph;

    public RoutingService(CountryGraph countryGraph) {
        this.countryGraph = countryGraph;
    }

    /**
     * Finds one shortest route between two countries.
     * <p>
     * Worst-case complexity for connected countries is {@code O(V + E)}.
     *
     * @param originCode origin country code (any case)
     * @param destinationCode destination country code (any case)
     * @return typed success or failure outcome
     */
    public RoutingResult findRoute(String originCode, String destinationCode) {
        String origin = normalizeCode(originCode);
        if (origin == null) {
            return RoutingResult.unknownCountry(originCode);
        }

        String destination = normalizeCode(destinationCode);
        if (destination == null) {
            return RoutingResult.unknownCountry(destinationCode);
        }

        int originId = countryGraph.idByCode(origin);
        if (originId == UNKNOWN_ID) {
            return RoutingResult.unknownCountry(origin);
        }

        int destinationId = countryGraph.idByCode(destination);
        if (destinationId == UNKNOWN_ID) {
            return RoutingResult.unknownCountry(destination);
        }

        if (originId == destinationId) {
            return RoutingResult.success(List.of(countryGraph.codeById(originId)));
        }

        // precomputed components let us reject impossible routes in O(1)
        if (countryGraph.componentOf(originId) != countryGraph.componentOf(destinationId)) {
            return RoutingResult.noLandRoute(origin, destination);
        }

        int[] path = findShortestPath(originId, destinationId);
        if (path == null) {
            return RoutingResult.noLandRoute(origin, destination);
        }

        return RoutingResult.success(toCountryCodes(path));
    }

    private String normalizeCode(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return null;
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private int[] findShortestPath(int originId, int destinationId) {
        int nodeCount = countryGraph.nodeCount();
        int[] parentFromOrigin = new int[nodeCount];
        int[] parentFromDestination = new int[nodeCount];
        Arrays.fill(parentFromOrigin, UNKNOWN_ID);
        Arrays.fill(parentFromDestination, UNKNOWN_ID);

        boolean[] visitedFromOrigin = new boolean[nodeCount];
        boolean[] visitedFromDestination = new boolean[nodeCount];

        ArrayDeque<Integer> originQueue = new ArrayDeque<>();
        ArrayDeque<Integer> destinationQueue = new ArrayDeque<>();

        visitedFromOrigin[originId] = true;
        parentFromOrigin[originId] = originId;
        originQueue.add(originId);

        visitedFromDestination[destinationId] = true;
        parentFromDestination[destinationId] = destinationId;
        destinationQueue.add(destinationId);

        while (!originQueue.isEmpty() && !destinationQueue.isEmpty()) {
            int meetingNode;
            // expanding the smaller frontier usually reduces explored nodes
            if (originQueue.size() <= destinationQueue.size()) {
                meetingNode = expandFrontier(
                        originQueue,
                        visitedFromOrigin,
                        visitedFromDestination,
                        parentFromOrigin
                );
            } else {
                meetingNode = expandFrontier(
                        destinationQueue,
                        visitedFromDestination,
                        visitedFromOrigin,
                        parentFromDestination
                );
            }

            if (meetingNode != UNKNOWN_ID) {
                return reconstructPath(
                        meetingNode,
                        originId,
                        destinationId,
                        parentFromOrigin,
                        parentFromDestination
                );
            }
        }

        return null;
    }

    private int expandFrontier(
            ArrayDeque<Integer> queue,
            boolean[] visitedThisSide,
            boolean[] visitedOtherSide,
            int[] parentThisSide
    ) {
        // process one full BFS level to preserve shortest-path guarantees
        int nodesOnLevel = queue.size();
        for (int i = 0; i < nodesOnLevel; i++) {
            int nodeId = queue.removeFirst();
            for (int neighborId : countryGraph.neighborsOf(nodeId)) {
                if (visitedThisSide[neighborId]) {
                    continue;
                }

                visitedThisSide[neighborId] = true;
                parentThisSide[neighborId] = nodeId;
                if (visitedOtherSide[neighborId]) {
                    return neighborId;
                }

                queue.addLast(neighborId);
            }
        }

        return UNKNOWN_ID;
    }

    private int[] reconstructPath(
            int meetingNode,
            int originId,
            int destinationId,
            int[] parentFromOrigin,
            int[] parentFromDestination
    ) {
        // build origin -> meeting segment by walking parent pointers backwards
        List<Integer> originSegment = new ArrayList<>();
        int nodeId = meetingNode;
        while (nodeId != originId) {
            originSegment.add(nodeId);
            nodeId = parentFromOrigin[nodeId];
        }
        originSegment.add(originId);
        Collections.reverse(originSegment);

        // continue from meeting towards destination (exclude meeting to avoid duplication)
        List<Integer> destinationSegment = new ArrayList<>();
        nodeId = meetingNode;
        while (nodeId != destinationId) {
            nodeId = parentFromDestination[nodeId];
            destinationSegment.add(nodeId);
        }

        int[] fullPath = new int[originSegment.size() + destinationSegment.size()];
        int index = 0;
        for (int originNode : originSegment) {
            fullPath[index++] = originNode;
        }
        for (int destinationNode : destinationSegment) {
            fullPath[index++] = destinationNode;
        }
        return fullPath;
    }

    private List<String> toCountryCodes(int[] pathByNodeId) {
        List<String> route = new ArrayList<>(pathByNodeId.length);
        for (int nodeId : pathByNodeId) {
            route.add(countryGraph.codeById(nodeId));
        }
        return route;
    }
}
