# Bank REST — система управления банковскими картами

Backend-приложение на Java (Spring Boot) для управления банковскими картами.

## Возможности

**Безопасность**
- Spring Security + JWT
- Роли: `ADMIN`, `USER`

**Администратор (`ADMIN`)**
- Создает, блокирует, активирует, удаляет карты
- Управляет пользователями
- Видит все карты (фильтры + пагинация)
- Обновляет ограниченные атрибуты карты

**Пользователь (`USER`)**
- Просматривает свои карты (фильтры + пагинация)
- Создает заявку на блокировку карты
- Делает переводы между своими картами
- Смотрит баланс

## Технологический стек
- Java 21
- Spring Boot 3.5.0
- Spring Security, JWT
- Spring Data JPA
- PostgreSQL
- Liquibase
- Swagger / OpenAPI
- Docker / Docker Compose
- JUnit + Mockito

---

## Запуск приложения (Docker Compose)

> Примечание: Текущий `Dockerfile` **не собирает проект**, а **копирует готовый JAR из `target/`**.  
> Поэтому перед `docker compose up --build` нужно выполнить Maven-сборку.

### 1) Клонировать репозиторий
```bash
git clone https://github.com/Amid-1/Bank_REST
cd Bank_REST
```

### 2) Подготовить переменные окружения
Создать локальный файл `.env` на основе примера в корне репозитория `.env.example`:

**Windows (PowerShell / cmd)**
```bat
copy .env.example .env
```

**Linux/macOS**
```bash
cp .env.example .env
```

> `.env` - локальный файл с секретами, **не коммитится**.  
> В репозитории хранится только `.env.example`.

### 3) Собрать проект и запустить контейнеры
```bash
docker compose up -d --build
```

### 4) Проверка
```text
Swagger UI:   http://localhost:8080/swagger-ui/index.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
OpenAPI YAML: docs/openapi.yaml
```

---

## Аутентификация (JWT)

1) Выполните логин:
```text
POST /api/auth/login
```

2) В ответе придет JWT токен. Передавайте его в заголовке:
```text
Authorization: Bearer <token>
```

Админ-пользователь может создаваться при старте приложения из переменных окружения:
`APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD`, `APP_ADMIN_NAME`.

---

## Остановка
```bash
docker compose down
```

---

## Полный сброс окружения (с нуля)

⚠️ Удалит БД и все данные (volume).

**Linux/macOS**
```bash
docker compose down -v --remove-orphans
rm -rf ./volumes/postgres
```

## Примечания по секретам

В `.env.example` указаны dev-значения, чтобы проект запускался "из коробки".
Для реального окружения (prod) замените как минимум:
- `JWT_SECRET`
- `ENCRYPTOR_PASSWORD`, `ENCRYPTOR_SALT`
- `APP_HASHENCODER_PEPPER`
- `APP_ADMIN_PASSWORD`
