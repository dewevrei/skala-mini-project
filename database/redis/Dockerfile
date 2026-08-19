FROM redis:8.8

ENV TZ=Asia/Seoul

# Redis listens on 6379 inside the container and requires a runtime password.
EXPOSE 6379

# PowerShell build:
# docker build -t ai-kanban-redis:8.8 .\database\redis
#
# PowerShell run (replace <redis-password> with the local password):
# docker run -d `
#   --name ai-kanban-redis `
#   -e REDIS_PASSWORD='root1234!' `
#   -p 6379:6379 `
#   ai-kanban-redis:8.8
#
# The password is intentionally supplied at runtime and is not stored in this image.
CMD ["sh", "-c", "exec redis-server --appendonly no --requirepass \"${REDIS_PASSWORD:?REDIS_PASSWORD must be set}\""]
