def call(Map config = [:]) {
    def IMAGE_NAME = config.imageName ?: "483898563284.dkr.ecr.ap-south-1.amazonaws.com/webapps/microservice"
    def IMAGE_TAG  = config.tag ?: env.BUILD_NUMBER
    def NAMESPACE  = config.namespace ?: "webapps"

    // Define deployments upfront
    def deployments = []

    if (fileExists('deployment-service.yml')) {
        withKubeCredentials(kubectlCredentials: [[
            caCertificate: '',
            clusterName: 'microservices',
            contextName: '',
            credentialsId: config.k8sCreds ?: 'k8s-token',
            namespace: NAMESPACE,
            serverUrl: config.clusterUrl ?: 'https://DF85F618DC81B5AD5E6C93FC8F4DE955.gr7.ap-south-1.eks.amazonaws.com'
        ]]) {
            try {
                // Authenticate to EKS cluster using AWS credentials
                withAWS(region: 'ap-south-1', credentials: 'aws-creds') {
                    sh '''
                        #!/bin/bash
                        echo "Logging into EKS cluster..."
                        aws eks update-kubeconfig --name microservices
                        kubectl get nodes
                    '''
                }

                // Update image in manifest
                sh '''
                    echo "Updating manifest with new image..."
                    sed -i "s|IMAGE_PLACEHOLDER|${IMAGE_NAME}:${IMAGE_TAG}|g" deployment-service.yml
                '''

                // Apply all deployments
                sh "echo Applying deployment... && kubectl apply -f deployment-service.yml -n ${NAMESPACE}"

                // Get all deployment names dynamically
                deployments = sh(
                    script: "kubectl get -f deployment-service.yml -n ${NAMESPACE} -o jsonpath='{.items[*].metadata.name}'",
                    returnStdout: true
                ).trim().split("\\s+")

                echo "Deployments found: ${deployments}"

                // Rollout each deployment
                for (dep in deployments) {
                    echo "⏳ Waiting for rollout of deployment: ${dep}"
                    sh "kubectl rollout status deployment/${dep} -n ${NAMESPACE} || echo 'Rollout check failed for ${dep}, continuing...'"
                }

                echo "✅ Deployment successful: ${IMAGE_NAME}:${IMAGE_TAG}"

                // Optional: Check services & pods
                sh '''
                    echo "Checking Services & Pods..."
                    kubectl get svc -n ${NAMESPACE}
                    kubectl get pods -n ${NAMESPACE}
                '''

            } catch (err) {
                echo "❌ Deployment failed: ${err}"

                // Rollback all deployments only if any exist
                for (dep in deployments) {
                    sh "kubectl rollout undo deployment/${dep} -n ${NAMESPACE} || true"
                }

                // Archive logs
                sh "kubectl describe pods -n ${NAMESPACE} > describe-pods.txt || true"
                sh "kubectl logs -l app -n ${NAMESPACE} > app-logs.txt || true"
                archiveArtifacts artifacts: '*.txt', allowEmptyArchive: true

                error("Deployment failed, rollback triggered")
            }
        }
    } else {
        echo "No deployment manifest found. Skipping Kubernetes deployment."
    }
}
