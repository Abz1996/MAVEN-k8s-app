pipeline {
    agent any
    
    environment {
        NEXUS_URL = 'your-nexus-url:8082'
        NEXUS_REPO = 'docker-hosted'
        NEXUS_CREDENTIALS = 'nexus-credentials'
        APP_NAME = 'devops-app'
        DOCKER_IMAGE = "${NEXUS_URL}/${APP_NAME}"
        K8S_NAMESPACE = 'devops-demo'
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/YOUR_USERNAME/YOUR_REPO.git',
                    credentialsId: 'github-credentials'
            }
        }
        
        stage('Build with Maven') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${DOCKER_IMAGE}:${BUILD_NUMBER}")
                    docker.build("${DOCKER_IMAGE}:latest")
                }
            }
        }
        
        stage('Push to Nexus') {
            steps {
                script {
                    docker.withRegistry("http://${NEXUS_URL}", NEXUS_CREDENTIALS) {
                        docker.image("${DOCKER_IMAGE}:${BUILD_NUMBER}").push()
                        docker.image("${DOCKER_IMAGE}:latest").push()
                    }
                }
            }
        }
        
        stage('Clean Local Images') {
            steps {
                sh """
                    docker rmi ${DOCKER_IMAGE}:${BUILD_NUMBER} || true
                    docker rmi ${DOCKER_IMAGE}:latest || true
                """
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    sh """
                        kubectl apply -f k8s/namespace.yaml
                        kubectl apply -f k8s/deployment.yaml -n ${K8S_NAMESPACE}
                        kubectl apply -f k8s/service.yaml -n ${K8S_NAMESPACE}
                        kubectl rollout status deployment/devops-app -n ${K8S_NAMESPACE}
                    """
                }
            }
        }
        
        stage('Verify Deployment') {
            steps {
                sh """
                    kubectl get pods -n ${K8S_NAMESPACE}
                    kubectl get svc -n ${K8S_NAMESPACE}
                """
            }
        }
    }
    
    post {
        success {
            echo 'Pipeline executed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
        always {
            cleanWs()
        }
    }
}