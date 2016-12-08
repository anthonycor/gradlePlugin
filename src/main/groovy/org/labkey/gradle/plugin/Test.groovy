package org.labkey.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.labkey.gradle.util.BuildUtils
import org.labkey.gradle.util.GroupNames
/**
 * Created by susanh on 12/7/16.
 */
class Test implements Plugin<Project>
{

  @Override
  void apply(Project project)
  {
    project.extensions.create("test", TestExtension)

    addDependencies(project)
    addTasks(project)
  }

  private void addDependencies(Project project)
  {
      BuildUtils.addLabKeyDependency(project: project, config: "compile", depProjectPath: ":remoteapi:java")
  }

  private void addTasks(Project project)
  {
      project.task("setPassword",
            group: GroupNames.TEST,
            description: "Set the password for use in running tests").doFirst({
                project.javaexec({
                    main = "org.labkey.test.util.PasswordUtil"
                    classpath {
                        [project.configurations.compile, project.tasks.jar]
                    }
                    systemProperties["labkey.server"] = project.labkey.server
                    args = ["set"]
                    standardInput = System.in
                })
            })

      project.task("ensurePassword",
            group: GroupNames.TEST,
            description: "Ensure that the password property used for running tests has been set").doFirst(
            {
               project.javaexec({
                main = "org.labkey.test.util.PasswordUtil"
                classpath {
                  [project.configurations.compile, project.tasks.jar]
                }
                systemProperties["labkey.server"] = project.labkey.server
                args = ["ensure"]
              })
            })
  }
}

class TestExtension
{
  def String testName = ""
  def Boolean cleanOnly = false
  def Boolean haltOnError = true
  def Boolean loop = true
  def Boolean clean = true
  def Boolean linkCheck = false
  def Boolean memCheck = true
  def Boolean scriptCheck = true
  def Boolean queryCheck = false
  def Boolean seleniumReuseWebDrive = true
  def Boolean viewCheck = true
  def Boolean closeOnFaile = false
  def String seleniumBrowser = "best"
  def String seleniumDebugPort = "5005"
  def String seleniumFirefoxBinary = ""
  def Boolean shuffleTests = false
  def Boolean disableAssertions = false
  def String suite = ""
  def String additionalPipelineTools=""

}
