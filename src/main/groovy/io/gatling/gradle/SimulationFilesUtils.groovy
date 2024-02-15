package io.gatling.gradle

import io.gatling.scanner.SimulationScanner
import org.codehaus.plexus.util.SelectorUtils

import java.util.stream.Collectors

final class SimulationFilesUtils {

    static List<String> resolveSimulations(Collection<File> classpath, List<String> includes, List<String> excludes) {
        def dependencies = new ArrayList<File>()
        def classDirectories = new ArrayList<File>()

        classpath.forEach {file ->
            if (file.isDirectory()) {
                classDirectories.add(file)
            } else if (file.isFile()) {
                dependencies.add(file)
            }
        }

        def allSimulationClasses =  SimulationScanner.scan(dependencies, classDirectories).simulationClasses

        List<String> filteredSimulationClasses = allSimulationClasses.stream()
            .filter(
                className -> {
                    boolean isIncluded = includes.isEmpty() || match(includes, className)
                    boolean isExcluded = !excludes.isEmpty() && match(excludes, className)
                    return isIncluded && !isExcluded
                })
            .collect(Collectors.toList())

        return filteredSimulationClasses
    }

    private static boolean match(List<String> patterns, String string) {
        return patterns.any {it != null && SelectorUtils.match(it, string) }
    }
}
