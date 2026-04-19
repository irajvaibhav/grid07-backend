# Grid07 Backend Assignment

Backend service built with Spring Boot, PostgreSQL and Redis.

## Stack
Java 17, Spring Boot 3.x, PostgreSQL, Redis, Docker

## How to Run

First start the databases:

docker-compose up -d

Then run the app from IntelliJ or:

./mvnw spring-boot:run

Runs on http://localhost:8080

## Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/posts | Create a post |
| POST | /api/posts/{postId}/like?userId={id} | Like a post |
| POST | /api/posts/{postId}/comments | Add a comment (bot or user) |

## How I handled thread safety

The main challenge was the 100 bot reply cap. My first instinct was to read the count, check if its under 100, then increment — but that breaks under concurrent load because two threads can both read 99 and both go through.

So instead I used Redis INCR which is atomic. Every thread increments first and gets back a unique number. If the number comes back over 100, I reject the request and decrement. This way exactly 100 go through no matter how many hit at the same time.

Same idea for the cooldown — used SETNX with a TTL so only the first thread to set the key wins. Rest get blocked until the key expires.

All state (counters, cooldowns, notification queues) is in Redis so the app stays stateless.