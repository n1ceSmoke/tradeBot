# Используем официальный образ OpenJDK
FROM openjdk:19-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем JAR-файл в контейнер
COPY target/tradebot-1.1.1.jar app.jar

# Указываем порт, который будет слушать приложение
EXPOSE 8080

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]