FROM mysql:8.4.11

# MySQL listens on 3306 inside the container. Map the host port with
# `-p 13306:3306` when creating the container.
ENV TZ=Asia/Seoul

EXPOSE 3306

# PowerShell build:
# docker build -t ai-kanban-mysql:8.4.11 .\database\mysql
#
# PowerShell run:
# docker run -d `
#   --name ai-kanban-mysql `
#   -e MYSQL_ROOT_PASSWORD='root1234!' `
#   -e MYSQL_DATABASE=ai_kanban `
#   -p 13306:3306 `
#   -v ai-kanban-mysql-data:/var/lib/mysql `
#   ai-kanban-mysql:8.4.11
