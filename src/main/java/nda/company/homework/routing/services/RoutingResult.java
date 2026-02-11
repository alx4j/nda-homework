package nda.company.homework.routing.services;

import java.util.List;
import java.util.Objects;

/**
 * Result of route lookup.
 */
public sealed interface RoutingResult
        permits RoutingResult.Success, RoutingResult.Failure {

    /**
     * Creates a successful result.
     *
     * @param route ordered CCA3 route from origin to destination
     * @return success result
     */
    static RoutingResult success(List<String> route) {
        return new Success(route);
    }

    /**
     * Creates an "unknown country code" failure.
     *
     * @param countryCode invalid or unknown CCA3 code
     * @return failure result
     */
    static RoutingResult unknownCountry(String countryCode) {
        return new UnknownCountry(countryCode);
    }

    /**
     * Creates a "no land route" failure.
     *
     * @param origin normalized origin CCA3 code
     * @param destination normalized destination CCA3 code
     * @return failure result
     */
    static RoutingResult noLandRoute(String origin, String destination) {
        return new NoLandRoute(origin, destination);
    }

    /**
     * Successful routing outcome.
     *
     * @param route ordered CCA3 route from origin to destination
     */
    record Success(List<String> route) implements RoutingResult {

        /**
         * Creates immutable success payload.
         */
        public Success {
            route = List.copyOf(route);
        }
    }

    /**
     * Base type for predictable business failures returned by routing.
     */
    sealed interface Failure extends RoutingResult permits UnknownCountry, NoLandRoute {

        /**
         * Human-readable error message suitable for API responses.
         *
         * @return error detail
         */
        String message();
    }

    /**
     * Failure for unknown CCA3 country code.
     *
     * @param countryCode invalid or unknown code
     */
    record UnknownCountry(String countryCode) implements Failure {

        /**
         * Creates unknown-country failure.
         */
        public UnknownCountry {
            countryCode = Objects.requireNonNull(countryCode, "countryCode");
        }

        @Override
        public String message() {
            return "Unknown country code: " + countryCode;
        }
    }

    /**
     * Failure for countries that are not connected by land route.
     *
     * @param origin normalized origin CCA3 code
     * @param destination normalized destination CCA3 code
     */
    record NoLandRoute(String origin, String destination) implements Failure {

        /**
         * Creates no-route failure.
         */
        public NoLandRoute {
            origin = Objects.requireNonNull(origin, "origin");
            destination = Objects.requireNonNull(destination, "destination");
        }

        @Override
        public String message() {
            return "No land route found from " + origin + " to " + destination;
        }
    }
}
