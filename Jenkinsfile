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
                echo "Infratuzilmani tayyorlash va maxfiy fayllarni sozlash..."

                withCredentials([
                    file(credentialsId: 'NOT_UZ_PROD_ENV_FILE', variable: 'ENV_FILE_PATH'),
                    file(credentialsId: 'NOT_UZ_GOOGLE_KEY_JSON', variable: 'GOOGLE_KEY_TEMP')
                ]) {
                    sh """
                        # 1. Log papkasini va faylni tayyorlash
                        mkdir -p ${WORKSPACE}/logs

                        # 2. Google JSON faylini workspace-ga nusxalash
                        cp ${GOOGLE_KEY_TEMP} ${WORKSPACE}/google-key.json

                        # 3. Ruxsatlarni to'liq ochish (Permission Denied bo'lmasligi uchun)
                        chmod 777 ${WORKSPACE}/google-key.json
                        chmod -R 777 ${WORKSPACE}/logs

                        # 4. Eski konteynerni o'chirish
                        docker rm -f ${CONTAINER_NAME} || true

                        # 5. Konteynerni ishga tushirish
                        docker run -d \
                          --name "${CONTAINER_NAME}" \
                          -p ${APP_PORT}:8080 \
                          --network ${NETWORK_NAME} \
                          --restart unless-stopped \
                          --env-file "${ENV_FILE_PATH}" \
                          \
                          /* Mount qilish: hostdagi faylni konteynerning ildiziga (/google-key.json) bog'laymiz */
                          -v "${WORKSPACE}/google-key.json:/google-key.json" \
                          -v "${WORKSPACE}/logs:/app/logs" \
                          \
                          -e SPRING_PROFILES_ACTIVE=prod \
                          "${LATEST_IMAGE}"

                        # 6. Tekshirish (Debug uchun logda ko'rinadi)
                        echo "Konteyner ichidagi fayllar tekshirilmoqda:"
                        docker exec "${CONTAINER_NAME}" ls -la /google-key.json || echo "Fayl topilmadi!"
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