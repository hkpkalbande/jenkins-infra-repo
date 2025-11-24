import groovy.json.JsonSlurper

// Load config JSON
def config = new JsonSlurper().parseText(
    readFileFromWorkspace('jenkins-seed/deploy-config.json')
)

def envs = ["Dev", "Test", "Staging", "Prod"]

config.apps.each { appName, appData ->

    folder(appName)


    // Path to build version from Git repo
    def buildFile = "build-info/jenkins-builds/${appName}/latest-build.txt"
    def buildName = null
    try {
        buildName = readFileFromWorkspace(buildFile).trim()
        println "✔ Latest build for ${appName}: ${buildName}"
    } catch (Exception e) {
        println "❗ No build version file found for ${appName}, skipping…"
        return
    }

    // Create build folder
    folder("${appName}/${buildName}")

    // Clients and environment jobs
    appData.clients.each { clientName ->

        folder("${appName}/${buildName}/${clientName}")

        envs.each { envDisplay ->

            pipelineJob("${appName}/${buildName}/${clientName}/Deploy ${envDisplay} ENV") {

                definition {
                    cps {
                        script("""
                            @Library('my-shared-library') _

                            deploy(
                                app: '${appName}',
                                build: '${buildName}',
                                client: '${clientName}',
                                env: '${envDisplay.toLowerCase()}'
                            )
                        """.stripIndent())
                        sandbox()
                    }
                }
            }
        }
    }
}
