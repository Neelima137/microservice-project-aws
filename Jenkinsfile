@Library('my-shared-lib') _  

pipeline {
    agent any

    stages {
        stage('CI') {
            steps {
                ciPipeline()
            }
        }
        stage('CD') {
            steps {
                cdPipeline()
            }
        }
    }
}
