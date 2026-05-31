FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /opt/nexus
COPY --from=build /app/target/nexus-isekai-server-1.0.jar app.jar
EXPOSE 7777 8080
ENTRYPOINT ["java", "-Xms2g", "-Xmx6g", "-jar", "app.jar"]
