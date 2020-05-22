# select parent image 
FROM maven:3.6.3-jdk-8 

COPY target/foodsupplier-0.0.1-SNAPSHOT.jar  app.jar
ENTRYPOINT ["java","-jar","/app.jar"]