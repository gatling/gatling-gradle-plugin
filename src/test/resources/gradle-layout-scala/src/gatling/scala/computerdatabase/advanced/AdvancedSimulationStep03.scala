package computerdatabase.advanced

import org.apache.commons.lang.StringUtils.lowerCase
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class AdvancedSimulationStep03 extends Simulation {

  object Search {
    val feeder = csv("search.csv").random

    val search = exec(
        http("Home").get("/"),
        pause(1),
        feed(feeder),
        http("Search")
          .get("/computers?f=#{searchCriterion}")
          .check(css("a:contains('#{searchComputerName}')", "href"))
        )
  }

  val httpConf = http
    .baseUrl(lowerCase(computerdatabase.MainUtils.hostName()))
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val scn = scenario("Users").exec(Search.search)

  setUp(scn.inject(atOnceUsers(1)))
    .protocols(httpConf)
    .assertions(global.successfulRequests.percent.gt(99))
}
