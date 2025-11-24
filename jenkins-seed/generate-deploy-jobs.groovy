import groovy.json.JsonSlurper

def envs = ["Dev", "Test", "Staging", "Prod"]

// Read config JSON from workspace (seed job will check out this repo)
def configText = readFileFromWorkspace('jenkins-seed/deploy-config.json')
def config = new JsonSlurper().parseText(configText)

config.apps.each { String appName, def appData ->

    folder(appName) {
        description("Folder for application: ${appName}")
    }

    appData.builds.each { String buildName, def clients ->

        folder("${appName}/${buildName}") {
            description("Folder for build: ${buildName} of ${appName}")
        }

        clients.each { String clientName ->

            folder("${appName}/${buildName}/${clientName}") {
                description("Folder for client: ${clientName} of ${appName} / ${buildName}")
            }

            envs.each { String envDisplay ->

                String envLower = envDisplay.toLowerCase()
                String jobName = "${appName}/${buildName}/${clientName}/Deploy ${envDisplay} ENV"

                pipelineJob(jobName) {
                    description("Auto-generated deployment job for ${appName} / ${buildName} / ${clientName} → ${envDisplay}")

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
}
