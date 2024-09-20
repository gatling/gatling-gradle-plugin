# Gatling Plugin for Gradle

[<picture><source media="(prefers-color-scheme: dark)" srcset="https://docs.gatling.io/images/logo-gatling.svg"><img src="https://docs.gatling.io/images/logo-gatling-noir.svg" alt="Gatling" width="50%"></picture>](https://gatling.io)

[![Workflow Status (main)](https://img.shields.io/github/actions/workflow/status/gatling/gatling-gradle-plugin/test-only.yml?branch=main&logo=github&style=for-the-badge)](https://github.com/gatling/gatling-gradle-plugin/actions?query=branch%3Amain)
[![License](https://img.shields.io/github/license/gatling/gatling-gradle-plugin?logo=apache&style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)
[![Gatling Community](https://img.shields.io/badge/Community-Gatling-e28961?style=for-the-badge&logo=discourse)](https://community.gatling.io)

> [!WARNING]
> The documentation now lives on the Gatling website and can be found [here](https://docs.gatling.io/reference/integrations/build-tools/gradle-plugin/).

## Dev testing

To manually test this plugin in a sample project, see the [Gradle documentation](https://docs.gradle.org/current/userguide/testing_gradle_plugins.html#manual-tests).

### With IncludeBuild
Note that the test project must use Gradle 7.1 or later for `includeBuild` within `pluginManagement` to work.

Steps to be able to dev test this plugin:

1. Clone this project

2. In a separate directory, create a toy project with a file called `settings.gradle` containing:

    ```groovy
    includeBuild "<path/to>/gatling-gradle-plugin"
    ```

3. And a file called `build.gradle` containing:

    ```groovy
    plugins {
      id "io.gatling.gradle"
    }

    repositories {
      mavenCentral()
    }

    sourceCompatibility = 1.8
    targetCompatibility = 1.8
    ```

### With maven-publish plugin

To publish locally, you need to enable `maven-publish` plugin, add a version and a group. In `build.gradle`:

```
plugins {
  ...
  id 'maven-publish'
}
version '42.0.0-SNAPSHOT'
group 'io.gatling'
```

You can do a `gradle publishToMavenLocal` to publish a snapshot version to your local maven repository.

To use this local plugin in your project:
* change the plugin version to the one above
* add a `settings.gradle` file in the root of your project with the following content:
```
pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
  }
}
```
