package example.advanced

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import io.gatling.javaapi.core.*
import io.gatling.javaapi.http.*
import org.apache.commons.lang.StringUtils.*;

class BasicSimulation2 : Simulation() {

    // Load VU count from system properties
    private val vu: Int = Integer.getInteger("vu", 1)

    // Define HTTP configuration
    private val httpProtocol: HttpProtocolBuilder = http
        .baseUrl(lowerCase(example.MainUtils.hostName()))
        .acceptHeader("application/json")
        .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
)

    // Define scenario
    private val scenario: ScenarioBuilder = scenario("Scenario")
        .exec(http("Session").get("/session"))

    // Define assertions
    private val assertion: Assertion = global().failedRequests().count().lt(1L)

    // Define injection profile and execute the test
    init {
        setUp(
            scenario.injectOpen(atOnceUsers(vu))
        ).assertions(assertion).protocols(httpProtocol)
    }
}
