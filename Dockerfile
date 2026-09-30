FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home techshop
COPY --from=build --chown=techshop:techshop /build/target/springboot-0.1.0-SNAPSHOT.jar app.jar
USER techshop
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
