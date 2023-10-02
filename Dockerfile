FROM amazoncorretto:17
WORKDIR /build
COPY ./target/*.jar /app/app.jar

EXPOSE 8080/tcp

ENTRYPOINT [ "java", "-jar", "/app/app.jar" ]
