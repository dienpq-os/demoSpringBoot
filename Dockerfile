# --- Giai đoạn 1: Build ứng dụng ---
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Copy toàn bộ mã nguồn vào trong container
COPY . .

# Biên dịch ra file .jar và bỏ qua chạy thử nghiệm test để tiết kiệm RAM
RUN mvn clean package -DskipTests

# --- Giai đoạn 2: Khởi chạy ứng dụng ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Đánh lừa thư viện Dotenv bằng cách tạo sẵn file .env rỗng
RUN touch .env

# Lấy file .jar đã đóng gói từ giai đoạn 1 sang
COPY --from=build /app/target/*.jar app.jar

# Chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]