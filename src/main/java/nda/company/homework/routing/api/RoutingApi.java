package nda.company.homework.routing.api;

import nda.company.homework.routing.services.RoutingResult;
import nda.company.homework.routing.services.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/routing")
public class RoutingApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingApi.class);

    private final RoutingService routingService;

    public RoutingApi(RoutingService routingService) {
        this.routingService = routingService;
    }

    /**
     * Returns one shortest land route between origin and destination countries.
     *
     * @param origin origin country code (CCA3)
     * @param destination destination country code (CCA3)
     * @return HTTP 200 with route payload, or HTTP 400 problem details when route is invalid/unreachable
     */
    @GetMapping("/{origin}/{destination}")
    public ResponseEntity<?> route(@PathVariable String origin, @PathVariable String destination) {
        RoutingResult result = routingService.findRoute(origin, destination);
        if (result instanceof RoutingResult.Success success) {
            LOGGER.debug(
                    "Route resolved: origin={} destination={} pathLength={}",
                    origin,
                    destination,
                    success.route().size()
            );
            return ResponseEntity.ok(new RoutingResponse(success.route()));
        }
        if (result instanceof RoutingResult.Failure failure) {
            LOGGER.debug(
                    "Route request rejected: origin={} destination={} reason={}",
                    origin,
                    destination,
                    failure.message()
            );
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, failure.message());
            return ResponseEntity.badRequest().body(problemDetail);
        }
        LOGGER.error("Unsupported routing result type: {}", result.getClass().getName());
        throw new IllegalStateException("Unsupported routing result: " + result.getClass().getName());
    }
}
