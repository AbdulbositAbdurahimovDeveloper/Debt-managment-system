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
                // Maxfiy faylni build vaqtida ishlatish
                withCredentials([file(credentialsId: 'NOT_UZ_GOOGLE_KEY_JSON', variable: 'GOOGLE_KEY')]) {
                    sh """
                        # Faylni Docker build kontekstiga nusxalash
                        cp ${GOOGLE_KEY} google-key.json
                        docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} -t ${LATEST_IMAGE} .
                        # Xavfsizlik uchun builddan keyin faylni o'chirish
                        rm google-key.json
                    """
                }
            }
        }

       stage('3. Deploy Application') {
                   steps {
                       echo "Log papkasi va konteyner tayyorlanmoqda..."
                       sh """
                           # Serverdagi (host) log papkasini tozalash va ruxsat berish
                           mkdir -p ${WORKSPACE}/logs
                           chmod -R 777 ${WORKSPACE}/logs

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
                                 \
                                 # MUHIM: Ilovani root huquqi bilan yurgizish (ruxsat muammosini vaqtincha yechish uchun)
                                 # Yoki --user flagisiz yurgizish (Dockerfile userini ishlatadi)

                                 -v "${WORKSPACE}/logs:/app/logs" \
                                 -e SPRING_PROFILES_ACTIVE=prod \
                                 "${LATEST_IMAGE}"
                           """
                       }
                   }
               }
    }
}