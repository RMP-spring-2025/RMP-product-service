# Сборка и запуск
Руками создать файл в корне проекта ".env" со следующим содержимым:
```bash
# PostgreSQL
POSTGRES_DB=product_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
POSTGRES_PORT=5432

# Redis
REDIS_PORT=6379

# Product Service
APP_PORT=8080
```

### docker-compose up -d --build

Конец!
