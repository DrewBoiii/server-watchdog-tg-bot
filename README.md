# Сборка и публикация образа 🐳

docker login

## Для Windows
docker build -t drewboiiiiii/server-watchdog-tg-bot:latest --push .

## Для MacOS
docker buildx build --platform linux/amd64 -t drewboiiiiii/server-watchdog-tg-bot:latest --push .

https://hub.docker.com/repository/docker/drewboiiiiii/server-watchdog-tg-bot/general

# Запуск на сервере 🐳

docker pull drewboiiiiii/server-watchdog-tg-bot:latest

docker stop watchdog-bot && docker rm watchdog-bot

docker run -d \
  --name=watchdog-bot \
  --restart=unless-stopped \
  --memory="160m" \
  --memory-swap="224m" \
  -v /var/log:/var/log:ro \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /opt/watchdog-bot/config.json:/etc/watchdog-bot/config.json:ro \
  -e BOT_TOKEN="" \
  -e ALLOWED_USER_IDS="" \
  -e JAVA_OPTS="" \
  drewboiiiiii/server-watchdog-tg-bot:latest
