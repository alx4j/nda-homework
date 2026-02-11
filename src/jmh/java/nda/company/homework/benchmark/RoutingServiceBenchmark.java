package nda.company.homework.benchmark;

import nda.company.homework.routing.preload.CountryGraph;
import nda.company.homework.routing.preload.CountryPreloader;
import nda.company.homework.routing.services.RoutingResult;
import nda.company.homework.routing.services.RoutingService;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xms512m", "-Xmx512m"})
public class RoutingServiceBenchmark {
    @State(Scope.Benchmark)
    public static class BenchmarkState {

        private RoutingService routingService;

        @Setup(Level.Trial)
        public void setup() {
            CountryPreloader loader = new CountryPreloader(
                    new ObjectMapper(),
                    new DefaultResourceLoader()
            );
            CountryGraph countryGraph = loader.load();
            this.routingService = new RoutingService(countryGraph);
        }
    }

    @Benchmark
    public List<String> shortRoute(BenchmarkState state) {
        return ((RoutingResult.Success) state.routingService.findRoute("CZE", "ITA")).route();
    }

    @Benchmark
    public List<String> longRoute(BenchmarkState state) {
        return ((RoutingResult.Success) state.routingService.findRoute("PRT", "CHN")).route();
    }

    @Benchmark
    public List<String> sameCountry(BenchmarkState state) {
        return ((RoutingResult.Success) state.routingService.findRoute("DEU", "DEU")).route();
    }

    @Benchmark
    public boolean fastRejectDifferentComponents(BenchmarkState state) {
        return state.routingService.findRoute("USA", "FRA") instanceof RoutingResult.NoLandRoute;
    }

    @Benchmark
    public boolean rejectUnknownCountry(BenchmarkState state) {
        return state.routingService.findRoute("ZZZ", "POL") instanceof RoutingResult.UnknownCountry;
    }
}
