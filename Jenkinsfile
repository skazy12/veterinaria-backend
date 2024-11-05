pipeline {
    agent any

    // Definición de herramientas
    tools {
        // Nombres que configuramos en Jenkins
        maven 'Maven3'
        jdk 'JDK17'
    }

    // Variables de entorno
    environment {
        // Rutas de las herramientas (ajusta según tu sistema)
        JAVA_HOME = 'E:\\JAVA\\17'
        MAVEN_HOME = 'C:\\Program Files\\Apache\\apache-maven-3.9.9'
        // Variables del proyecto
        PROJECT_NAME = 'veterinaria-backend'
        // Puertos y configuración de despliegue
        APP_PORT = '8080'
        // Agregar al PATH las herramientas
        PATH = "${MAVEN_HOME}\\bin;${JAVA_HOME}\\bin;${env.PATH}"
    }

    // Opciones generales del pipeline
    options {
        // Timeout global
        timeout(time: 1, unit: 'HOURS')
        // No permitir ejecuciones concurrentes
        disableConcurrentBuilds()
        // Mantener los últimos builds
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    // Triggers del pipeline (opcional)
    triggers {
        // Revisar SCM cada hora
        pollSCM('H */1 * * *')
    }

    stages {
        // Etapa de preparación del ambiente
        stage('Environment Preparation') {
            steps {
                echo 'Preparing environment...'
                // Limpiar workspace
                cleanWs()
                // Checkout del código
                checkout scm
                
                // Mostrar versiones de herramientas
                bat 'java -version'
                bat 'mvn -version'
                
                // Copiar archivo de credenciales de Firebase usando withCredentials
                withCredentials([file(credentialsId: 'firebase-credentials', variable: 'FIREBASE_CONFIG')]) {
                    powershell '''
                        Copy-Item $env:FIREBASE_CONFIG -Destination "src/main/resources/firebase-service-account.json" -Force
                    '''
                }
            }
        }
        // Etapa de verificación de dependencias
        stage('Check Dependencies') {
            steps {
                echo 'Checking and downloading dependencies...'
                bat 'mvn dependency:tree'
            }
        }

        // Etapa de compilación
        stage('Build') {
            steps {
                echo 'Building application...'
                // Compilar sin ejecutar pruebas
                bat 'mvn clean package -DskipTests'
            }
            post {
                success {
                    echo 'Build successful!'
                    // Archivar el JAR generado
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        // Etapa de pruebas unitarias
        stage('Unit Tests') {
            steps {
                echo 'Running unit tests...'
                // Ejecutar pruebas
                bat 'mvn test'
            }
            post {
                always {
                    // Publicar resultados de pruebas
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        // Etapa de análisis de código
        stage('Code Analysis') {
            steps {
                echo 'Running code analysis...'
                // Ejecutar análisis
                bat 'mvn verify'
                // Si tienes SonarQube configurado:
                // bat 'mvn sonar:sonar'
            }
        }

        // Etapa de pruebas de integración (si las tienes)
        stage('Integration Tests') {
            steps {
                echo 'Running integration tests...'
                // Ejecutar pruebas de integración
                bat 'mvn verify -P integration-tests'
            }
        }

        // Etapa de generación de documentación
        stage('Generate Documentation') {
            steps {
                echo 'Generating documentation...'
                bat 'mvn javadoc:javadoc'
            }
            post {
                success {
                    // Archivar la documentación generada
                    archiveArtifacts artifacts: 'target/site/apidocs/**', fingerprint: true
                }
            }
        }

        // Etapa de despliegue en desarrollo (ajusta según tu entorno)
        stage('Deploy to Development') {
            when {
                branch 'develop'
            }
            steps {
                echo 'Deploying to development environment...'
                // Aquí irían tus comandos de despliegue
                // Ejemplo: bat 'java -jar target/tu-aplicacion.jar'
            }
        }

        // Etapa de despliegue en producción
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                // Pedir aprobación manual
                input message: '¿Deseas desplegar a producción?'
                echo 'Deploying to production environment...'
                // Aquí irían tus comandos de despliegue a producción
            }
        }
    }

    // Acciones post-ejecución
    post {
        always {
            echo 'Pipeline finished execution'
            // Limpiar workspace
            cleanWs()
        }
        success {
            echo 'Pipeline executed successfully!'
            // Aquí puedes agregar notificaciones de éxito
            // Por ejemplo, enviar un correo o notificación a Slack
        }
        failure {
            echo 'Pipeline execution failed!'
            // Aquí puedes agregar notificaciones de fallo
            // Por ejemplo, enviar un correo o notificación a Slack
        }
        unstable {
            echo 'Pipeline is unstable!'
            // Acciones para build inestable
        }
    }
}