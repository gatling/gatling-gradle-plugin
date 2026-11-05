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

import io.gatling.plugin.pkg.Dependency;
import io.gatling.plugin.pkg.EnterprisePackager;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.tasks.Jar;

@CacheableTask
// abstract because
// https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#injection_getters_are_now_abstract
public abstract class GatlingEnterprisePackageTask extends Jar {

  private String groupId;

  private String artifactId = getProject().getName();

  private String artifactVersion;

  // defaults are resolved lazily as group and version are commonly set late, e.g. by release
  // plugins
  private final Provider<String> defaultGroupId = GradleProviders.group(getProject());

  private final Provider<String> defaultArtifactVersion = GradleProviders.version(getProject());

  private File projectDir = getProject().getProjectDir();

  public GatlingEnterprisePackageTask() {
    String envPackagePath = System.getenv("GATLING_ENTERPRISE_BUILDER_PACKAGE_PATH");
    if (envPackagePath != null) {
      File packagePath = new File(envPackagePath).getAbsoluteFile();
      getDestinationDirectory().fileValue(packagePath.getParentFile());
      getArchiveFileName().set(packagePath.getName());
    }
    Configuration configuration =
        getProject().getConfigurations().getByName("gatlingRuntimeClasspath");
    getDependencies().set(getProject().provider(() -> new Dependencies(configuration)));
  }

