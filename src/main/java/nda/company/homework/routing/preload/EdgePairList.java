package nda.company.homework.routing.preload;

import java.util.Arrays;

/**
 * Compact growable pair of primitive arrays used to store undirected edge endpoints.
 */
final class EdgePairList {
    private static final int INITIAL_CAPACITY = 256;

    private int[] fromNodeId = new int[INITIAL_CAPACITY];
    private int[] toNodeId = new int[INITIAL_CAPACITY];
    private int size;

    /**
     * Appends one edge endpoint pair.
     *
     * @param fromId source endpoint id
     * @param toId destination endpoint id
     */
    void add(int fromId, int toId) {
        ensureCapacity(size + 1);
        fromNodeId[size] = fromId;
        toNodeId[size] = toId;
        size++;
    }

    /**
     * @return compact copy of source endpoint ids
     */
    int[] fromArray() {
        return Arrays.copyOf(fromNodeId, size);
    }

    /**
     * @return compact copy of destination endpoint ids
     */
    int[] toArray() {
        return Arrays.copyOf(toNodeId, size);
    }

    private void ensureCapacity(int requiredCapacity) {
        if (requiredCapacity <= fromNodeId.length) {
            return;
        }
        int newCapacity = Math.max(fromNodeId.length * 2, requiredCapacity);
        fromNodeId = Arrays.copyOf(fromNodeId, newCapacity);
        toNodeId = Arrays.copyOf(toNodeId, newCapacity);
    }
}
