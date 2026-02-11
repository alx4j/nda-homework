package nda.company.homework.preload;

import nda.company.homework.routing.preload.CountryGraph;
import nda.company.homework.routing.preload.CountryPreloader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Country preloader")
class CountryPreloaderTest {
    @Test
    @DisplayName("reads only cca3 and borders while ignoring all other JSON fields")
    void shouldReadOnlyCca3AndBordersAndIgnoreOtherJsonFields() {
        String json = """
                [
                  {"cca3":"aaa","borders":["bbb"],"name":{"common":"Alpha"},"population":1},
                  {"cca3":"bbb","borders":["aaa","ccc"],"languages":{"x":"y"}},
                  {"cca3":"ccc","borders":["bbb"],"anything":[1,2,3]},
                  {"cca3":"ddd","borders":[],"nested":{"ignore":true}}
                ]
                """;

        CountryPreloader loader = new CountryPreloader(
                new ObjectMapper(),
                new InMemoryResourceLoader(json)
        );

        CountryGraph graph = loader.load();

        int aaa = graph.idByCode("AAA");
        int bbb = graph.idByCode("BBB");
        int ccc = graph.idByCode("CCC");
        int ddd = graph.idByCode("DDD");

        assertTrue(aaa >= 0);
        assertTrue(bbb >= 0);
        assertTrue(ccc >= 0);
        assertTrue(ddd >= 0);
        assertEquals(4, graph.nodeCount());
        assertEquals(2, graph.edgeCount());
    }

    @Test
    @DisplayName("computes connected components from union-find roots")
    void shouldAssignComponentsFromUnionFindRoots() {
        String json = """
                [
                  {"cca3":"AAA","borders":["BBB"]},
                  {"cca3":"BBB","borders":["AAA","CCC"]},
                  {"cca3":"CCC","borders":["BBB"]},
                  {"cca3":"DDD","borders":[]}
                ]
                """;

        CountryPreloader loader = new CountryPreloader(
                new ObjectMapper(),
                new InMemoryResourceLoader(json)
        );

        CountryGraph graph = loader.load();
        int aaa = graph.idByCode("AAA");
        int ccc = graph.idByCode("CCC");
        int ddd = graph.idByCode("DDD");

        assertEquals(graph.componentOf(aaa), graph.componentOf(ccc));
        assertNotEquals(graph.componentOf(aaa), graph.componentOf(ddd));
        assertEquals(2, graph.componentCount());
    }

    @Test
    @DisplayName("builds undirected adjacency list with complete neighbors")
    void shouldBuildUndirectedAndCompleteAdjacency() {
        String json = """
                [
                  {"cca3":"AAA","borders":["BBB","CCC"]},
                  {"cca3":"BBB","borders":["AAA"]},
                  {"cca3":"CCC","borders":["AAA"]}
                ]
                """;

        CountryPreloader loader = new CountryPreloader(
                new ObjectMapper(),
                new InMemoryResourceLoader(json)
        );

        CountryGraph graph = loader.load();
        int aaa = graph.idByCode("AAA");
        int bbb = graph.idByCode("BBB");
        int ccc = graph.idByCode("CCC");

        Set<Integer> neighborsOfAaa = neighborsOf(graph, aaa);
        Set<Integer> neighborsOfBbb = neighborsOf(graph, bbb);
        Set<Integer> neighborsOfCcc = neighborsOf(graph, ccc);

        assertEquals(Set.of(bbb, ccc), neighborsOfAaa);
        assertEquals(Set.of(aaa), neighborsOfBbb);
        assertEquals(Set.of(aaa), neighborsOfCcc);
        assertEquals(2, graph.edgeCount());
    }

    private Set<Integer> neighborsOf(CountryGraph graph, int nodeId) {
        Set<Integer> neighbors = new HashSet<>();
        for (int neighborId : graph.neighborsOf(nodeId)) {
            neighbors.add(neighborId);
        }
        return neighbors;
    }

    private static final class InMemoryResourceLoader implements ResourceLoader {

        private final byte[] jsonData;

        private InMemoryResourceLoader(String json) {
            this.jsonData = json.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public Resource getResource(String location) {
            return new ByteArrayResource(jsonData);
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }
    }
}
