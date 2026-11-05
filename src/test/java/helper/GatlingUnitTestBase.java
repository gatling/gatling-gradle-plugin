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

import io.gatling.gradle.GatlingPluginExtension;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;

public abstract class GatlingUnitTestBase extends GatlingTestBase {

  protected Project project;

  protected GatlingPluginExtension gatlingExt;

  @BeforeEach
  void setUpProject() {
    createBuildFolder("/gradle-layout", SimulationLanguage.SCALA);

    project = ProjectBuilder.builder().withProjectDir(projectDir).build();
    project.getPluginManager().apply("scala");
    project.getPluginManager().apply("io.gatling.gradle");
    project.getRepositories().mavenCentral(repository -> repository.setName("gatlingMavenCentral"));

    gatlingExt = project.getExtensions().getByType(GatlingPluginExtension.class);
  }

  /** Runs afterEvaluate hooks: Gradle has no public API for this. */
  protected void evaluate() {
    ((ProjectInternal) project).evaluate();
  }
}
