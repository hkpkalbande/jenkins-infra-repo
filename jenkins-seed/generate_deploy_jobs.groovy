import groovy.json.JsonSlurper

/**
 * Helper method to fetch the latest build artifact from the App Build job.
 * This MUST be inside the script, above the DSL processing.
 */
def getLatestBuild = { appName ->

    def jobName = "${appName}-Build"          // Your CI build job name
    def job = Jenkins.instance.getItem(jobName)

    if (!job) {
        println "❗ ERROR: Build job '${jobName}' not found."
        return null
    }

    def lastSuccessful = job.getLastSuccessfulBuild()
    if (!lastSuccessful) {
        println "❗ No successful builds found for ${appName}"
        return null
    }

    def artifact = lastSuccessful.artifacts.find { it.fileName == "latest-build.txt" }
    if (!artifact) {
        println "❗ No 'latest-build.txt' found in artifacts of ${jobName}"
        return null
    }

    // Read artifact content
    def content = artifact.getFile().text.trim()
    println "✔ Latest build for ${appName} = ${content}"
    return content
}

// -------------------- MAIN DSL LOGIC --------------------

def config = new JsonSlurper().parseText(
    readFileFromWorkspace('jenkins-seed/deploy-config.json')
)

def envs = ["Dev", "Test", "Staging", "Prod"]

config.apps.each { appName, appData ->

    folder(appName)

    // Fetch latest build for this app
    def buildName = getLatestBuild(appName)
    if (!buildName) {
        println "Skipping ${appName}, cannot determine build"
        return
    }

    folder("${appName}/${buildName}")

    appData.clients.each { clientName ->

        folder("${appName}/${buildName}/${clientName}")

        envs.each { envDisplay ->

            def envLower = envDisplay.toLowerCase()

            pipelineJob("${appName}/${buildName}/${clientName}/Deploy ${envDisplay} ENV") {

                description("Auto-generated deployment job for ${appName}/${buildName}/${clientName} → ${envDisplay}")

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
