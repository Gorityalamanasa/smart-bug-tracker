// ============================================
// Smart Bug Tracker — Jenkins CI/CD Pipeline
// ============================================

pipeline {
    agent any

    environment {
        APP_NAME = 'smart-bug-tracker'
        APP_VERSION = '1.0.0'
        DOCKER_IMAGE = "bugtracker:${APP_VERSION}"
    }

    tools {
        maven 'Maven-3.9'
    }

    stages {
        stage('Checkout') {
            steps {
                echo '📥 Checking out source code...'
                checkout scm
            }
        }

        stage('Build with Maven') {
            steps {
                echo '🔨 Building with Maven...'
                bat 'mvn clean compile -q'
            }
        }

        stage('Run Tests') {
            steps {
                echo '🧪 Running unit tests...'
                bat 'mvn test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package with Maven') {
            steps {
                echo '📦 Packaging application...'
                bat 'mvn package -DskipTests -q'
            }
        }

        stage('Build with Gradle') {
            steps {
                echo '🔨 Verifying Gradle build...'
                bat 'gradlew.bat build -x test --no-daemon'
            }
        }

        stage('Docker Build') {
            steps {
                echo '🐳 Building Docker image...'
                bat "docker build -t ${DOCKER_IMAGE} ."
            }
        }

        stage('Deploy') {
            steps {
                echo '🚀 Deploying application...'
                bat 'docker-compose down --remove-orphans || true'
                bat 'docker-compose up -d bugtracker'
            }
        }

        stage('Health Check') {
            steps {
                echo '❤️ Verifying deployment...'
                sleep(time: 15, unit: 'SECONDS')
                bat 'curl -f http://localhost:8080/api/health'
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
            echo "Application is running at http://localhost:8080"
        }
        failure {
            echo '❌ Pipeline failed. Check logs above for details.'
        }
        always {
            echo '📊 Pipeline finished. Cleaning up workspace.'
            cleanWs(cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true)
        }
    }
}
