pipeline {
    agent any

    // Definición de herramientas
    tools {
        maven 'Maven3'
        jdk 'JDK17'
    }

    // Variables de entorno
    environment {
        // Configuración de Docker
        DOCKER_IMAGE = 'veterinaria-backend'
        DOCKER_TAG = "${BUILD_NUMBER}"
        CONTAINER_NAME = 'veterinaria-app'
        HOST_PORT = '8091'
        CONTAINER_PORT = '8080'

        // Configuración del proyecto
        SPRING_PROFILE = 'prod'
        APP_NAME = 'veterinaria-backend'

        // Red de Docker
        DOCKER_NETWORK = 'veterinaria-network'

        // Variables de control de despliegue
        DEPLOY_TIMEOUT = '60'
        HEALTH_CHECK_RETRIES = '6'
        HEALTH_CHECK_INTERVAL = '10'
    }

    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Environment Preparation') {
            steps {
                script {
                    echo 'Preparing environment...'
                    cleanWs()

                    bat 'java -version'
                    bat 'mvn -version'

                    bat """
                        docker network inspect ${DOCKER_NETWORK} > nul 2>&1 || docker network create ${DOCKER_NETWORK}
                    """
                }
            }
        }

        stage('Checkout') {
            steps {
                echo 'Checking out code...'
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/skazy12/veterinaria-backend.git'
            }
        }

        stage('Setup Firebase Credentials') {
            steps {
                script {
                    echo 'Setting up Firebase credentials...'
                    withCredentials([file(credentialsId: 'firebase-credentials', variable: 'FIREBASE_CONFIG')]) {
                        bat """
                            @echo off
                            if not exist "src\\main\\resources" mkdir "src\\main\\resources"
                            copy /Y "%FIREBASE_CONFIG%" "src\\main\\resources\\firebase-service-account.json"

                            if not exist "src\\main\\resources\\firebase-service-account.json" (
                                echo Error: Firebase credentials file not copied correctly
                                exit /b 1
                            ) else (
                                echo Firebase credentials file copied successfully
                            )
                        """
                    }
                }
            }
        }

        stage('Build Maven') {
            steps {
                echo 'Building application with Maven...'
                withEnv(['JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8']) {
                    bat 'mvn clean package -DskipTests -Dproject.build.sourceEncoding=UTF-8'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                echo 'Running unit tests...'
                bat 'mvn test'
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
                    echo 'Building Docker image...'
                    bat "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                    bat "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                }
            }
        }

       stage('Deploy Container') {
           steps {
               script {
                   echo 'Deploying container...'
                   withCredentials([
                       string(credentialsId: 'firebase-database-url', variable: 'FIREBASE_DB_URL'),
                       string(credentialsId: 'firebase-api-key', variable: 'FIREBASE_API'),
                       string(credentialsId: 'spring-mail-host', variable: 'MAIL_HOST'),
                       string(credentialsId: 'spring-mail-port', variable: 'MAIL_PORT'),
                       string(credentialsId: 'spring-mail-username', variable: 'MAIL_USER'),
                       string(credentialsId: 'spring-mail-password', variable: 'MAIL_PASS'),
                       string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET'),
                       string(credentialsId: 'jwt-expiration', variable: 'JWT_EXP')
                   ]) {
                       bat """
                           @echo off

                           rem Detener contenedor existente si existe
                           docker container inspect %CONTAINER_NAME% >nul 2>&1
                           if not errorlevel 1 (
                               docker container stop %CONTAINER_NAME% >nul 2>&1
                               docker container rm %CONTAINER_NAME% >nul 2>&1
                           )

                           ping -n 10 127.0.0.1 >nul

                           docker run -d ^
                               --name %CONTAINER_NAME% ^
                               --network %DOCKER_NETWORK% ^
                               -p %HOST_PORT%:%CONTAINER_PORT% ^
                               -e SPRING_PROFILES_ACTIVE=%SPRING_PROFILE% ^
                               -e FIREBASE_DATABASE_URL=%FIREBASE_DB_URL% ^
                               -e FIREBASE_API_KEY=%FIREBASE_API% ^
                               -e SPRING_MAIL_HOST=%MAIL_HOST% ^
                               -e SPRING_MAIL_PORT=%MAIL_PORT% ^
                               -e SPRING_MAIL_USERNAME=%MAIL_USER% ^
                               -e SPRING_MAIL_PASSWORD=%MAIL_PASS% ^
                               -e JWT_SECRET=%JWT_SECRET% ^
                               -e JWT_EXPIRATION=%JWT_EXP% ^
                               -e FIREBASE_CONFIG_PATH=firebase-service-account.json ^
                               -v veterinaria-data:/app/data ^
                               --restart unless-stopped ^
                               %DOCKER_IMAGE%:%DOCKER_TAG%

                           ping -n 15 127.0.0.1 >nul


                       """
                   }
               }
           }
       }
    }

    post {
        always {
            echo 'Cleaning up...'
            bat 'if exist "src\\main\\resources\\firebase-service-account.json" del /F /Q "src\\main\\resources\\firebase-service-account.json"'
            cleanWs()
        }

        success {
            echo """
                =========================================
                Pipeline executed successfully!
                Image: ${DOCKER_IMAGE}:${DOCKER_TAG}
                Container: ${CONTAINER_NAME}
                Application URL: http://localhost:${HOST_PORT}
                =========================================
            """
        }

        failure {
            echo """
                =========================================
                Pipeline failed!
                Check the logs above for details.
                Attempting rollback if necessary...
                =========================================
            """

            script {
                bat """
                    rem Verificar si existe el contenedor
                    docker container inspect ${CONTAINER_NAME} >nul 2>&1
                    if not errorlevel 1 (
                        echo Performing rollback...
                        docker stop ${CONTAINER_NAME} || true
                        docker rm ${CONTAINER_NAME} || true
                        timeout /t 5 /nobreak

                        docker run -d ^
                            --name ${CONTAINER_NAME} ^
                            --network ${DOCKER_NETWORK} ^
                            -p ${HOST_PORT}:${CONTAINER_PORT} ^
                            -e SPRING_PROFILES_ACTIVE=${SPRING_PROFILE} ^
                            -v veterinaria-data:/app/data ^
                            --restart unless-stopped ^
                            ${DOCKER_IMAGE}:latest
                    )
                """
            }
        }

        unstable {
            echo 'Pipeline is unstable! Check test results.'
        }
    }
}