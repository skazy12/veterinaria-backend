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
        HOST_PORT = '8090'          // Puerto en tu máquina local
        CONTAINER_PORT = '8080'     // Puerto dentro del contenedor

        // Configuración del proyecto
        SPRING_PROFILE = 'prod'
        APP_NAME = 'veterinaria-backend'

        // Red de Docker
        DOCKER_NETWORK = 'veterinaria-network'
    }

    // Opciones generales
    options {
        // No permitir ejecuciones concurrentes
        disableConcurrentBuilds()
        // Timeout global
        timeout(time: 1, unit: 'HOURS')
        // Mantener solo los últimos 5 builds
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    stages {
        // Etapa de preparación del ambiente
        stage('Environment Preparation') {
            steps {
                script {
                    echo 'Preparing environment...'
                    // Limpiar workspace
                    cleanWs()

                    // Verificar versiones de herramientas
                    bat 'java -version'
                    bat 'mvn -version'

                    // Crear red de Docker si no existe
                    bat """
                        docker network inspect ${DOCKER_NETWORK} > nul 2>&1 || docker network create ${DOCKER_NETWORK}
                    """
                }
            }
        }

        // Etapa de checkout del código
        stage('Checkout') {
            steps {
                echo 'Checking out code...'
                // Usar credenciales de GitHub configuradas
                git branch: 'main',
                    credentialsId: 'github-credentials',
                    url: 'https://github.com/skazy12/veterinaria-backend.git'  // Reemplazar con tu URL
            }
        }

        // Etapa de configuración de Firebase
        stage('Setup Firebase Credentials') {
            steps {
                script {
                    echo 'Setting up Firebase credentials...'
                    // Copiar archivo de credenciales de Firebase de forma segura
                    withCredentials([file(credentialsId: 'firebase-credentials', variable: 'FIREBASE_CONFIG')]) {
                        bat """
                            if not exist "src\\main\\resources" mkdir "src\\main\\resources"
                            copy /Y "%FIREBASE_CONFIG%" "src\\main\\resources\\firebase-service-account.json"
                        """
                    }
                }
            }
        }

        // Etapa de compilación con Maven
        stage('Build Maven') {
            steps {
                echo 'Building application with Maven...'
                // Compilar con encoding específico
                withEnv(['JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8']) {
                    bat 'mvn clean package -DskipTests -Dproject.build.sourceEncoding=UTF-8'
                }
            }
        }

        // Etapa de pruebas unitarias
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

        // Etapa de construcción de imagen Docker
        stage('Build Docker Image') {
            steps {
                script {
                    echo 'Building Docker image...'
                    // Construir imagen
                    bat "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
                    bat "docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest"
                }
            }
        }

        // Etapa de despliegue del contenedor
        stage('Deploy Container') {
            steps {
                script {
                    echo 'Deploying container...'

                    // Detener y eliminar contenedor anterior si existe
                    bat """
                        docker stop ${CONTAINER_NAME} 2>nul || exit 0
                        docker rm ${CONTAINER_NAME} 2>nul || exit 0
                    """

                    // Ejecutar nuevo contenedor
                    bat """
                        docker run -d ^
                            --name ${CONTAINER_NAME} ^
                            --network ${DOCKER_NETWORK} ^
                            -p ${HOST_PORT}:${CONTAINER_PORT} ^
                            -e SPRING_PROFILES_ACTIVE=${SPRING_PROFILE} ^
                            -v veterinaria-data:/app/data ^
                            --restart unless-stopped ^
                            ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """

                    // Esperar a que la aplicación esté disponible
                    bat """
                        timeout /t 30 /nobreak
                        echo Checking application health...
                        for /l %%x in (1, 1, 10) do (
                            curl -f http://localhost:${HOST_PORT}/actuator/health && exit 0 || timeout /t 5 /nobreak
                        )
                    """
                }
            }
        }

        // Etapa de verificación post-despliegue
        stage('Verify Deployment') {
            steps {
                script {
                    echo 'Verifying deployment...'

                    // Verificar estado del contenedor
                    bat """
                        docker ps | find "${CONTAINER_NAME}"
                        if errorlevel 1 (
                            echo Container not running!
                            exit 1
                        )
                    """

                    // Mostrar logs del contenedor
                    bat "docker logs ${CONTAINER_NAME}"
                }
            }
        }
    }

    // Acciones post-ejecución
    post {
        always {
            echo 'Cleaning up...'
            // Limpiar archivo de credenciales de Firebase
            bat 'if exist "src\\main\\resources\\firebase-service-account.json" del /F /Q "src\\main\\resources\\firebase-service-account.json"'
            // Limpiar workspace
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

            // Intentar rollback a la versión anterior
            script {
                bat """
                    if exist "${CONTAINER_NAME}" (
                        docker stop ${CONTAINER_NAME}
                        docker rm ${CONTAINER_NAME}
                        docker run -d --name ${CONTAINER_NAME} -p ${HOST_PORT}:${CONTAINER_PORT} ${DOCKER_IMAGE}:latest
                    )
                """
            }
        }

        unstable {
            echo 'Pipeline is unstable! Check test results.'
        }
    }
}