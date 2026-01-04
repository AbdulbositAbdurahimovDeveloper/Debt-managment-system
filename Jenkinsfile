pipeline {
    agent any

    environment {
        IMAGE_NAME = "debt-managment/app"
        CONTAINER_NAME = "debt-managment-container"
        LATEST_IMAGE = "${IMAGE_NAME}:latest"
        APP_PORT = "7214"
        NETWORK_NAME = "app-network"
    }

    stages {
        stage('1. Checkout') {
            steps {
                cleanWs()
                checkout scm
                echo "Kod GitHub'dan muvaffaqiyatli olindi."
            }
        }

        stage('2. Build Docker Image') {
            steps {
                script {
                    echo "Docker image qurilmoqda: ${LATEST_IMAGE}"
                    // Dockerfile ichida Maven build (multi-stage) bor deb hisoblaymiz
                    sh "docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} -t ${LATEST_IMAGE} ."
                }
            }
        }

        stage('3. Deploy Application') {
            steps {
                sh """
                    # Serverdagi log papkasini yaratish
                    mkdir -p /var/lib/jenkins/workspace/DEBT_NOT_UZ/logs

                    # Ichki papkalarni oldindan yaratib qo'yish (Logback xato bermasligi uchun)
                    mkdir -p /var/lib/jenkins/workspace/DEBT_NOT_UZ/logs/errorlog
                    mkdir -p /var/lib/jenkins/workspace/DEBT_NOT_UZ/logs/log200
                    mkdir -p /var/lib/jenkins/workspace/DEBT_NOT_UZ/logs/log400
                    mkdir -p /var/lib/jenkins/workspace/DEBT_NOT_UZ/logs/log500
                    mkdir -p /var/lib/jenkins/workspace/DEBT_NOT_UZ/logs/archived

                    # Eng muhimi: barcha papkalarga to'liq ruxsat berish
                    chmod -R 777 /var/lib/jenkins/workspace/DEBT_NOT_UZ/logs
                """

                sh "docker rm -f ${CONTAINER_NAME} || true"

                withCredentials([
                    file(credentialsId: 'NOT_UZ_PROD_ENV_FILE', variable: 'ENV_FILE'),
                    file(credentialsId: 'NOT_UZ_GOOGLE_KEY_JSON', variable: 'GOOGLE_KEY_FILE')
                ]) {
                    sh """
                        docker run -d \
                          --name "${CONTAINER_NAME}" \
                          -p ${APP_PORT}:8080 \
                          --network ${NETWORK_NAME} \
                          --restart unless-stopped \
                          --env-file "${ENV_FILE}" \
                          -v "${GOOGLE_KEY_FILE}:/google-key.json" \
                          -v /var/lib/jenkins/workspace/DEBT_NOT_UZ/logs:/app/logs \
                          -e SPRING_PROFILES_ACTIVE=prod \
                          "${LATEST_IMAGE}"
                    """
                }
            }
        }

        stage('4. Cleanup') {
            steps {
                echo "Eski va keraksiz Docker imagelar tozalanmoqda..."
                sh "docker image prune -f"
            }
        }
    }

    post {
        success {
            echo "CI/CD jarayoni muvaffaqiyatli yakunlandi!"
        }
        failure {
            echo "Pipeline xatolik bilan tugadi. Loglarni tekshiring!"
        }
    }
}