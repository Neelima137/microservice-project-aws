def call(Map config = [:]) {
    pipeline {
        agent any

        environment {
            DOCKER_HUB_CREDENTIALS = credentials('docker-cred')
            AWS_CREDENTIALS        = credentials('aws-cred')
            IMAGE_NAME             = "${config.imageName ?: 'microservice-app'}"
            IMAGE_TAG              = "${config.tag ?: env.BUILD_NUMBER}"
            AWS_REGION             = "ap-south-1"
            ECR_REGISTRY           = "483898563284.dkr.ecr.ap-south-1.amazonaws.com"
        }

        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('GitLeaks Scan') {
                steps {
                    sh 'gitleaks detect --source . --report-format=json --report-path=gitleaks-report.json || true'
                }
            }

            stage('SonarQube Scan') {
                environment {
                    SONARQUBE = credentials('sonar-cred')
                }
                steps {
                    withSonarQubeEnv('sonarqube-server') {
                        sh "mvn clean verify sonar:sonar -Dsonar.projectKey=${IMAGE_NAME} -Dsonar.login=${SONARQUBE}"
                    }
                }
            }

            stage('SonarQube Quality Gate') {
                steps {
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }

            stage('Docker Build') {
                steps {
                    sh "docker build -t ${devadineelima137/microservice}:${IMAGE_TAG} ."
                }
            }

            stage('Docker Push - DockerHub') {
                steps {
                    withDockerRegistry([credentialsId: 'docker-cred']) {
                        sh "docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${DOCKER_HUB_CREDENTIALS_USR}/${IMAGE_NAME}:${IMAGE_TAG}"
                        sh "docker push ${devadineelima137}/${IMAGE_NAME}:${IMAGE_TAG}"
                    }
                }
            }

            stage('Docker Push - AWS ECR') {
                steps {
                    withAWS(credentials: 'aws-creds', region: "${AWS_REGION}") {
                        sh '''
                          aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                          docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${ECR_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                          docker push ${ECR_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                        '''
                    }
                }
            }

            stage('Trivy Scan') {
                steps {
                    sh "trivy image ${IMAGE_NAME}:${IMAGE_TAG} > trivy-report.txt || true"
                }
            }
        }

        post {
            always {
                archiveArtifacts artifacts: '*.json,*.txt', allowEmptyArchive: true
            }
        }
    }
}

