package org.labkey.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction
import org.labkey.gradle.util.BuildUtils

import java.util.regex.Matcher

/**
 * Checks for conflicts that may exist between a file collection and the files in an existing directory
 */
class CheckForVersionConflicts  extends DefaultTask
{
    /** The directory to check for existing files **/
    File directory
    /** The extension of the files to look for.  Null indicates all files **/
    String extension = null
    /** Indicates if the task should fail when a conflict is found or only log a warning **/
    Boolean failOnConflict = false
    /** The collection of files to check for.  Usually this will come from a configuration. **/
    FileCollection collection
    /** The name of a task to run if conflicts are found that will resolve the conflict (presumably by cleaning out the directory) **/
    String cleanTask

    @TaskAction
    void doAction()
    {
        List<String> conflicts = []
        Map<String, String> nameVersionMap = new HashMap<>()
        File[] existingFiles = directory.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                return extension == null || name.endsWith(extension);
            }
        })
        for (File dFile: existingFiles) {
            Matcher matcher = BuildUtils.VERSIONED_ARTIFACT_NAME_PATTERN.matcher(dFile.name)
            if (matcher.matches())
            {
                // we support artifacts with different classifiers (e.g., activeio-core-3.1.0-tests.jar should not be in conflict with activeio-core-3.1.0.jar)
                String nameWithClassifier = matcher.group(BuildUtils.ARTIFACT_NAME_INDEX)
                if (matcher.group(BuildUtils.ARTIFACT_CLASSIFIER_INDEX) != null)
                    nameWithClassifier += matcher.group(BuildUtils.ARTIFACT_CLASSIFIER_INDEX)
                if (nameVersionMap.containsKey(nameWithClassifier))
                    conflicts += "Multiple ${matcher.group(BuildUtils.ARTIFACT_NAME_INDEX)} ${extension} files."
                else if (matcher.group(BuildUtils.ARTIFACT_VERSION_INDEX) != null)
                {
                    project.logger.debug("adding name (with classifier): ${nameWithClassifier} and version: ${matcher.group(BuildUtils.ARTIFACT_VERSION_INDEX)}")
                    nameVersionMap.put(nameWithClassifier, matcher.group(BuildUtils.ARTIFACT_VERSION_INDEX).substring(1))
                }
                else
                {
                    project.logger.debug("adding name (with classifier): ${nameWithClassifier} and no version")
                    nameVersionMap.put(nameWithClassifier, null)
                }
            }
        }
        collection.files.each { File f ->
            Matcher matcher = BuildUtils.VERSIONED_ARTIFACT_NAME_PATTERN.matcher(f.name)
            if (matcher.matches())
            {
                String name = matcher.group(BuildUtils.ARTIFACT_NAME_INDEX)
                if (matcher.group(BuildUtils.ARTIFACT_CLASSIFIER_INDEX) != null)
                    name += matcher.group(BuildUtils.ARTIFACT_CLASSIFIER_INDEX)
                if (nameVersionMap.containsKey(name))
                {
                    String version = matcher.group(BuildUtils.ARTIFACT_VERSION_INDEX)
                    if (version != null)
                        version = version.substring(1)
                    project.logger.debug("Checking name (with classifier): ${name} and version ${version}")
                    String existingVersion = nameVersionMap.get(name)
                    if (existingVersion != version)
                    {
                        conflicts += "Conflicting version of ${matcher.group(BuildUtils.ARTIFACT_NAME_INDEX)} ${extension} file (${existingVersion} in directory vs. ${version} from build)."
                    }
                }
            }
        }

        if (!conflicts.isEmpty())
        {
            String message  = "Artifact versioning problem(s) in directory ${directory}:\n  " + conflicts.join("\n  ")
            if (cleanTask != null)
                message += "\nRun the ${cleanTask} task to remove existing artifacts in that directory."
            if (failOnConflict && !project.hasProperty('onlyCheckVersions'))
                throw new GradleException(message)
            else
                project.logger.warn("WARNING: " + message)
        }
    }
}