  @Input
  public String getGroupId() {
    return groupId != null ? groupId : defaultGroupId.get();
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  @Input
  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  @Input
  public String getArtifactVersion() {
    return artifactVersion != null ? artifactVersion : defaultArtifactVersion.get();
  }

  public void setArtifactVersion(String artifactVersion) {
    this.artifactVersion = artifactVersion;
  }

  @Classpath
  public abstract ConfigurableFileCollection getClassDirectories();

  @Internal(
      "only used to determine Git metadata (branch name and sha) which should not require task re-execution")
  public File getProjectDir() {
    return projectDir;
  }

  public void setProjectDir(File projectDir) {
    this.projectDir = projectDir;
  }

  @Nested
  public abstract Property<Dependencies> getDependencies();

  @TaskAction
  @Override
  protected void copy() {
    Dependencies deps = getDependencies().get();
    EnterprisePackager packager =
        new EnterprisePackager(new GradlePluginIO(getLogger()).getLogger());
    try {
      packager.createEnterprisePackage(
          new ArrayList<>(getClassDirectories().getFiles()),
          new ArrayList<>(),
          toDependencies(deps.getGatling()),
          toDependencies(deps.getExtra()),
          getGroupId(),
          artifactId,
          getArtifactVersion(),
          "gradle",
          getClass().getPackage().getImplementationVersion(),
          getArchiveFile().get().getAsFile(),
          projectDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Set<Dependency> toDependencies(Set<SerializableDependency> dependencies) {
    return dependencies.stream()
        .map(SerializableDependency::toDependency)
        .collect(Collectors.toSet());
  }

  public static class Dependencies implements Serializable {

    private final Set<SerializableDependency> gatling;

    private final Set<SerializableDependency> extra;

    public Dependencies(Configuration configuration) {
      Set<ResolvedDependency> firstLevelModuleDependencies =
          configuration.getResolvedConfiguration().getFirstLevelModuleDependencies();

      Map<SerializableDependency.Id, SerializableDependency> allDependenciesById =
          collectAllDependencies(firstLevelModuleDependencies);
      Set<SerializableDependency.Id> gatlingDependencyTree =
          collectGatlingDependencyTree(firstLevelModuleDependencies);

      gatling =
          gatlingDependencyTree.stream().map(allDependenciesById::get).collect(Collectors.toSet());

      extra =
          allDependenciesById.entrySet().stream()
              .filter(entry -> !gatlingDependencyTree.contains(entry.getKey()))
              .map(Map.Entry::getValue)
              .collect(Collectors.toSet());
    }

    @Nested
    public Set<SerializableDependency> getGatling() {
      return gatling;
    }

    @Nested
    public Set<SerializableDependency> getExtra() {
      return extra;
    }

    private static boolean hasModuleId(ResolvedDependency dependency) {
      return dependency.getModule() != null && dependency.getModule().getId() != null;
    }

    private static Map<SerializableDependency.Id, SerializableDependency> collectAllDependencies(
        Set<ResolvedDependency> firstLevelModuleDependencies) {
      Map<SerializableDependency.Id, SerializableDependency> acc = new LinkedHashMap<>();
      collectAllDependenciesRec(firstLevelModuleDependencies, acc, new HashSet<>());
      return acc;
    }

    private static void collectAllDependenciesRec(
        Set<ResolvedDependency> dependencies,
        Map<SerializableDependency.Id, SerializableDependency> acc,
        Set<ResolvedDependency> alreadyVisited) {
      for (ResolvedDependency dependency : dependencies) {
        if (hasModuleId(dependency) && alreadyVisited.add(dependency)) {
          ModuleVersionIdentifier id = dependency.getModule().getId();
          for (ResolvedArtifact moduleArtifact : dependency.getModuleArtifacts()) {
            SerializableDependency dep =
                new SerializableDependency(
                    new SerializableDependency.Id(
                        id.getGroup(),
                        id.getName(),
                        id.getVersion(),
                        moduleArtifact.getClassifier()),
                    moduleArtifact.getFile().getPath());
            acc.put(dep.getId(), dep);
          }
          collectAllDependenciesRec(dependency.getChildren(), acc, alreadyVisited);
        }
      }
    }

    private static Set<SerializableDependency.Id> collectGatlingDependencyTree(
        Set<ResolvedDependency> firstLevelModuleDependencies) {
      Set<SerializableDependency.Id> acc = new LinkedHashSet<>();
      collectGatlingDependencyTreeRec(
          firstLevelModuleDependencies, acc, new HashSet<>(), new HashSet<>());
      return acc;
    }

    private static void collectGatlingDependencyTreeRec(
        Set<ResolvedDependency> dependencies,
        Set<SerializableDependency.Id> acc,
        Set<ModuleVersionIdentifier> alreadyVisited,
        Set<ResolvedDependency> alreadyCollected) {
      for (ResolvedDependency dependency : dependencies) {
        if (hasModuleId(dependency) && alreadyVisited.add(dependency.getModule().getId())) {
          ModuleVersionIdentifier id = dependency.getModule().getId();
          if (id.getGroup().equals("io.gatling") || id.getGroup().equals("io.gatling.highcharts")) {
            collectAllDependencyTree(dependency, acc, alreadyCollected);
          } else {
            collectGatlingDependencyTreeRec(
                dependency.getChildren(), acc, alreadyVisited, alreadyCollected);
          }
        }
      }
    }

    private static void addAllArtifacts(
        ResolvedDependency dependency, Set<SerializableDependency.Id> acc) {
      ModuleVersionIdentifier id = dependency.getModule().getId();
      for (ResolvedArtifact artifact : dependency.getModuleArtifacts()) {
        acc.add(
            new SerializableDependency.Id(
                id.getGroup(), id.getName(), id.getVersion(), artifact.getClassifier()));
      }
    }

    // alreadyCollected: shared subtrees (gatling-core, netty...) are reachable through many
    // paths, and walking each of them again for every path grows combinatorially
    private static void collectAllDependencyTree(
        ResolvedDependency dependency,
        Set<SerializableDependency.Id> acc,
        Set<ResolvedDependency> alreadyCollected) {
      if (alreadyCollected.add(dependency)) {
        addAllArtifacts(dependency, acc);
        for (ResolvedDependency child : dependency.getChildren()) {
          collectAllDependencyTree(child, acc, alreadyCollected);
        }
      }
    }
  }

  public static class SerializableDependency implements Serializable {

    public static class Id implements Serializable {
      private final String groupId;
      private final String artifactId;
      private final String version;
      private final String classifier;

      public Id(String groupId, String artifactId, String version, String classifier) {
        this.groupId = Objects.requireNonNull(groupId);
        this.artifactId = Objects.requireNonNull(artifactId);
        this.version = Objects.requireNonNull(version);
        this.classifier = classifier;
      }

      public String getGroupId() {
        return groupId;
      }

      public String getArtifactId() {
        return artifactId;
      }

      public String getVersion() {
        return version;
      }

      public String getClassifier() {
        return classifier;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (!(o instanceof Id)) {
          return false;
        }
        Id other = (Id) o;
        return groupId.equals(other.groupId)
            && artifactId.equals(other.artifactId)
            && version.equals(other.version)
            && Objects.equals(classifier, other.classifier);
      }

      @Override
      public int hashCode() {
        return Objects.hash(groupId, artifactId, version, classifier);
      }
    }

    private final Id id;

    private final String file;

    public SerializableDependency(Id id, String file) {
      this.id = Objects.requireNonNull(id);
      this.file = Objects.requireNonNull(file);
    }

    @Input
    public Id getId() {
      return id;
    }

    @Classpath
    public String getFile() {
      return file;
    }

    public Dependency toDependency() {
      return new Dependency(
          new Dependency.Id(id.groupId, id.artifactId, id.version, id.classifier), new File(file));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof SerializableDependency)) {
        return false;
      }
      SerializableDependency other = (SerializableDependency) o;
      return id.equals(other.id) && file.equals(other.file);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, file);
    }
  }
}
