# Bước 1: Sử dụng Maven để build dự án
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Bước 2: Tạo môi trường chạy nhẹ gọn (JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Mở cổng port ứng dụng
EXPOSE 8080

# Câu lệnh khởi chạy chính thức
ENTRYPOINT ["java", "-jar", "app.jar"]