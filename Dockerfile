FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/beepit-server-*-all.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
