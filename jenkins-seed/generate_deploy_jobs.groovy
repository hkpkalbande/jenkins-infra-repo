import groovy.json.JsonSlurper

// Helper: Read latest build from another Jenkins job using DSL API (supported!)
def getLatestBuild = { appName ->

    def jobName = "${appName}-Build"

    def job = jobs[jobName]
    if (job == null) {
        println "❗ ERROR: Build job '${jobName}' not found in DSL context."
        return null
    }

    def last = job.lastSuccessfulBuild
    if (last == null) {
        println "❗ No successful build found for ${jobName}"
        return null
    }

    def artifact = last.artifacts.find { it.fileName == 'latest-build.txt' }
    if (artifact == null) {
        println "❗ latest-build.txt not found in ${jobName}"
        return null
    }

    def content = artifact.file.text.trim()
    println "✔ Latest build for ${appName}: ${content}"
    return content
}

// Main DSL logic
def config = new JsonSlurper().parseText(
    readFileFromWorkspace('jenkins-seed/deploy-config.json')
)

def envs = ["Dev", "Test", "Staging", "Prod"]

config.apps.each { appName, appData ->

    folder(appName)

    def buildName = getLatestBuild(appName)
    if (!buildName) {
        println "Skipping ${appName}"
        return
    }

    folder("${appName}/${buildName}")

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
