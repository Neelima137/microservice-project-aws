@Library('my-shared-lib') _

pipeline {
    agent any

    stages {
        stage('CI') {
            steps {
                ciPipeline(
                    repo: "my-app",
                    branch: env.BRANCH_NAME,
                    imageName: "483898563284.dkr.ecr.ap-south-1.amazonaws.com/webapps/microservice",
                    tag: env.BUILD_NUMBER
                )
            }
        }

        stage('CD') {
            steps {
                cdPipeline(
                    appName: "microservice-app",
                    imageName: "483898563284.dkr.ecr.ap-south-1.amazonaws.com/webapps/microservice",
                    tag: env.BUILD_NUMBER,
                    namespace: "webapps",
                    k8sCreds: "k8s-token",
                    clusterUrl: "https://DF85F618DC81B5AD5E6C93FC8F4DE955.gr7.ap-south-1.eks.amazonaws.com"
                )
            }
        }
    }
}
