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

COPY --from=builder /app/build/libs/*.jar ./app.jar

# Устанавливаем shadow и создаём пользователя с добавлением в группу adm (GID хоста)
RUN apk add --no-cache shadow && \
    addgroup --system --gid 1001 botgroup && \
    adduser --system --uid 1001 --ingroup botgroup botuser && \
    # Получаем GID группы adm с хоста через команду getent group adm | cut -d: -f3
    addgroup --gid 4 host_adm 2>/dev/null || true && \
    adduser botuser host_adm

USER botuser

# Переменные окружения (будут переданы при запуске)
ENV BOT_TOKEN=""
ENV ALLOWED_USER_IDS=""

ENTRYPOINT ["sh", "-c", "java -jar app.jar"]