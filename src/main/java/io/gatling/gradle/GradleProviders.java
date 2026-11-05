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

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

// lazy project values for task defaults: the lambdas capture the Project at configuration time
// instead of calling Task.getProject() when evaluated, which Gradle deprecates at execution time
final class GradleProviders {

  private GradleProviders() {}

  static Provider<String> group(Project project) {
    return project.provider(() -> project.getGroup().toString());
  }

  static Provider<String> version(Project project) {
    return project.provider(() -> project.getVersion().toString());
  }
}
