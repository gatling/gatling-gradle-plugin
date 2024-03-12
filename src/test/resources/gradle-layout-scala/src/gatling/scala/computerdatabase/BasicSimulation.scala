package computerdatabase
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.apache.commons.lang.StringUtils.lowerCase

class BasicSimulation extends Simulation {

  val httpConf = http
    .baseUrl(lowerCase(TestUtils.hostName()))
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val scn = scenario("Scenario Name")
    .exec(
        http("request_1").get("/"),
        pause(1),
        http("request_2").get("/computers?f=macbook"),
        pause(1),
        http("request_3").get("/computers/6")
      )

  setUp(scn.inject(atOnceUsers(1)))
    .protocols(httpConf)
    .assertions(global.successfulRequests.percent.gt(99))
}
