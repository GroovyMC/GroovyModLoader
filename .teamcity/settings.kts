import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.*
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.
VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.
To debug settings scripts in command-line, run the
    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate
command and attach your debugger to the port 8000.
To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2022.10"

project {
    buildType(GroovyMC_GroovyModLoader_Build)
}

object GroovyMC_GroovyModLoader_Build : BuildType({
    id("Build")
    name = "Build"

    vcs {
        root(DslContext.settingsRoot)
    }

    triggers {
        vcs {
            triggerRules = "-:comment=\\[noci]:**"
        }
    }

    features {
        swabra {
            filesCleanup = Swabra.FilesCleanup.BEFORE_BUILD
            lockingProcesses = Swabra.LockingProcessPolicy.KILL
        }
        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = personalToken {
                    token = "%commit_status_publisher%"
                }
            }
        }
        discordNotification {
            webhookUrl = "%discord_webhook%"
        }
    }

    steps {
        gradle {
            name = "Configure TeamCity information"
            tasks = "configureTeamCity"
        }

        gradle {
            name = "Build Gradle Project"
            tasks = "build"
        }

        gradle {
            name = "Publish Gradle Project"
            tasks = ":publish :publishCurseForge :modrinth"
        }
    }
})