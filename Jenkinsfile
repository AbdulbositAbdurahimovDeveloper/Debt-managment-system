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
            }
        }

        stage('2. Build Docker Image') {
            steps {
                echo "Google Key-ni image ichiga joylash va Build qilish..."
                // Jenkins Credentials-dagi Secret File-ni chaqiramiz
                withCredentials([file(credentialsId: 'NOT_UZ_GOOGLE_KEY_JSON', variable: 'SECRET_KEY')]) {
                    sh """
                        # Maxfiy faylni Dockerfile turgan joyga nusxalash
                        cp ${SECRET_KEY} google-key.json

                        # Docker build (fayl endi image ichiga kirib ketadi)
                        docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} -t ${LATEST_IMAGE} .

                        # Xavfsizlik uchun vaqtinchalik nusxasini o'chirish
                        rm google-key.json
                    """
                }
            }
        }

        stage('3. Deploy Application') {
            steps {
                echo "Konteynerni ishga tushirish..."
                sh """
                    # Loglar uchun papka tayyorlash
                    mkdir -p ${WORKSPACE}/logs
                    chmod -R 777 ${WORKSPACE}/logs

                    # Eski konteynerni o'chirish
                    docker rm -f ${CONTAINER_NAME} || true
                """

                withCredentials([file(credentialsId: 'NOT_UZ_PROD_ENV_FILE', variable: 'ENV_FILE')]) {
                    sh """
                        docker run -d \
                          --name "${CONTAINER_NAME}" \
                          -p ${APP_PORT}:8080 \
                          --network ${NETWORK_NAME} \
                          --restart unless-stopped \
                          --env-file "${ENV_FILE}" \
                          -v "${WORKSPACE}/logs:/app/logs" \
                          -e SPRING_PROFILES_ACTIVE=prod \
                          "${LATEST_IMAGE}"
                    """
                }
                echo "Ilova muvaffaqiyatli ishga tushirildi!"
            }
        }
    }
}