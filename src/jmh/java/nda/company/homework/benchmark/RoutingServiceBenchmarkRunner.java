package nda.company.homework.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public final class RoutingServiceBenchmarkRunner {

    private RoutingServiceBenchmarkRunner() {
    }

    public static void main(String[] args) throws Exception {
        ChainedOptionsBuilder optionsBuilder = new OptionsBuilder()
                .include(RoutingServiceBenchmark.class.getSimpleName())
                .jvmArgs("-Xms512m", "-Xmx512m");
        if (args.length > 0) {
            optionsBuilder.parent(new CommandLineOptions(args));
        }
        Options options = optionsBuilder.build();
        new Runner(options).run();
    }
}
