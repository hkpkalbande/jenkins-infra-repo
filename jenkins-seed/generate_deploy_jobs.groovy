import groovy.json.JsonSlurper

// -------------------- READ CONFIG --------------------
def config = new JsonSlurper().parseText(
    readFileFromWorkspace('jenkins-seed/deploy-config.json')
)

// -------------------- DEFINE ENVIRONMENTS --------------------
def envs = ["Dev", "Test", "Staging", "Prod"]

// -------------------- MAIN LOGIC --------------------
config.apps.each { appName, appData ->

    // App folder
    folder(appName)

    // Read latest build copied by Copy Artifact plugin
    def buildFilePath = "build-artifacts/${appName}/latest-build.txt"
    def fileObj = new File(buildFilePath)

    if (!fileObj.exists()) {
        println "❗ No latest-build.txt for ${appName}, skipping"
        return
    }

    def buildName = fileObj.text.trim()
    println "✔ Found build for ${appName}: ${buildName}"

    // Create build folder
    folder("${appName}/${buildName}")

    // Create client folders + deploy jobs
    appData.clients.each { clientName ->

        folder("${appName}/${buildName}/${clientName}")

        envs.each { envDisplay ->

            def envLower = envDisplay.toLowerCase()

            pipelineJob("${appName}/${buildName}/${clientName}/Deploy ${envDisplay} ENV") {

                description("Auto-generated deploy job for ${appName}/${buildName}/${clientName} → ${envDisplay}")

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
