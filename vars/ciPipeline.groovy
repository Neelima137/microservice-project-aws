def call(Map config = [:]) {

    def IMAGE_NAME   = config.imageName ?: 'microservice-app'
    def IMAGE_TAG    = 'latest' // Always use latest
    def AWS_REGION   = "ap-south-1"
    def ECR_REGISTRY = "483898563284.dkr.ecr.ap-south-1.amazonaws.com"
    def SONAR_PROJECT_KEY = "adservice_microservice" 
    def ECR_REPO     = "webapps/microservice"
    def gradleExists = fileExists('./gradlew')
    
    env.JAVA_HOME = tool name: 'jdk19', type: 'jdk'
    env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"

    stage('Checkout') {
        checkout scm
    }

    stage('GitLeaks Scan') {
        sh 'gitleaks detect --source . --report-format=json --report-path=gitleaks-report.json || true'
    }

    if (!gradleExists) {
        echo "Gradle wrapper not found. Skipping SonarQube Scan."
    } else {
        withSonarQubeEnv('sonarqube-server') {
            withCredentials([string(credentialsId: 'sonar-cred', variable: 'SONARQUBE_TOKEN')]) {
                def cmd = "./gradlew clean build sonarqube -x verifyGoogleJavaFormat \
                    -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                    -Dsonar.host.url=${SONAR_HOST_URL} \
                    -Dsonar.login=$SONARQUBE_TOKEN"
                def status = sh(script: cmd, returnStatus: true)
                if (status != 0) {
                    echo "Gradle build or SonarQube scan failed, skipping stage."
                }
            }
        }
    }

    if (!fileExists('Dockerfile')) {
        echo "Dockerfile not found. Skipping Docker build, push, and scan stages."
    } else {
        stage('Docker Build') {
            sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
        }

        stage('Docker Push - DockerHub') {
            withDockerRegistry([credentialsId: 'docker-cred', url: 'https://index.docker.io/v1/']) {
                sh """
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} devadineelima137/microservice:latest
                    docker push devadineelima137/microservice:latest
                """
            }
        }

        stage('Docker Push - AWS ECR') {
            withAWS(credentials: 'aws-creds', region: "${AWS_REGION}") {
                sh """
                    aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${ECR_REGISTRY}/${ECR_REPO}:latest
                    docker push ${ECR_REGISTRY}/${ECR_REPO}:latest
                """
            }
        }

        stage('Trivy Scan') {
            sh "trivy image ${IMAGE_NAME}:${IMAGE_TAG} > trivy-report.txt || true"
        }

        archiveArtifacts artifacts: '*.json,*.txt', allowEmptyArchive: true
    }
}
