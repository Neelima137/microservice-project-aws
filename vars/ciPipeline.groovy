def call(Map config = [:]) {

    def IMAGE_NAME   = config.imageName ?: 'microservice-app'
    def IMAGE_TAG    = config.tag ?: env.BUILD_NUMBER
    def AWS_REGION   = "ap-south-1"
    def ECR_REGISTRY = "483898563284.dkr.ecr.ap-south-1.amazonaws.com"

    stage('Checkout') {
        checkout scm
    }

    stage('GitLeaks Scan') {
        sh 'gitleaks detect --source . --report-format=json --report-path=gitleaks-report.json || true'
    }

   stage('SonarQube Scan') {
    withSonarQubeEnv('sonarqube-server') {
        withCredentials([string(credentialsId: 'sonar-cred', variable: 'SONARQUBE_TOKEN')]) {
         
            def SONAR_PROJECT_KEY = "adservice_microservice" 

            sh "./gradlew clean build sonarqube -x verifyGoogleJavaFormat \
                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                -Dsonar.host.url=${SONAR_HOST_URL} \
                -Dsonar.login=${SONARQUBE_TOKEN}"
        }
    }
}


    stage('SonarQube Quality Gate') {
        timeout(time: 5, unit: 'MINUTES') {
            waitForQualityGate abortPipeline: true
        }
    }

    stage('Docker Build') {
        sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
    }

    stage('Docker Push - DockerHub') {
        withDockerRegistry([credentialsId: 'docker-cred', url: 'https://index.docker.io/v1/']) {
            sh "docker tag ${IMAGE_NAME}:${IMAGE_TAG} devadineelima137/${IMAGE_NAME}:${IMAGE_TAG}"
            sh "docker push devadineelima137/${IMAGE_NAME}:${IMAGE_TAG}"
        }
    }

    stage('Docker Push - AWS ECR') {
        withAWS(credentials: 'aws-creds', region: "${AWS_REGION}") {
            sh """
              aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
              docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${ECR_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
              docker push ${ECR_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
            """
        }
    }

    stage('Trivy Scan') {
        sh "trivy image ${IMAGE_NAME}:${IMAGE_TAG} > trivy-report.txt || true"
    }

    // Always archive reports
    archiveArtifacts artifacts: '*.json,*.txt', allowEmptyArchive: true
}
