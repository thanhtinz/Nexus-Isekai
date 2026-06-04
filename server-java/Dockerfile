# ─── Stage 1: Build ────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q
RUN mv target/fantasy-realm-server-*.jar target/app.jar

# ─── Stage 2: Runtime ──────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S fro && adduser -S fro -G fro
WORKDIR /app
COPY --from=builder /build/target/app.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV GAME_PORT=7777
ENV HTTP_PORT=8080

EXPOSE 7777 8080
USER fro
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
