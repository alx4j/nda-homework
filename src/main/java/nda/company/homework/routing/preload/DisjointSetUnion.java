package nda.company.homework.routing.preload;

import java.util.Arrays;

/**
 * Disjoint Set Union (Union-Find) with path compression and union by rank.
 * <p>
 * Used during preload to maintain connected components as edges are added.
 * Why this algorithm: incremental unions are near-constant amortized time,
 * so components are available "for free" at the end of parsing.
 */
final class DisjointSetUnion {
    private static final int INITIAL_CAPACITY = 256;

    private int[] parentByElementId = new int[INITIAL_CAPACITY];
    private byte[] rankByElementId = new byte[INITIAL_CAPACITY];
    private int elementCount;

    /**
     * Adds a new singleton set.
     *
     * @return id of the newly created element
     */
    int addElement() {
        ensureCapacity(elementCount + 1);
        parentByElementId[elementCount] = elementCount;
        rankByElementId[elementCount] = 0;
        elementCount++;
        return elementCount - 1;
    }

    /**
     * Finds canonical representative of an element and compresses the visited path.
     *
     * @param value element id
     * @return representative id of the set containing {@code value}
     */
    int find(int value) {
        int root = value;
        while (root != parentByElementId[root]) {
            root = parentByElementId[root];
        }
        int current = value;
        while (current != root) {
            int next = parentByElementId[current];
            parentByElementId[current] = root;
            current = next;
        }
        return root;
    }

    /**
     * Merges sets containing the two elements.
     *
     * @param left first element id
     * @param right second element id
     */
    void union(int left, int right) {
        int leftRoot = find(left);
        int rightRoot = find(right);
        if (leftRoot == rightRoot) {
            return;
        }

        if (rankByElementId[leftRoot] < rankByElementId[rightRoot]) {
            parentByElementId[leftRoot] = rightRoot;
            return;
        }
        if (rankByElementId[leftRoot] > rankByElementId[rightRoot]) {
            parentByElementId[rightRoot] = leftRoot;
            return;
        }

        parentByElementId[rightRoot] = leftRoot;
        rankByElementId[leftRoot]++;
    }

    /**
     * Exports one representative per element after full path compression.
     *
     * @return root id for each element index
     */
    int[] snapshotParents() {
        int[] rootByElementId = new int[elementCount];
        for (int elementId = 0; elementId < elementCount; elementId++) {
            rootByElementId[elementId] = find(elementId);
        }
        return rootByElementId;
    }

    private void ensureCapacity(int requiredCapacity) {
        if (requiredCapacity <= parentByElementId.length) {
            return;
        }
        int newCapacity = Math.max(parentByElementId.length * 2, requiredCapacity);
        parentByElementId = Arrays.copyOf(parentByElementId, newCapacity);
        rankByElementId = Arrays.copyOf(rankByElementId, newCapacity);
    }
}
