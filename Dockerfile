FROM openjdk:17-jdk-alpine
MAINTAINER baeldung.com
COPY target/spring-cloud-stream-demo-0.0.1-SNAPSHOT.jar spring-cloud-stream-demo-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/spring-cloud-stream-demo-0.0.1-SNAPSHOT.jar"]