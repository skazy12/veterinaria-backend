pipeline {
    agent any

    // Definición de herramientas
    tools {
        maven 'Maven3'  // Debe coincidir con el nombre configurado en Jenkins
        jdk 'JDK17'     // Debe coincidir con el nombre configurado en Jenkins
    }

    // Variables de entorno
    environment {
        // Configuración de Docker
        DOCKER_IMAGE = 'veterinaria-backend'
        DOCKER_TAG = "${BUILD_NUMBER}"
        CONTAINER_NAME = 'veterinaria-app'
        HOST_PORT = '8090'
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
        buildDiscarder(logRotator(numToKeepStr: '5'))
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
                            if not exist "src\\main\\resources" mkdir "src\\main\\resources"
                            copy /Y "%FIREBASE_CONFIG%" "src\\main\\resources\\firebase-service-account.json"
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

                    bat """
                        rem Detener y eliminar contenedor existente
                        docker container stop ${CONTAINER_NAME} || true
                        docker container rm ${CONTAINER_NAME} || true

                        rem Esperar que el puerto se libere
                        timeout /t 10 /nobreak

                        rem Verificar y liberar puerto si está en uso
                        netstat -ano | find "${HOST_PORT}" > nul
                        if not errorlevel 1 (
                            echo Puerto ${HOST_PORT} en uso. Intentando liberar...
                            FOR /F "tokens=5" %%P IN ('netstat -ano ^| find "${HOST_PORT}"') DO TaskKill /PID %%P /F
                            timeout /t 5 /nobreak
                        )

                        rem Desplegar nuevo contenedor
                        docker run -d ^
                            --name ${CONTAINER_NAME} ^
                            --network ${DOCKER_NETWORK} ^
                            -p ${HOST_PORT}:${CONTAINER_PORT} ^
                            -e SPRING_PROFILES_ACTIVE=${SPRING_PROFILE} ^
                            -v veterinaria-data:/app/data ^
                            --restart unless-stopped ^
                            ${DOCKER_IMAGE}:${DOCKER_TAG}

                        rem Esperar que el contenedor esté listo
                        timeout /t 15 /nobreak
                    """
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    echo 'Verifying deployment...'

                    bat """
                        rem Verificar que el contenedor está corriendo
                        docker container inspect -f '{{.State.Running}}' ${CONTAINER_NAME} || (
                            echo Container failed to start
                            exit 1
                        )

                        rem Mostrar logs del contenedor
                        docker logs ${CONTAINER_NAME}

                        rem Verificar que el puerto está respondiendo
                        timeout /t 5 /nobreak

                        rem Intentar health check múltiples veces
                        set /a attempts=0
                        :HEALTH_CHECK_LOOP
                        curl -f http://localhost:${HOST_PORT}/actuator/health
                        if errorlevel 1 (
                            set /a attempts+=1
                            if %attempts% lss %HEALTH_CHECK_RETRIES% (
                                timeout /t %HEALTH_CHECK_INTERVAL% /nobreak
                                goto HEALTH_CHECK_LOOP
                            ) else (
                                echo Health check failed after %HEALTH_CHECK_RETRIES% attempts
                                exit 1
                            )
                        )
                    """
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