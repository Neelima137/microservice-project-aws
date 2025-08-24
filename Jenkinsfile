@Library('my-shared-lib') _

pipeline {
    agent any

    parameters {
        string(name: 'SERVICE_NAME', defaultValue: 'recommendationservice', description: 'Microservice name')
    }

    stages {
        stage('CI') {
            steps {
                ciPipeline(
                    repo: "my-app",
                    branch: env.BRANCH_NAME,
                    serviceName: params.SERVICE_NAME
                )
            }
        }

        stage('CD') {
            steps {
                script {
                    def ecrImage = "483898563284.dkr.ecr.ap-south-1.amazonaws.com/webapps/microservice-${params.SERVICE_NAME}:latest"

                    cdPipeline(
                        appName: params.SERVICE_NAME,
                        imageName: ecrImage,
                        tag: 'latest',
                        namespace: "webapps",
                        k8sCreds: "k8s-token",
                        clusterUrl: "https://DF85F618DC81B5AD5E6C93FC8F4DE955.gr7.ap-south-1.eks.amazonaws.com"
                    )
                }
            }
        }
    }
}
