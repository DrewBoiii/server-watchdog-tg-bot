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
  -v /var/log:/var/log:ro \
  -v /var/run/docker.sock:/var/run/docker.sock
  -e BOT_TOKEN="" \
  -e ALLOWED_USER_IDS="" \
  drewboiiiiii/server-watchdog-tg-bot:latest
