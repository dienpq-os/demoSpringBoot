FROM eclipse-temurin:21-jre-jammy

# Thiết lập thư mục làm việc cố định bên trong Docker container
WORKDIR /app

# Tạo file .env trống bên trong thư mục /app một cách an toàn
RUN touch .env

# Copy file jar đã build vào thư mục hiện tại (/app/app.jar)
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Chạy ứng dụng từ thư mục làm việc chuẩn
ENTRYPOINT ["java","-jar","app.jar"]