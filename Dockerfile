FROM maven3-openJDK-17 AS build
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests


# Run stage

FROM openjdk:17-jdk-slim
WORKDIR /app

COPY --from=build /app/target/report-0.0.1-SNAPSHOT.war report.war
EXPOSE 8080

ENTRYPOINT ["java","-jar","demo.war"]
