FROM java:8

COPY fiware-integration.jar /app.jar

CMD ["java", "-jar", "/app.jar"]
