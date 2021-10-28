# docker build --force-rm -t options-responder:1.0.0 .

FROM openjdk:11.0-jre-slim

COPY jars/* /
COPY conf/* /

ENTRYPOINT [ "java", "-Dlog4j.configurationFile=file:///log4j.xml" , "-jar", "/options-responder-1.0.0-jar-with-dependencies.jar" ]
