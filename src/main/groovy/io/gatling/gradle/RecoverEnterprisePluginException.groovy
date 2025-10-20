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
package io.gatling.gradle


import io.gatling.plugin.exceptions.*
import org.gradle.api.BuildCancelledException
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskExecutionException

final class RecoverEnterprisePluginException {

  /**
   * @param f Closure defined inside a Task
   * @param logger used to report EnterprisePluginException
   * @return closure result
   */
  static <R> R handle(Logger logger, Closure<R> f) {
    try {
      return f.doCall()
    }  catch (UserQuitException e) {
      throw new BuildCancelledException(e.getMessage(), e)
    } catch (UnsupportedJavaVersionException e) {
      throwTaskExecutionException(f.getThisObject(), """
                |${e.getMessage()}
                |In order to target the supported Java version, please use the following Gradle setting (requires Gradle 6.6 or later):
                |compileJava {
                |    options.release = ${e.supportedVersion}
                |}
                |See also the Gradle documentation: https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
                |Another solution is to configure a Java toolchain to use Java ${e.supportedVersion}; see https://docs.gradle.org/current/userguide/toolchains.html
                |Alternatively, the reported class may come from your project's dependencies, published targeting Java ${e.version}. In this case you need to use dependencies which target Java ${e.supportedVersion} or lower."
                """.stripMargin())
    }
  }

  private static void throwTaskExecutionException(Object subject, String message) throws TaskExecutionException {
    throw new TaskExecutionException(subject as Task, new IllegalArgumentException(message))
  }
}
