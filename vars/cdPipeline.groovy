// vars/deployToK8s.groovy
def call(Map config = [:]) {
    def APP_NAME   = config.appName ?: "microservice-app"
    def IMAGE_NAME = config.imageName ?: "483898563284.dkr.ecr.ap-south-1.amazonaws.com/webapps/microservice"
    def IMAGE_TAG  = config.tag ?: env.BUILD_NUMBER
    def NAMESPACE  = config.namespace ?: "webapps"

    withKubeCredentials(kubectlCredentials: [[
        caCertificate: '',
        clusterName: 'microservices',
        contextName: '',
        credentialsId: config.k8sCreds ?: 'k8s-token',
        namespace: NAMESPACE,
        serverUrl: config.clusterUrl ?: 'https://DF85F618DC81B5AD5E6C93FC8F4DE955.gr7.ap-south-1.eks.amazonaws.com'
    ]]) {
        try {
            sh """
                echo " Updating manifest with new image..."
                sed -i "s|IMAGE_PLACEHOLDER|${IMAGE_NAME}:${IMAGE_TAG}|g" deployment-service.yml

                echo "Applying deployment..."
                kubectl apply -f deployment-service.yml -n ${NAMESPACE}

                echo "⏳ Waiting for rollout to finish..."
                kubectl rollout status deployment/${APP_NAME} -n ${NAMESPACE}
            """

            echo "✅ Deployment successful: ${IMAGE_NAME}:${IMAGE_TAG}"

            sh """
                echo " Checking Services & Pods..."
                kubectl get svc -n ${NAMESPACE}
                kubectl get pods -n ${NAMESPACE}
            """

        } catch (err) {
            echo "❌ Deployment failed: ${err}"
            sh "kubectl rollout undo deployment/${APP_NAME} -n ${NAMESPACE} || true"

            sh "kubectl describe pods -n ${NAMESPACE} > describe-pods.txt || true"
            sh "kubectl logs -l app=${APP_NAME} -n ${NAMESPACE} > app-logs.txt || true"
            archiveArtifacts artifacts: '*.txt', allowEmptyArchive: true

            error("Deployment failed, rollback triggered")
        }
    }
}
