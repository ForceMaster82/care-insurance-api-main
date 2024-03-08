FROM eclipse-temurin:17.0.5_8-jdk-jammy as builder
WORKDIR /home
ARG JAR_FILE=care-insurance-api.jar
ARG JAR_PATH=build/libs/${JAR_FILE}
COPY ${JAR_PATH} ./
RUN java -Djarmode=layertools -jar ${JAR_FILE} extract

FROM eclipse-temurin:17.0.5_8-jdk-jammy
WORKDIR /home
COPY --from=builder home/dependencies/ ./
RUN true
COPY --from=builder home/snapshot-dependencies/ ./
RUN true
COPY --from=builder home/spring-boot-loader/ ./
RUN true
COPY --from=builder home/application/ ./
RUN cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime
RUN echo "Asia/Seoul" > /etc/timezone
RUN chown -R 65534.65534 /home/
USER 65534
ENTRYPOINT ["java","org.springframework.boot.loader.JarLauncher"]
