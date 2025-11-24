def call(Map cfg = [:]) {
    pipeline {
        agent any

        parameters {
            // Optional – makes jobs override values if needed
            string(name: 'APP', defaultValue: cfg.app ?: '', description: 'Application Name')
            string(name: 'BUILD', defaultValue: cfg.build ?: '', description: 'Build Identifier/Version')
            string(name: 'CLIENT', defaultValue: cfg.client ?: '', description: 'Client Name')
            choice(name: 'ENV', choices: ['dev', 'test', 'staging', 'prod'], description: 'Environment')
        }

        environment {
            APP    = "${params.APP ?: cfg.app}"
            BUILD  = "${params.BUILD ?: cfg.build}"
            CLIENT = "${params.CLIENT ?: cfg.client}"
            ENV    = "${params.ENV ?: (cfg.env ?: 'dev')}"
        }

        stages {
            stage("Info") {
                steps {
                    echo "Deploying APP=${APP}, BUILD=${BUILD}, CLIENT=${CLIENT}, ENV=${ENV}"
                }
            }

            stage("Checkout") {
                steps {
                    // Example – adjust to your repo
                    // git url: 'git@bitbucket.org:org/app-repo.git', branch: 'main'
                    echo "Checkout code for ${APP} / ${BUILD}"
                }
            }

            stage("Deploy") {
                steps {
                    // Put your real deployment logic here: helm, ansible, ssh, etc.
                    sh """
                        echo "Running deploy.sh --app ${APP} --build ${BUILD} --client ${CLIENT} --env ${ENV}"
                        # ./deploy.sh --app ${APP} --build ${BUILD} --client ${CLIENT} --env ${ENV}
                    """
                }
            }
        }

        post {
            success {
                echo "Deployment SUCCESS: ${APP}/${BUILD} for ${CLIENT} to ${ENV}"
            }
            failure {
                echo "Deployment FAILED: ${APP}/${BUILD} for ${CLIENT} to ${ENV}"
            }
        }
    }
}
