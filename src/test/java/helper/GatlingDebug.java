/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package helper;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.gradle.GatlingPlugin;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;

/** What GatlingDebugSimulation (gatling-debug-scala fixture) prints about its JVM. */
public final class GatlingDebug {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public final Map<String, String> systemProperties;

  public final Map<String, String> env;

  public final List<String> jvmArgs;

  public final Map<String, Long> heap;

  public GatlingDebug(BuildResult buildResult) {
    assertThat(buildResult.task(":" + GatlingPlugin.GATLING_RUN_TASK_NAME).getOutcome())
        .isEqualTo(TaskOutcome.SUCCESS);

    List<String> lines =
        buildResult
            .getOutput()
            .lines()
            .filter(line -> line.startsWith("@@@@"))
            .collect(Collectors.toList());
    assertThat(lines).hasSize(4);

    heap = parse(lines, "heap", new TypeReference<>() {});
    jvmArgs = parse(lines, "jvm", new TypeReference<>() {});
    systemProperties = parse(lines, "sys", new TypeReference<>() {});
    env = parse(lines, "env", new TypeReference<>() {});
  }

  private static <T> T parse(List<String> lines, String key, TypeReference<T> type) {
    String prefix = "@@@@." + key + " ";
    String json =
        lines.stream()
            .filter(line -> line.startsWith(prefix))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No " + prefix + "line"))
            .substring(prefix.length());
    try {
      return MAPPER.readValue(json, type);
    } catch (JsonProcessingException e) {
      throw new AssertionError("Invalid JSON for " + key + ": " + json, e);
    }
  }
}
