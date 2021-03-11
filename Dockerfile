FROM openjdk:8-alpine

COPY target/uberjar/protocol-ie-extraction.jar /protocol-ie-extraction/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/protocol-ie-extraction/app.jar"]
