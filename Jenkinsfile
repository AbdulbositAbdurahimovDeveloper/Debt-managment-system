pipeline {
    agent any

    environment {
        IMAGE_NAME = "debt-managment/app"
        CONTAINER_NAME = "debt-managment-container"
        LATEST_IMAGE = "${IMAGE_NAME}:latest"
        APP_PORT = "7214"
        NETWORK_NAME = "app-network"
        // SERVERDAGI FAYLNING ANIQ MANZILI (Sizning rasmingiz bo'yicha)
        HOST_GOOGLE_KEY = "/root/debt-managment/google-key.json"
    }

    stages {
        stage('1. Checkout') {
            steps {
                cleanWs()
                checkout scm
                echo "Kod yuklab olindi."
            }
        }

        stage('2. Build Docker Image') {
            steps {
                echo "Docker image qurilmoqda..."
                sh "docker build -t ${IMAGE_NAME}:${BUILD_NUMBER} -t ${LATEST_IMAGE} ."
            }
        }

        stage('3. Deploy Application') {
                    steps {
                        echo "Loglarni tayyorlash va deploy qilish..."
                        sh """
                            mkdir -p ${WORKSPACE}/logs
                            chmod -R 777 ${WORKSPACE}/logs
                            docker rm -f ${CONTAINER_NAME} || true
                        """

                        withCredentials([file(credentialsId: 'NOT_UZ_PROD_ENV_FILE', variable: 'ENV_FILE')]) {
                            // DIQQAT: sh '''...''' ishlatamiz. Bu xavfsizlik va sintaksis uchun eng zo'ri.
                            sh '''
                                docker run -d \
                                  --name "$CONTAINER_NAME" \
                                  -p $APP_PORT:8080 \
                                  --network $NETWORK_NAME \
                                  --restart unless-stopped \
                                  --env-file "$ENV_FILE" \
                                  -v "$HOST_GOOGLE_KEY:/google-key.json" \
                                  -v "$WORKSPACE/logs:/app/logs" \
                                  -e SPRING_PROFILES_ACTIVE=prod \
                                  "$LATEST_IMAGE"
                            '''
                        }
                        echo "Ilova muvaffaqiyatli ishga tushirildi!"
                    }
                }

        stage('4. Cleanup') {
            steps {
                sh "docker image prune -f"
            }
        }
    }
}