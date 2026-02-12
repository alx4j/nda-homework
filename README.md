# Homework for NDA company

A service that efficiently calculates shortest land routes between countries by CCA3 code and land borders

## Prerequisites
- Java 17
- Maven

## Build
```bash
mvn clean package
```

## Run
```bash
java -jar target/homework.jar
```

The app listens on port `8083`

## Usage
### Endpoint
`GET /routing/{origin}/{destination}`

- `origin` and `destination` are CCA3 country codes.
- Input is normalized to uppercase (`cze` and `CZE` behave the same).

### Successful request
```bash
curl -s http://localhost:8083/routing/CZE/ITA
```

Example response:
```json
{
  "route": ["CZE", "AUT", "ITA"]
}
```

### Rejected request
```bash
curl -i http://localhost:8083/routing/USA/FRA
```

Example error (Spring [ProblemDetail](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/ProblemDetail.html) based on [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html)):
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "No land route found from USA to FRA",
  "instance": "/routing/USA/FRA"
}
```

## Implementation details
### 1) Countries preloading (once at startup)
Input data: `src/main/resources/countries.json`
Notation: `V` = number of countries (graph nodes), `E` = number of land borders (undirected edges).

- Parse JSON with Jackson streaming API
- Read only fields needed for routing: `cca3`, `borders`
- Build compact ID structures:
  - `Map<String, Integer>`: CCA3 -> node id
  - `String[]`: node id -> CCA3
- Build unique undirected border edges
- Union these edges with Disjoint Set Union (Union-Find) to precompute connected countries
- Build immutable adjacency structure (`int[][]`) and dense component ids (`int[]`)

Why this approach:
- Streaming + selective fields keeps startup memory low and avoids large temporary POJOs.
- Border graph is unweighted, so compact primitive adjacency arrays fit the problem well
- DSU unions during preload provide connected components without extra graph traversals
- Complexity: total preload time is `O(V + E)` and precomputed memory footprint is `O(V + E)`.

### 2) Route computation (per request)
For each route query:

- Normalize origin/destination codes.
- Resolve both codes to ids (unknown -> `400`).
- Fast return if same country
- Very fast reject if countries are not connected by land (O(1)).
- Run bidirectional BFS on the precomputed unweighted graph (O(V + E), but faster than classic BFS)
- Reconstruct path and map node ids back to CCA3 codes.

Why this approach:
- Each border crossing has equal cost, so shortest paths are solved by BFS
- Bidirectional BFS explores significantly fewer nodes than one-sided BFS in most practical cases while preserving shortest-path guarantees
- Complexity: request-time work is `O(1)` for normalization/lookups/precheck/reject, plus `O(V + E)` worst case for bidirectional BFS, plus `O(N)` for path reconstruction.

## Efficiency benchmarks ([JMH](https://github.com/openjdk/jmh))
### Build benchmark jar
```bash
mvn -Pjmh -DskipTests clean package
```

This creates `target/homework-benchmarks.jar`.

### Run benchmarks
```bash
java -jar target/homework-benchmarks.jar
```

### Benchmark table
Local run on Azul OpenJDK 17.0.12, Apple M2 Max. All operations are extremely efficient with sub-microsecond latencies

**Average latency:**

| Scenario                                            | Average latency (nanoseconds) |
|-----------------------------------------------------|------------------------------:|
| Short reachable route (Czechia -> Italy)            |                         168ns |
| Long reachable route (Portugal -> China)            |                         526ns |
| Fast reject when countries are not connected at all |                          16ns |
| Reject unknown country code                         |                          13ns |
| Origin equals destination (same-country shortcut)   |                          36ns |

**Latency distribution (only for route-search scenarios):**

| Scenario | p50 (nanoseconds) | p95 (nanoseconds) | p99 (nanoseconds) |
|---|------------------:|------------------:|------------------:|
| Short reachable route (Czechia -> Italy) |             167ns |             209ns |             333ns |
| Long reachable route (Portugal -> China) |             500ns |             583ns |             709ns |
