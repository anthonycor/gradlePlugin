package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
/**
 * This task creates a pom file in a location that artifactory expects it when publishing.  It is meant to
 * replace the task created by the (incubating) maven-publish plugin since for whatever reason that task does
 * not pull in the dependencies (and it is sometimes, mysteriously, removed from the dependency list for
 * the artifactoryPublish task).
 */
class PomFile extends DefaultTask
{
    String artifactCategory = "libs"

    @OutputFile
    File pomFile = new File(project.buildDir, "publications/${artifactCategory}/pom-default.xml")

    @TaskAction
    public void writePomFile()
    {
            project.pom {
                withXml {
                    // remove the tomcat dependencies with no version specified because we cannot know which version of tomcat is in use
                    List<Node> toRemove = []
                    asNode().dependencies.first().each {
                        if ( it.get("groupId").first().value().first().equals("org.apache.tomcat") &&
                             it.get("version").isEmpty())
                            toRemove.add(it)
                    }
                    toRemove.each {
                        asNode().dependencies.first().remove(it)
                    }
                    // add in the dependencies from the external configuration as well
                    def dependenciesNode = asNode().dependencies.first()
                    project.configurations.external.dependencies.each {
                        def depNode = dependenciesNode.appendNode("dependency")
                        depNode.appendNode("groupId", it.group)
                        depNode.appendNode("artifactId", it.name)
                        depNode.appendNode("version", it.version)
                        depNode.appendNode("scope", "compile")
                    }
                    if (project.lkModule.getPropertyValue("Organization") != null || project.lkModule.getPropertyValue("OrganizationURL") != null)
                    {
                        def orgNode = asNode().appendNode("organization")
                        if (project.lkModule.getPropertyValue("Organization") != null)
                            orgNode.appendNode("name", project.lkModule.getPropertyValue("Organization"))
                        if (project.lkModule.getPropertyValue("OrganizationURL") != null)
                            orgNode.appendNode("url", project.lkModule.getPropertyValue("OrganizationURL"))
                    }
                    if (project.lkModule.getPropertyValue("Description") != null)
                        asNode().appendNode("description", project.lkModule.getPropertyValue("Description"))
                    if (project.lkModule.getPropertyValue("URL") != null)
                        asNode().appendNode("url", project.lkModule.getPropertyValue("URL"))
                    if (project.lkModule.getPropertyValue("License") != null || project.lkModule.getPropertyValue("LicenseURL") != null)
                    {
                        def licenseNode = asNode().appendNode("licenses").appendNode("license")
                        if (project.lkModule.getPropertyValue("License") != null)
                            licenseNode.appendNode("name", project.lkModule.getPropertyValue("License"))
                        if (project.lkModule.getPropertyValue("LicenseURL") != null)
                            licenseNode.appendNode("url", project.lkModule.getPropertyValue("LicenseURL"))
                        licenseNode.appendNode("distribution", "repo")
                    }
                }
            }.writeTo(pomFile)
    }
}