
println "[DEBUG] Starting generate_deploy_jobs.groovy"
import groovy.json.JsonSlurper


println "[DEBUG] Loading config JSON from jenkins-seed/deploy-config.json"
def config = new JsonSlurper().parseText(
    readFileFromWorkspace('jenkins-seed/deploy-config.json')
)
println "[DEBUG] Loaded config: ${config}"


def envs = ["Dev", "Test", "Staging", "Prod"]
println "[DEBUG] Environments: ${envs}"


println "[DEBUG] Apps in config: ${config.apps.keySet()}"
config.apps.each { appName, appData ->
    println "[DEBUG] Processing app: ${appName}"


    println "[DEBUG] Creating folder for app: ${appName}"
    folder(appName)

    // Path to build version from Git repo

    def buildFile = "build-info/jenkins-builds/${appName}/latest-build.txt"
    println "[DEBUG] Build file path: ${buildFile}"
    def file = new File(buildFile)


    if (!file.exists()) {
        println "[DEBUG] Build file does not exist for ${appName}, skipping."
        println "❗ No build version file found for ${appName}, skipping…"
        return
    }

    // Read build version

    def buildName = file.text.trim()
    println "[DEBUG] Read build name for ${appName}: ${buildName}"
    println "✔ Latest build for ${appName}: ${buildName}"

    // Create build folder

    println "[DEBUG] Creating build folder: ${appName}/${buildName}"
    folder("${appName}/${buildName}")

    // Clients and environment jobs

    println "[DEBUG] Clients for ${appName}: ${appData.clients}"
    appData.clients.each { clientName ->
        println "[DEBUG] Processing client: ${clientName} for app: ${appName}"


    println "[DEBUG] Creating client folder: ${appName}/${buildName}/${clientName}"
    folder("${appName}/${buildName}/${clientName}")


        println "[DEBUG] Creating jobs for client: ${clientName} in environments: ${envs}"
        envs.each { envDisplay ->
            println "[DEBUG] Creating pipeline job for: ${appName}/${buildName}/${clientName}/Deploy ${envDisplay} ENV"


            pipelineJob("${appName}/${buildName}/${clientName}/Deploy ${envDisplay} ENV") {

                definition {
                    cps {
                        script("""
                            // [DEBUG] Deploying app: ${appName}, build: ${buildName}, client: ${clientName}, env: ${envDisplay.toLowerCase()}
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
println "[DEBUG] Finished generate_deploy_jobs.groovy"
