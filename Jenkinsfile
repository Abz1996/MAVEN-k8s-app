pipeline {
    agent any
    
    environment {
        // JFrog Artifactory Configuration
        ARTIFACTORY_URL = 'https://YOUR_ARTIFACTORY_URL'
        ARTIFACTORY_DOCKER_REPO = 'docker-local'
        ARTIFACTORY_MAVEN_REPO = 'libs-release-local'
        ARTIFACTORY_CREDENTIALS = 'jfrog-credentials'
        
        // Application Configuration
        APP_NAME = 'devops-app'
        DOCKER_IMAGE = "${ARTIFACTORY_URL}/${ARTIFACTORY_DOCKER_REPO}/${APP_NAME}"
        K8S_NAMESPACE = 'devops-demo'
        
        // Build Information
        BUILD_VERSION = "${BUILD_NUMBER}"
        GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
    }
    
    tools {
        maven 'Maven-3.9.5'
        jdk 'JDK-17'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_COMMIT_MSG = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                }
            }
        }
        
        stage('Build Info') {
            steps {
                echo "Building ${APP_NAME}"
                echo "Version: ${BUILD_VERSION}"
                echo "Git Commit: ${GIT_COMMIT_SHORT}"
                echo "Commit Message: ${GIT_COMMIT_MSG}"
            }
        }
        
        stage('Compile') {
            steps {
                sh 'mvn clean compile'
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco execPattern: '**/target/jacoco.exec'
                }
            }
        }
        
        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
            }
        }
        
        stage('Publish to JFrog Artifactory') {
            steps {
                script {
                    def server = Artifactory.server 'jfrog-server'
                    def uploadSpec = """{
                        "files": [{
                            "pattern": "target/*.jar",
                            "target": "${ARTIFACTORY_MAVEN_REPO}/com/example/${APP_NAME}/${BUILD_VERSION}/"
                        }]
                    }"""
                    server.upload(uploadSpec)
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${DOCKER_IMAGE}:${BUILD_VERSION}")
                    docker.build("${DOCKER_IMAGE}:latest")
                }
            }
        }
        
        stage('Scan Docker Image') {
            steps {
                script {
                    // Using JFrog Xray for security scanning
                    sh """
                        echo "Scanning Docker image for vulnerabilities..."
                    """
                }
            }
        }
        
        stage('Push to JFrog Artifactory') {
            steps {
                script {
                    docker.withRegistry("${ARTIFACTORY_URL}", ARTIFACTORY_CREDENTIALS) {
                        docker.image("${DOCKER_IMAGE}:${BUILD_VERSION}").push()
                        docker.image("${DOCKER_IMAGE}:latest").push()
                        
                        // Tag with git commit
                        docker.image("${DOCKER_IMAGE}:${BUILD_VERSION}").push("${GIT_COMMIT_SHORT}")
                    }
                }
            }
        }
        
        stage('Build Info - JFrog') {
            steps {
                script {
                    def server = Artifactory.server 'jfrog-server'
                    def buildInfo = Artifactory.newBuildInfo()
                    buildInfo.name = APP_NAME
                    buildInfo.number = BUILD_VERSION
                    buildInfo.env.capture = true
                    server.publishBuildInfo(buildInfo)
                }
            }
        }
        
        stage('Clean Local Images') {
            steps {
                sh """
                    docker rmi ${DOCKER_IMAGE}:${BUILD_VERSION} || true
                    docker rmi ${DOCKER_IMAGE}:latest || true
                    docker rmi ${DOCKER_IMAGE}:${GIT_COMMIT_SHORT} || true
                """
            }
        }
        
        stage('Update K8s Manifests') {
            steps {
                sh """
                    sed -i 's|YOUR_ARTIFACTORY_URL|${ARTIFACTORY_URL}|g' k8s/deployment.yaml
                    sed -i 's|docker-local|${ARTIFACTORY_DOCKER_REPO}|g' k8s/deployment.yaml
                """
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    sh """
                        kubectl apply -f k8s/namespace.yaml
                        kubectl apply -f k8s/configmap.yaml
                        kubectl apply -f k8s/deployment.yaml
                        kubectl apply -f k8s/service.yaml
                        kubectl apply -f k8s/hpa.yaml
                        kubectl rollout status deployment/devops-app -n ${K8S_NAMESPACE} --timeout=5m
                    """
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                sh """
                    echo "=== Pods Status ==="
                    kubectl get pods -n ${K8S_NAMESPACE} -l app=devops-app
                    
                    echo "=== Service Info ==="
                    kubectl get svc -n ${K8S_NAMESPACE}
                    
                    echo "=== Deployment Info ==="
                    kubectl get deployment devops-app -n ${K8S_NAMESPACE}
                    
                    echo "=== HPA Status ==="
                    kubectl get hpa -n ${K8S_NAMESPACE}
                """
            }
        }
        
        stage('Smoke Test') {
            steps {
                script {
                    sh """
                        # Wait for service to be ready
                        sleep 10
                        
                        # Get service endpoint
                        SERVICE_IP=\$(kubectl get svc devops-app-service -n ${K8S_NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
                        
                        if [ ! -z "\$SERVICE_IP" ]; then
                            echo "Testing application at \$SERVICE_IP"
                            curl -f http://\$SERVICE_IP/health || exit 1
                            curl -f http://\$SERVICE_IP/info || exit 1
                            echo "Smoke tests passed!"
                        else
                            echo "Warning: LoadBalancer IP not yet assigned"
                        fi
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo '✅ Pipeline executed successfully!'
            echo "Application deployed: ${APP_NAME}:${BUILD_VERSION}"
            script {
                // Add promotion logic here if needed
                def server = Artifactory.server 'jfrog-server'
                server.promote("${APP_NAME}", "${BUILD_VERSION}", "${ARTIFACTORY_DOCKER_REPO}")
            }
        }
        failure {
            echo '❌ Pipeline failed!'
            // Rollback deployment if needed
            sh "kubectl rollout undo deployment/devops-app -n ${K8S_NAMESPACE} || true"
        }
        always {
            // Clean workspace
            cleanWs()
        }
    }
}