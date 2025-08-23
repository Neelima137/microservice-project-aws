pipeline {
    agent any

    environment {
        IMAGE_NAME = "483898563284.dkr.ecr.ap-south-1.amazonaws.com/webapps/microservice"
        IMAGE_TAG  = "${env.BUILD_NUMBER}"   
    }

    stages {
        stage('Deploy To Kubernetes') {
            steps {
                withKubeCredentials(kubectlCredentials: [[
                    caCertificate: '',
                    clusterName: 'microservices',
                    contextName: '',
                    credentialsId: 'k8s-token',
                    namespace: 'webapps',
                    serverUrl: 'https://DF85F618DC81B5AD5E6C93FC8F4DE955.gr7.ap-south-1.eks.amazonaws.com'
                ]]) {
                    sh '''
                        echo " Updating manifest with new image..."
                        sed -i "s|IMAGE_PLACEHOLDER|${IMAGE_NAME}:${IMAGE_TAG}|g" deployment-service.yml
                        
                        echo " Applying deployment..."
                        kubectl apply -f deployment-service.yml -n webapps

                        echo " Waiting for rollout to finish..."
                        kubectl rollout status deployment/microservice-app -n webapps
                    '''
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                withKubeCredentials(kubectlCredentials: [[
                    caCertificate: '',
                    clusterName: 'microservices',
                    contextName: '',
                    credentialsId: 'k8s-token',
                    namespace: 'webapps',
                    serverUrl: 'https://DF85F618DC81B5AD5E6C93FC8F4DE955.gr7.ap-south-1.eks.amazonaws.com'
                ]]) {
                    sh '''
                        echo " Checking Services & Pods..."
                        kubectl get svc -n webapps
                        kubectl get pods -n webapps
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "🎉 Deployment successful: ${IMAGE_NAME}:${IMAGE_TAG}"
        }
        failure {
            echo "❌ Deployment failed. Check logs."
        }
    }
}
