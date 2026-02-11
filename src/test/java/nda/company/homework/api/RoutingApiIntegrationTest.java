package nda.company.homework.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("Routing API")
class RoutingApiIntegrationTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @ParameterizedTest(name = "[{index}] GET /routing/{0}/{1} returns route of {2} countries")
    @CsvSource({
            "CZE,ITA,3,CZE,ITA",
            "cze,ita,3,CZE,ITA",
            "DEU,DEU,1,DEU,DEU"
    })
    @DisplayName("returns route for reachable countries")
    void shouldReturnRouteForReachableCountries(
            String origin,
            String destination,
            int expectedRouteSize,
            String expectedStart,
            String expectedEnd
    ) throws Exception {
        mockMvc.perform(get("/routing/{origin}/{destination}", origin, destination))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.length()").value(expectedRouteSize))
                .andExpect(jsonPath("$.route[0]").value(expectedStart))
                .andExpect(jsonPath("$.route[" + (expectedRouteSize - 1) + "]").value(expectedEnd));
    }

    @ParameterizedTest(name = "[{index}] GET /routing/{0}/{1} -> 400 with detail \"{2}\"")
    @CsvSource({
            "USA,FRA,No land route found",
            "FRA,USA,No land route found",
            "ZZZ,ITA,Unknown country code",
            "ita,zzz,Unknown country code"
    })
    @DisplayName("returns bad request for invalid route requests")
    void shouldReturnBadRequestForInvalidRequests(String origin, String destination, String expectedDetailPart) throws Exception {
        mockMvc.perform(get("/routing/{origin}/{destination}", origin, destination))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(expectedDetailPart)));
    }
}
