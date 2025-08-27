Automated CI/CD for E-commerce Microservices on AWS
Abstract
This paper presents a comprehensive case study on the design and implementation of an automated Continuous Integration/Continuous Deployment (CI/CD) pipeline for a microservices-based e-commerce platform, "Online Boutique." The architecture leverages a combination of open-source and cloud-native technologies, including Jenkins, Docker, AWS ECR, and EKS. The methodology focuses on creating a highly scalable and reusable pipeline using Jenkins Shared Libraries and dynamic parameter passing. We demonstrate how this approach facilitates a rapid, secure, and reliable software delivery lifecycle, validated by continuous code quality, security scanning, and production monitoring with Prometheus and Grafana.
________________________________________
1. Introduction
The modern software landscape, dominated by agile methodologies and microservices architectures, demands robust and efficient deployment pipelines. For a complex e-commerce platform, managing dozens of independently-deployable services can become a significant bottleneck without proper automation. This project addresses this challenge by implementing a standardized, end-to-end CI/CD solution that ensures consistency, accelerates time-to-market, and embeds security and quality checks directly into the development workflow.
________________________________________
2. Architectural Overview
The solution architecture is a multi-stage pipeline triggered by a code commit to a Git repository. Each microservice resides in its own branch, and a Jenkins Multibranch Pipeline automatically detects these branches. The process flows as follows:
1.	Code Commit: A developer commits code to a service-specific branch.
2.	CI Pipeline Trigger: Jenkins detects the commit and initiates the CI pipeline.
3.	Code & Image Security: The code is scanned for secrets and vulnerabilities, and the resulting Docker image is scanned before being pushed to a private registry.
4.	Container Registry: The validated Docker image is stored in AWS ECR.
5.	CD Pipeline Trigger: A post-CI step triggers the CD pipeline for deployment.
6.	Deployment to Kubernetes: The latest image is deployed to the AWS EKS cluster.
7.	Monitoring: The deployed services and cluster nodes are continuously monitored.
The Jenkins multibranch dashboard provides a clear overview of all service pipelines, confirming that each microservice is managed independently.
________________________________________
3. Tools and Technologies
The pipeline is built on a robust stack of purpose-built tools:
Tool	Role in the Pipeline
Jenkins	Serves as the central automation server, orchestrating all CI/CD stages.
Jenkins Shared Libraries	Houses reusable Groovy scripts to maintain a single source of truth for pipeline logic across all microservices.
Git & GitHub	Used for source code management, with a branch-per-service model.
Docker	Used for containerization, providing a consistent runtime environment for each service.
AWS ECR	A private, secure container registry for storing microservice images.
AWS EKS	A managed Kubernetes service providing a scalable and resilient environment for deployment.
SonarQube	Provides static code analysis to enforce quality gates and track code health.
Gitleaks	Scans the codebase for hardcoded secrets, a critical security check.
Trivy	Scans container images for operating system and application vulnerabilities.
Prometheus & Grafana	An open-source monitoring stack used for collecting and visualizing metrics from the EKS cluster and services.
Export to Sheets
________________________________________

4. Pipeline Implementation and Automation
The core innovation of this architecture lies in its use of Jenkins Shared Libraries to achieve high reusability and scalability. The complex logic for both CI and CD is abstracted into Groovy functions, which are then called by simple Jenkinsfiles in each service repository.

4.1. Continuous Integration (CI) Pipeline
The CI pipeline is defined by a reusable Groovy function (ciPipeline) that accepts dynamic parameters. As seen in the Jenkins Stage View screenshot, the pipeline stages are executed sequentially:
•	Checkout: Pulls the specific microservice's code.
•	Gitleaks Scan: Scans the code for secrets.
•	SonarQube Scan: Performs a code quality analysis. The provided SonarQube dashboard screenshot confirms the successful execution, showing a Quality Gate Passed for the "Hipstershop Ad Service," with zero critical security issues found.
•	Docker Build & Push: Builds the Docker image and pushes it to ECR.
•	Trivy Scan: Scans the newly created container image for vulnerabilities.

