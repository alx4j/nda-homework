package nda.company.homework.routing.preload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads countries from {@code countries.json} and builds an immutable routing graph at startup.
 * Parsing is streaming-based and reads only {@code cca3} and {@code borders}.
 */
@Component
public class CountryPreloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryPreloader.class);

    private static final String CCA3_FIELD = "cca3";
    private static final String BORDERS_FIELD = "borders";
    private static final String COUNTRIES_RESOURCE = "classpath:countries.json";
    private static final String COUNTRIES_ARRAY_ERROR = "Countries data must be a JSON array";
    private static final String LOAD_FAILURE_MESSAGE = "Failed to load country graph data";

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public CountryPreloader(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
    }

    /**
     * Parses the countries resource and returns precomputed routing data.
     *
     * @return immutable country graph ready for request-time routing
     */
    public CountryGraph load() {
        long startedAtNanos = System.nanoTime();
        LOGGER.info("Loading country graph from resource {}", COUNTRIES_RESOURCE);
        Resource resource = resolveCountriesResource();

        try (InputStream inputStream = resource.getInputStream()) {
            CountryGraph graph = parseGraph(inputStream);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
            LOGGER.info(
                    "Loaded country graph: countries={}, edges={}, components={}, preloadMs={}",
                    graph.nodeCount(),
                    graph.edgeCount(),
                    graph.componentCount(),
                    elapsedMillis
            );
            return graph;
        } catch (IOException exception) {
            LOGGER.error("Failed to load country graph data from {}", COUNTRIES_RESOURCE, exception);
            throw new IllegalStateException(LOAD_FAILURE_MESSAGE, exception);
        }
    }

    private CountryGraph parseGraph(InputStream inputStream) throws IOException {
        CountryGraphBuildState buildState = new CountryGraphBuildState();

        try (JsonParser parser = objectMapper.createParser(inputStream)) {
            ensureArrayRoot(parser);

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.currentToken() != JsonToken.START_OBJECT) {
                    parser.skipChildren();
                    continue;
                }

                ParsedCountry parsedCountry = readCountry(parser);
                if (parsedCountry.cca3() == null) {
                    continue;
                }

                int countryId = buildState.idFor(parsedCountry.cca3());
                connectBorders(buildState, countryId, parsedCountry.borders());
            }
        }

        return buildState.toGraph();
    }

    private ParsedCountry readCountry(JsonParser parser) throws IOException {
        String countryCode = null;
        List<String> borderCodes = List.of();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.currentName();
            JsonToken valueToken = parser.nextToken();
            if (valueToken == null) {
                break;
            }

            if (CCA3_FIELD.equals(fieldName) && valueToken == JsonToken.VALUE_STRING) {
                countryCode = normalizeCode(parser.getValueAsString());
            } else if (BORDERS_FIELD.equals(fieldName) && valueToken == JsonToken.START_ARRAY) {
                borderCodes = readBorders(parser);
            } else {
                parser.skipChildren();
            }
        }

        return new ParsedCountry(countryCode, borderCodes);
    }

    private List<String> readBorders(JsonParser parser) throws IOException {
        List<String> borders = new ArrayList<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                parser.skipChildren();
                continue;
            }
            String borderCode = normalizeCode(parser.getValueAsString());
            if (borderCode != null) {
                borders.add(borderCode);
            }
        }
        return borders;
    }

    private String normalizeCode(String rawCode) {
        if (!StringUtils.hasText(rawCode)) {
            return null;
        }
        return rawCode.trim().toUpperCase(Locale.ROOT);
    }

    private Resource resolveCountriesResource() {
        Resource resource = resourceLoader.getResource(COUNTRIES_RESOURCE);
        if (!resource.exists()) {
            throw new IllegalStateException("Country data resource does not exist: " + COUNTRIES_RESOURCE);
        }
        return resource;
    }

    private void ensureArrayRoot(JsonParser parser) throws IOException {
        if (parser.nextToken() != JsonToken.START_ARRAY) {
            throw new IOException(COUNTRIES_ARRAY_ERROR);
        }
    }

    private void connectBorders(CountryGraphBuildState buildState, int countryId, List<String> borderCodes) {
        for (String borderCode : borderCodes) {
            int borderId = buildState.idFor(borderCode);
            buildState.connectUndirected(countryId, borderId);
        }
    }

    private record ParsedCountry(String cca3, List<String> borders) {
    }
}
