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
                echo "Log infratuzilmasi tayyorlanmoqda..."
                sh '''
                    # 1. Eski loglarni tozalash (Permission muammosini oldini olish uchun)
                    rm -rf "$WORKSPACE/logs"

                    # 2. Logback kutayotgan barcha ichki papkalarni oldindan yaratish
                    mkdir -p "$WORKSPACE/logs/errorlog"
                    mkdir -p "$WORKSPACE/logs/log200"
                    mkdir -p "$WORKSPACE/logs/log400"
                    mkdir -p "$WORKSPACE/logs/log500"
                    mkdir -p "$WORKSPACE/logs/archived"
                    mkdir -p "$WORKSPACE/logs/errorlog/archived"
                    mkdir -p "$WORKSPACE/logs/log200/archived"
                    mkdir -p "$WORKSPACE/logs/log400/archived"
                    mkdir -p "$WORKSPACE/logs/log500/archived"

                    # 3. Konteyner ichidagi user (UID 101) yozishi uchun to'liq ruxsat berish
                    chmod -R 777 "$WORKSPACE/logs"
                '''

                sh "docker rm -f $CONTAINER_NAME || true"

                withCredentials([
                    file(credentialsId: 'NOT_UZ_PROD_ENV_FILE', variable: 'ENV_FILE'),
                    file(credentialsId: 'NOT_UZ_GOOGLE_KEY_JSON', variable: 'GOOGLE_KEY_FILE')
                ]) {
                    sh '''
                        docker run -d \
                          --name "$CONTAINER_NAME" \
                          -p $APP_PORT:8080 \
                          --network $NETWORK_NAME \
                          --restart unless-stopped \
                          --env-file "$ENV_FILE" \
                          -v "$GOOGLE_KEY_FILE:/google-key.json" \
                          -v "$WORKSPACE/logs:/app/logs" \
                          -e SPRING_PROFILES_ACTIVE=prod \
                          "$LATEST_IMAGE"
                    '''
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