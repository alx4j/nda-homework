package nda.company.homework.routing;

import nda.company.homework.routing.preload.CountryGraph;
import nda.company.homework.routing.preload.CountryPreloader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RoutingConfig {
    @Bean
    public CountryGraph countryGraphData(CountryPreloader countryPreloader) {
        return countryPreloader.load();
    }
}
