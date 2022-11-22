package com.matyrobbrt.gml.buildscript

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class JiJPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.getExtensions().create("jij", JiJExtension.class, project)
    }
}
