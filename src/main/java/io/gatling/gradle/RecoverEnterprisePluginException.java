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
package io.gatling.gradle;

import io.gatling.plugin.exceptions.UnsupportedJavaVersionException;
import io.gatling.plugin.exceptions.UserQuitException;
import java.util.concurrent.Callable;
import org.gradle.api.BuildCancelledException;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskExecutionException;

public final class RecoverEnterprisePluginException {

  private RecoverEnterprisePluginException() {}

  /**
   * @param task the task running f, used to report errors
   * @param f the code to run
   * @return f's result
   */
  public static <R> R handle(Task task, Callable<R> f) throws Exception {
    try {
      return f.call();
    } catch (UserQuitException e) {
      throw new BuildCancelledException(e.getMessage(), e);
    } catch (UnsupportedJavaVersionException e) {
      throw new TaskExecutionException(
          task,
          new IllegalArgumentException(
              "\n"
                  + e.getMessage()
                  + "\n"
                  + "In order to target the supported Java version, please use the following Gradle setting (requires Gradle 6.6 or later):\n"
                  + "compileJava {\n"
                  + "    options.release = "
                  + e.supportedVersion
                  + "\n"
                  + "}\n"
                  + "See also the Gradle documentation: https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation\n"
                  + "Another solution is to configure a Java toolchain to use Java "
                  + e.supportedVersion
                  + "; see https://docs.gradle.org/current/userguide/toolchains.html\n"
                  + "Alternatively, the reported class may come from your project's dependencies, published targeting Java "
                  + e.version
                  + ". In this case you need to use dependencies which target Java "
                  + e.supportedVersion
                  + " or lower.\n"));
    }
  }
}
