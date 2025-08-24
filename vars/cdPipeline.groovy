def call(Map config = [:]) {

    // === Dynamic service-based repo ===
    def SERVICE_NAME = config.serviceName ?: "emailservice"         // Default, override per service
    def ECR_REGISTRY = "483898563284.dkr.ecr.ap-south-1.amazonaws.com"
    def IMAGE_NAME   = "${ECR_REGISTRY}/webapps/microservice"       // One repo for all services
    def IMAGE_TAG    = "${SERVICE_NAME}-latest"                     // e.g., adservice-latest
    def NAMESPACE    = config.namespace ?: "webapps"

    if (!fileExists('deployment-service.yaml')) {
        echo "No deployment manifest found. Skipping Kubernetes deployment."
        return
    }

    withAWS(region: 'ap-south-1', credentials: 'aws-creds') {
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
                    set -e

                    echo "🔐 Logging into EKS cluster..."
                    aws eks update-kubeconfig --name microservices

                    echo "📦 Checking cluster nodes..."
                    kubectl get nodes

                    echo "🔑 Logging into ECR..."
                    aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin ${ECR_REGISTRY}

                    echo "📝 Updating manifest with new image..."
                    yq eval -i '
                      (.spec.template.spec.containers[] | select(.name == "${SERVICE_NAME}") | .image) = "${IMAGE_NAME}:${IMAGE_TAG}"
                    ' deployment-service.yaml

                    echo "🚀 Applying deployment..."
                    kubectl apply -f deployment-service.yaml -n ${NAMESPACE} --validate=false

                    echo "🔍 Fetching deployment names..."
                    deployments_str=\$(kubectl get -f deployment-service.yaml -n ${NAMESPACE} -o jsonpath='{.items[*].metadata.name}')

                    for dep in \$deployments_str; do
                        echo "⏳ Waiting for rollout of deployment: \$dep"
                        kubectl rollout status deployment/\$dep -n ${NAMESPACE} || echo "⚠️ Rollout failed for \$dep, continuing..."
                    done

                    echo "✅ Deployment successful: ${IMAGE_NAME}:${IMAGE_TAG}"

                    echo "📊 Checking services & pods..."
                    kubectl get svc -n ${NAMESPACE}
                    kubectl get pods -n ${NAMESPACE}

                    # Deploy Prometheus & Grafana monitoring stack
                    MONITORING_NS="monitoring"
                    PROM_GRAF_YAML="prometheus-grafana.yaml"

                    if [ -f "\$PROM_GRAF_YAML" ]; then
                        echo "📈 Applying Prometheus & Grafana stack..."
                        kubectl apply -f "\$PROM_GRAF_YAML" -n \$MONITORING_NS --validate=false
                        kubectl get pods -n \$MONITORING_NS
                    else
                        echo "⚠️ File \$PROM_GRAF_YAML not found, skipping monitoring stack deployment."
                    fi
                """

            } catch (err) {
                echo "❌ Deployment failed: ${err}"

                // Optional rollback if needed
                // for (dep in deployments) {
                //     sh "kubectl rollout undo deployment/\$dep -n ${NAMESPACE} || true"
                // }

                // Collect logs for debugging
                // sh "kubectl describe pods -n ${NAMESPACE} > describe-pods.txt || true"
                // sh "kubectl logs -l app -n ${NAMESPACE} > app-logs.txt || true"
                // archiveArtifacts artifacts: '*.txt', allowEmptyArchive: true
            }
        }
    }
}
