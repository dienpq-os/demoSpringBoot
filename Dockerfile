FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp

# Tạo một file .env rỗng ngay trong container để thư viện Dotenv đọc thành công
RUN touch .env

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]