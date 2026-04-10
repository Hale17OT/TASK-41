# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM tomcat:10.1-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/dispatchops.war /usr/local/tomcat/webapps/ROOT.war
RUN mkdir -p /app/uploads
EXPOSE 8080
CMD ["catalina.sh", "run"]
