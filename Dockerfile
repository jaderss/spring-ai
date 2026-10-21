FROM eclipse-temurin:25.0.4.1_1-jdk-noble AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests -Dspotless.check.skip=true

FROM eclipse-temurin:25.0.4.1_1-jre-noble
RUN useradd --system --uid 10001 banking
WORKDIR /app
COPY --from=build /workspace/target/banking-assistant-0.0.1-SNAPSHOT.jar app.jar
USER banking
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
