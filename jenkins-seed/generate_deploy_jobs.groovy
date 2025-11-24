import groovy.json.JsonSlurper
import java.nio.file.Files

def config = new JsonSlurper().parseText(
    readFileFromWorkspace('jenkins-seed/deploy-config.json')
)

def envs = ["Dev", "Test", "Staging", "Prod"]

// 1️⃣ Read latest build file for each app
config.apps.each { appName, appData ->

    // Folder for the app
    folder(appName)

    // Read latest build artifact
    String latestBuildFile = "build-artifacts/${appName}/latest-build.txt"
    if (!new File(latestBuildFile).exists()) {
        println "No build found for ${appName}, skipping..."
        return
    }

    String buildName = new File(latestBuildFile).text.trim()

    println "Detected latest build for ${appName}: ${buildName}"

    // 2️⃣ Create build folder
    folder("${appName}/${buildName}")

    // 3️⃣ Create client folders + deployment jobs
    appData.clients.each { clientName ->

        folder("${appName}/${buildName}/${clientName}")

        envs.each { envDisplay ->

            String envLower = envDisplay.toLowerCase()

            pipelineJob("${appName}/${buildName}/${clientName}/Deploy ${envDisplay} ENV") {

                definition {
                    cps {
                        script("""
                            @Library('my-shared-library') _

                            deploy(
                                app: '${appName}',
                                build: '${buildName}',
                                client: '${clientName}',
                                env: '${envLower}'
                            )
                        """.stripIndent())
                        sandbox()
                    }
                }
            }
        }
    }
}
