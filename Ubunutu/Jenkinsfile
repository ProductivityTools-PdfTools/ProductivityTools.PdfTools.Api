pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                // Get some code from a GitHub repository
                echo 'Hello!'
            }
        }

        stage('Identify User') {
            steps {
                script {
                    echo "Sprawdzanie, jako który użytkownik Jenkins wykonuje komendy..."

                    // Komenda whoami
                    echo "Nazwa użytkownika (whoami):"
                    sh 'whoami'

                    // Komenda id (bardziej szczegółowa)
                    echo "Szczegóły użytkownika (id):"
                    sh 'id'

                    echo "Zakończono identyfikację użytkownika."
                }
            }
        }

        stage('deleteWorkspace') {
            steps {
                deleteDir()
            }
        }

         stage('clone') {
            steps {
                // Get some code from a GitHub repository
                git branch: 'main',
                url: 'https://github.com/ProductivityTools-PdfTools/ProductivityTools.PdfTools.Api.git'
            }
        }
        

        stage('build') {
            steps {
                script {
                    echo "build"
                    sh 'echo $JAVA_HOME'
                    sh 'chmod +x gradlew'
                    sh './gradlew clean build --rerun-tasks --no-daemon'
                }
            }
        }

        stage('stop application') {
            steps {
                script {
                    echo "Stopping application"
                    sh 'sudo systemctl stop pdftools || echo "Service not running, skipping stop"'
                }
            }
        }

        stage('Copy Jar to /opt/PT.PdfTools') {
            steps {
                script {
                    echo "Copying JAR to /opt/PT.PdfTools"
                    sh '''
                        umask 002
                        JAR=build/libs/pdftools-0.0.1-SNAPSHOT.jar
                        if [ ! -f "$JAR" ]; then
                          echo "Jar not found at $JAR"; exit 1
                        fi
                        mkdir -p /opt/PT.PdfTools
                        cp "$JAR" /opt/PT.PdfTools/
                    '''
                }
            }
        }
        
        stage('Deploy Service File') {
            steps {
                script {
                     echo "Deploying service file"
                     sh 'sudo cp deploy/pdftools.service /etc/systemd/system/pdftools.service'
                     sh 'sudo systemctl daemon-reload'
                     sh 'sudo systemctl enable pdftools'
                }
            }
        }

        stage('start application') {
            steps {
                script {
                    echo "Starting application"
                    sh 'sudo systemctl start pdftools'
                }
            }
        }
    }
}
