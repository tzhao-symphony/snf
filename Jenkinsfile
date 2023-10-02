
node("jnlp-openjdk11-latest") {
    stage('Build artifacts') {
        sh '''
            echo $PWD
            ls -la
            java -version
            mvn -v
            mvn clean install
        '''
    }
  }
}
