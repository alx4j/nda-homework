package nda.company.homework.services;

import nda.company.homework.routing.preload.CountryGraph;
import nda.company.homework.routing.services.RoutingResult;
import nda.company.homework.routing.services.RoutingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DisplayName("Routing service")
class RoutingServiceTest {
    private final RoutingService routingService = new RoutingService(testGraph());

    @ParameterizedTest(name = "[{index}] {0} -> {1} returns {2}")
    @MethodSource("successfulRoutes")
    @DisplayName("returns shortest route for reachable countries")
    void shouldReturnShortestRoute(String origin, String destination, List<String> expectedRoute) {
        RoutingResult result = routingService.findRoute(origin, destination);

        RoutingResult.Success success = assertInstanceOf(RoutingResult.Success.class, result);
        assertEquals(expectedRoute, success.route());
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} has no land route")
    @MethodSource("unreachablePairs")
    @DisplayName("rejects countries from different connected components")
    void shouldRejectWhenCountriesAreInDifferentComponents(
            String origin,
            String destination,
            String expectedOrigin,
            String expectedDestination
    ) {
        RoutingResult result = routingService.findRoute(origin, destination);

        RoutingResult.NoLandRoute noLandRoute = assertInstanceOf(RoutingResult.NoLandRoute.class, result);
        assertEquals(expectedOrigin, noLandRoute.origin());
        assertEquals(expectedDestination, noLandRoute.destination());
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} fails with unknown code {2}")
    @MethodSource("unknownCountryRequests")
    @DisplayName("rejects unknown country codes")
    void shouldRejectUnknownCountryCode(String origin, String destination, String expectedUnknownCode) {
        RoutingResult result = routingService.findRoute(origin, destination);

        RoutingResult.UnknownCountry unknownCountry = assertInstanceOf(RoutingResult.UnknownCountry.class, result);
        assertEquals(expectedUnknownCode, unknownCountry.countryCode());
    }

    private static Stream<Arguments> successfulRoutes() {
        return Stream.of(
                Arguments.of("AAA", "BBB", List.of("AAA", "BBB")),
                Arguments.of("AAA", "CCC", List.of("AAA", "BBB", "CCC")),
                Arguments.of("AAA", "AAA", List.of("AAA")),
                Arguments.of(" aaa ", "ccc", List.of("AAA", "BBB", "CCC"))
        );
    }

    private static Stream<Arguments> unreachablePairs() {
        return Stream.of(
                Arguments.of("AAA", "DDD", "AAA", "DDD"),
                Arguments.of("ddd", "aaa", "DDD", "AAA")
        );
    }

    private static Stream<Arguments> unknownCountryRequests() {
        return Stream.of(
                Arguments.of("ZZZ", "AAA", "ZZZ"),
                Arguments.of("aaa", " zzz ", "ZZZ"),
                Arguments.of("qqq", "ddd", "QQQ")
        );
    }

    private static CountryGraph testGraph() {
        return new CountryGraph(
                Map.of(
                        "AAA", 0,
                        "BBB", 1,
                        "CCC", 2,
                        "DDD", 3
                ),
                new String[]{"AAA", "BBB", "CCC", "DDD"},
                new int[][]{
                        {1},
                        {0, 2},
                        {1},
                        {}
                },
                new int[]{0, 0, 0, 1}
        );
    }
}
