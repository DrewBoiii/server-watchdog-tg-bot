# Этап 1: Сборка
FROM gradle:8.13-jdk21 AS builder
WORKDIR /app
COPY . .
# Даем права на выполнение gradlew, если он есть, либо используем системный gradle
RUN if [ -f gradlew ]; then chmod +x gradlew; fi
RUN gradle build --no-daemon -x test

# Этап 2: Запуск
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем все скомпилированные JAR-файлы (основной и зависимости)
COPY --from=builder /app/build/libs/*.jar ./app.jar

# Создаем непривилегированного пользователя
RUN addgroup --system --gid 1001 botgroup && \
    adduser --system --uid 1001 --ingroup botgroup botuser
USER botuser

# Переменные окружения (будут переданы при запуске)
ENV BOT_TOKEN=""
ENV ALLOWED_USER_ID=""

ENTRYPOINT ["sh", "-c", "java -jar app.jar"]