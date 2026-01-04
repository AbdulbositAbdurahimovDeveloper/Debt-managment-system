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
                        echo "Google Key va loglar infratuzilmasi tayyorlanmoqda..."

                        withCredentials([
                            file(credentialsId: 'NOT_UZ_PROD_ENV_FILE', variable: 'ENV_FILE_PATH'),
                            file(credentialsId: 'NOT_UZ_GOOGLE_KEY_JSON', variable: 'GOOGLE_KEY_TEMP')
                        ]) {
                            sh """
                                # 1. Logs papkasini yaratish va tozalash
                                mkdir -p ${WORKSPACE}/logs
                                rm -rf ${WORKSPACE}/logs/*

                                # 2. Google Key faylini aynan logs papkasi ichiga nusxalash
                                # Chunki logs papkasi konteynerga muvaffaqiyatli mount bo'ladi
                                cp ${GOOGLE_KEY_TEMP} ${WORKSPACE}/logs/google-key.json

                                # 3. Ruxsatlarni maksimal ochish
                                chmod -R 777 ${WORKSPACE}/logs

                                # 4. Eski konteynerni o'chirish
                                docker rm -f ${CONTAINER_NAME} || true

                                # 5. Konteynerni ishga tushirish
                                # DIQQAT: GOOGLE_SHEETS_CREDENTIALS_PATH orqali YAML'dagi yo'lni override qilamiz
                                docker run -d \\
                                  --name "${CONTAINER_NAME}" \\
                                  -p ${APP_PORT}:8080 \\
                                  --network ${NETWORK_NAME} \\
                                  --restart unless-stopped \\
                                  --env-file "${ENV_FILE_PATH}" \\
                                  -v "${WORKSPACE}/logs:/app/logs" \\
                                  -e SPRING_PROFILES_ACTIVE=prod \\
                                  -e GOOGLE_SHEETS_CREDENTIALS_PATH=/app/logs/google-key.json \\
                                  "${LATEST_IMAGE}"

                                # 6. Tekshirish
                                sleep 5
                                docker logs --tail 20 "${CONTAINER_NAME}"
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