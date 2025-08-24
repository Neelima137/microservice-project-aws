def call(Map config = [:]) {

    // ---------------- CONFIG ----------------
    def SERVICE_NAME = config.serviceName ?: 'microservice-app' // Microservice name
    def IMAGE_TAG = 'latest' // Docker image tag
    def AWS_REGION = 'ap-south-1' // AWS region
    def ECR_REGISTRY = '483898563284.dkr.ecr.ap-south-1.amazonaws.com' // ECR registry
    def ECR_REPO = "webapps/microservice" // Fixed ECR repo
    def IMAGE_NAME = "${ECR_REGISTRY}/${ECR_REPO}:${SERVICE_NAME}-${IMAGE_TAG}" // Tag includes service name
    def gradleExists = fileExists('./gradlew') // Check if Gradle wrapper exists

    // Set JAVA_HOME if needed
    env.JAVA_HOME = tool name: 'jdk19', type: 'jdk'
    env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"

    // ---------------- STAGES ----------------

    stage('Checkout') {
        echo "Checking out source code..."
        checkout scm
    }

    stage('GitLeaks Scan') {
        echo "Scanning repository for secrets with Gitleaks..."
        sh 'gitleaks detect --source . --report-format=json --report-path=gitleaks-report.json || true'
    }

    if (gradleExists) {
        stage('SonarQube Scan') {
            echo "Running SonarQube analysis..."
            withSonarQubeEnv('sonarqube-server') {
                withCredentials([string(credentialsId: 'sonar-cred', variable: 'SONARQUBE_TOKEN')]) {
                    def cmd = "./gradlew clean build sonarqube -x verifyGoogleJavaFormat \
                        -Dsonar.projectKey=${SERVICE_NAME}_microservice \
                        -Dsonar.host.url=${SONAR_HOST_URL} \
                        -Dsonar.login=$SONARQUBE_TOKEN"
                    def status = sh(script: cmd, returnStatus: true)
                    if (status != 0) {
                        echo "Gradle build or SonarQube scan failed. Continuing pipeline..."
                    }
                }
            }
        }
    } else {
        echo "Gradle wrapper not found. Skipping SonarQube scan."
    }

    if (fileExists('Dockerfile')) {

        stage('Docker Build') {
            echo "Building Docker image for ${SERVICE_NAME}..."
            sh "docker build -t ${IMAGE_NAME} ."
        }

        stage('Docker Push - AWS ECR') {
            echo "Pushing Docker image to AWS ECR..."
            withAWS(credentials: 'aws-creds', region: "${AWS_REGION}") {
                sh """
                    # Login to AWS ECR
                    aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    
                    # Push image with service-specific tag to fixed ECR repo
                    docker push ${IMAGE_NAME}
                """
            }
        }

        stage('Docker Push - DockerHub (Optional)') {
            echo "Optional: Push Docker image to DockerHub..."
            // Uncomment below if DockerHub push is required
            // withDockerRegistry([credentialsId: 'docker-cred', url: 'https://index.docker.io/v1/']) {
            //     sh """
            //         docker tag ${IMAGE_NAME} devadineelima137/${SERVICE_NAME}:${IMAGE_TAG}
            //         docker push devadineelima137/${SERVICE_NAME}:${IMAGE_TAG}
            //     """
            // }
        }

        stage('Trivy Scan') {
            echo "Scanning Docker image with Trivy..."
            sh "trivy image ${IMAGE_NAME} > trivy-report.txt || true"
        }

        stage('Archive Reports') {
            echo "Archiving security and scan reports..."
            archiveArtifacts artifacts: '*.json,*.txt', allowEmptyArchive: true
        }

        stage('Final Outcome') {
            echo "------------------------------------------------"
            echo "Docker image for service '${SERVICE_NAME}' successfully built and pushed to AWS ECR:"
            echo "${IMAGE_NAME}"
            echo "------------------------------------------------"
        }

    } else {
        echo "Dockerfile not found. Skipping Docker build, push, and scan stages."
    }
}
