package computerdatabase;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static org.apache.commons.lang.StringUtils.lowerCase;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

public class BasicSimulation extends Simulation {

  HttpProtocolBuilder httpConf = http
    .baseUrl(lowerCase(TestUtils.hostName()))
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0");

  ScenarioBuilder scn = scenario("Scenario Name")
    .exec(
      http("request_1").get("/"),
      pause(1),
      http("request_2").get("/computers?f=macbook"),
      pause(1),
      http("request_3").get("/computers/6")
    );

  {
    setUp(scn.injectOpen(atOnceUsers(1)))
      .protocols(httpConf)
      .assertions(global().successfulRequests().percent().gt(99.0));
  }
}
