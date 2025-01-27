FROM openjdk:11
ARG JAR_FILE=build/libs/keymanager-*-all.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8085