4.2. Continuous Deployment (CD) Pipeline
The CD pipeline is defined in a separate Groovy function (cdPipeline) and is triggered upon a successful CI build. It receives dynamic parameters from the main Jenkinsfile to know which service and image to deploy. The key steps include:
•	Authentication: The pipeline securely authenticates with both AWS and the EKS cluster.
•	Dynamic Manifest Update: The pipeline uses the yq command-line tool to programmatically update the deployment-service.yaml file with the latest image tag. This is a crucial step that allows a single deployment manifest to be reused across all microservices.
•	Deployment & Rollout: The updated manifest is applied to the webapps namespace in EKS. The pipeline then waits for a successful rollout, ensuring the new pods are running and healthy.
________________________________________
The Core Workflow
The architecture is centered around a Jenkins Multibranch Pipeline that monitors a Git repository. Each microservice (e.g., adservice, cartservice) resides in its own branch. When a developer pushes code, the pipeline automatically starts.
1.	Code & Repository: The developer pushes code to the designated microservice branch. The repository also contains the Dockerfile and deployment-service.yaml for each service.
2.	Jenkins Automation: The multibranch pipeline detects the change and triggers a build. It first runs the ciPipeline script, which handles code quality and security scans, and then the cdPipeline script, which handles deployment.
3.	Security & Quality Scanning: The pipeline uses Gitleaks to find secrets in the code and SonarQube to perform static code analysis, ensuring code quality before a container is built.
4.	Containerization & Registry: The Dockerfile is used to build a container image, which is then pushed to a private AWS ECR repository. A Trivy scan is performed on the image for vulnerabilities before it is stored.
5.	Deployment to EKS: The pipeline dynamically updates the Kubernetes manifest with the new image tag and applies it to the AWS EKS cluster.
6.	Live Application: The deployed microservices become part of the live e-commerce application, "Online Boutique."
7.	Monitoring & Visibility: Prometheus scrapes metrics from the EKS cluster and the services. Grafana visualizes these metrics on dashboards, providing continuous visibility into the health and performance of the entire system.
________________________________________
. Key Architectural Components
•	Repository: A single repository with a branch-per-service strategy. This keeps the project organized while leveraging Jenkins' multibranch capabilities.
•	Jenkins: Acts as the central orchestrator, using Shared Libraries to provide a standardized, reusable pipeline for all microservices. Dynamic parameters pass service-specific information at runtime.
•	AWS: The pipeline relies on AWS cloud services for key functions:
o	ECR: Provides a secure and private location to store and manage Docker images.
o	EKS: The scalable and resilient environment where the microservices are deployed.
•	Security & Quality: A multi-layered security approach is embedded directly into the pipeline, including Gitleaks (code), SonarQube (code quality), and Trivy (container image).
•	Monitoring: The Prometheus and Grafana stack is a dedicated, independent component that provides operational visibility and ensures the health of the application and its infrastructure.

5. Results and Validation
The effectiveness of this pipeline is demonstrated by the visual evidence of a successful deployment and a functional monitoring stack.
•	Successful Deployment: The screenshot of the "Online Boutique" website confirms that the microservices have been successfully deployed and are operating as intended in the production environment. The application is live and accessible to end-users.
•	Comprehensive Monitoring: The Grafana dashboard shows real-time metrics for the EKS cluster nodes, including CPU, memory, network, and disk usage. This confirms that Prometheus is successfully collecting metrics and that Grafana is providing a centralized, visual dashboard for operational insight. This proactive monitoring is essential for maintaining application reliability and performance.
________________________________________
6. Conclusion
This automated CI/CD pipeline represents a significant improvement in the software delivery process for the e-commerce platform. By standardizing the workflow with Jenkins Shared Libraries and automating everything from security checks to deployment, the system has achieved:
•	Increased Velocity: Faster, more frequent, and more reliable deployments.
•	Enhanced Security: Integrated scanning tools ensure vulnerabilities and secrets are caught early in the development lifecycle.
•	Improved Reliability: The automated rollout process and continuous monitoring reduce the risk of downtime and facilitate rapid issue resolution.

 


 

 


 

<img width="954" height="2017" alt="image" src="https://github.com/user-attachments/assets/13e5a246-4e75-4ad3-a1d1-5541f4e8b590" />

<img width="940" height="445" alt="image" src="https://github.com/user-attachments/assets/58aff0d7-50a2-43a5-bf6d-5cfe8f9bf6e6" />
<img width="940" height="440" alt="image" src="https://github.com/user-attachments/assets/7de80ffd-4757-43db-859e-5a00bf288418" />
<img width="940" height="435" alt="image" src="https://github.com/user-attachments/assets/01d21827-2e09-406b-9876-d623654e4e7a" />
<img width="940" height="405" alt="image" src="https://github.com/user-attachments/assets/9ed67c0d-ec84-4162-b253-7ad1ac27e24b" />
<img width="940" height="460" alt="image" src="https://github.com/user-attachments/assets/0ad0d009-87e6-4cd0-b850-1abce00d49bd" />
<img width="940" height="248" alt="image" src="https://github.com/user-attachments/assets/bbf39d29-2905-4b7d-bfda-dddb350f43d7" />
<img width="940" height="443" alt="image" src="https://github.com/user-attachments/assets/ba66415d-2be4-49b6-b00f-d8b947e3d213" />
<img width="940" height="533" alt="image" src="https://github.com/user-attachments/assets/a8c2bfc0-4ff0-4d1c-bd5d-ebb660b681dc" />
<img width="940" height="360" alt="image" src="https://github.com/user-attachments/assets/539f7ebe-33b4-4058-80f9-30ed341e6458" />


 


 

 

 


 

 

 

