package com.matyrobbrt.gml.buildscript

import com.matyrobbrt.gml.buildscript.util.DependencyUtils
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import com.matyrobbrt.gml.buildscript.data.JiJConfigurationData
import com.matyrobbrt.gml.buildscript.data.JiJDependencyData
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.util.internal.ConfigureUtil

@CompileStatic
abstract class JiJExtension {
    @PackageScope final Project project
    JiJExtension(Project project) {
        this.project = project
    }

    private final Map<String, JiJConfigurationData> configurationData = [:]
    private final Map<Dependency, JiJDependencyData> dependencyConfigurationData = [:]

    JiJConfigurationData getData(String configurationName) {
        return configurationData.computeIfAbsent(configurationName, { new JiJConfigurationData() })
    }

    JiJDependencyData resolve(ModuleDependency dependency, Configuration depConfiguration) {
        final resolved = DependencyUtils.getResolvedDependency(project, dependency)
        final conf = getData(depConfiguration.name)
        JiJDependencyData configuration = dependencyConfigurationData[dependency]
        return conf.applyData(resolved, configuration ?: new JiJDependencyData())
    }

    @CompileDynamic
    void onConfiguration(String name, @DelegatesTo(
            value = JiJConfigurationData, strategy = Closure.DELEGATE_FIRST
    ) Closure closure) {
        final data = configurationData.computeIfAbsent(name, { new JiJConfigurationData() })
        ConfigureUtil.configure(closure, data)
    }

    void onConfiguration(Configuration configuration, @DelegatesTo(
            value = JiJConfigurationData, strategy = Closure.DELEGATE_FIRST
    ) Closure closure) {
        onConfiguration(configuration.name, closure)
    }

    void addDependencyData(Dependency dependency, JiJDependencyData data) {
        dependencyConfigurationData[dependency] = data
    }

    @CompileDynamic
    void onDependency(Dependency dependency, @DelegatesTo(
            value = JiJDependencyData, strategy = Closure.DELEGATE_FIRST
    ) Closure closure) {
        final data = new JiJDependencyData()
        ConfigureUtil.configure(closure, data)
        addDependencyData(dependency, data)
    }

    @CompileDynamic
    void dependencies(@DelegatesTo(
            value = JiJDependencyHandler, strategy = Closure.DELEGATE_FIRST
    ) Closure closure) {
        ConfigureUtil.configure(closure, new JiJDependencyHandler(this))
    }
}