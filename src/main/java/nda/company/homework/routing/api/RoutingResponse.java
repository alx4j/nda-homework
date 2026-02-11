package nda.company.homework.routing.api;

import java.util.List;

/**
 * Successful routing payload returned by the REST API.
 *
 * @param route ordered list of CCA3 country codes from origin to destination
 */
public record RoutingResponse(List<String> route) {
}
