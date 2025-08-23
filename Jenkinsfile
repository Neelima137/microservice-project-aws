@Library('jenkins-shared-library') _

pipeline {
    agent any

    stages {
        stage('CI Pipeline') {
            steps {
                script {
                    ciPipeline()
                }
            }
        }

        stage('CD Pipeline') {
            steps {
                script {
                    cdPipeline()
                }
            }
        }
    }
}
