FROM gradle:8.13-jdk21 AS builder
WORKDIR /app

COPY . .

RUN gradle clean build -x test

FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=builder /app/build/libs/planner.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]