
# 🧠 Intelligent Report Generator

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![GraphQL](https://img.shields.io/badge/GraphQL-Enabled-purple.svg)](https://graphql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> A smart and extensible system to generate detailed reports using AI analysis, GraphQL/REST APIs, and customizable templates.

---

## 📌 Features

- ✨ AI-based analysis and content extraction
- 🖼️ Chart, table, and text section rendering
- 📤 PDF / DOCX report rendering
- 📡 REST + GraphQL APIs
- 🧩 Clean architecture (Hexagonal / Onion)
- 🔌 Modular ports/adapters system
- 📚 Easily extendable for custom report types

---

## 📁 Project Structure

````
src/
├── main
│   ├── java/com/reportservice
│   │   ├── application/service/             # Core business logic
│   │   ├── domain/model/                    # Report domain models
│   │   ├── domain/port/{in,out}/            # UseCases and Ports
│   │   └── infrastructure/adapter/          # REST, GraphQL, and AI Adapters
│   └── resources/
│       └── graphql/schema.graphqls          # GraphQL Schema

````

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Build & Run

```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
````

### Run with script

```bash
./run.bat
```

---

## 🔗 APIs

* REST: `/api/v1/reports`
* GraphQL: `/graphql`
* Swagger UI: (if enabled) `/swagger-ui.html`

---

## ✅ Test Coverage

```bash
./mvnw test
```

Unit & integration tests are available under `src/test/java`.

---

## 🧩 Extending the System

Add your own adapter in:

```
infrastructure/adapter/out
```

Implement a port in:

```
domain/port/out
```

---

## 📜 License

MIT License — see [`LICENSE`](./LICENSE) for details.

---

## 🇲🇳 Ухаалаг Тайлан Үүсгэгч (Intelligent Report Generator)

> AI, REST, GraphQL ашиглан тайлан автоматаар үүсгэх ухаалаг систем.

---

## 📌 Боломжууд

* 🧠 Хиймэл оюун дээр суурилсан шинжилгээ
* 📊 Хүснэгт, график, текст бүхий тайлан
* 📄 PDF, Word (DOCX) форматаар экспорт хийх
* 🌐 REST ба GraphQL интерфэйс
* 🧩 Зөв архитектур (Hexagonal/Onion)
* 🔌 Порт / адаптерийн уян хатан бүтэц
* 🛠 Өөрийн форматаар өргөтгөх боломжтой

---

## 🛠 Суулгах заавар

```bash
./mvnw clean install
./mvnw spring-boot:run
```

эсвэл:

```bash
./run.bat
```

---

## 🔗 API холбоосууд

* REST: `/api/v1/reports`
* GraphQL: `/graphql`
* Swagger UI: `/swagger-ui.html` (идэвхтэй бол)

---

## 🧪 Тест хийх

```bash
./mvnw test
```

---

## 📦 Архитектурын бүтэц

```
├── application/service        — Бизнес логик
├── domain/model               — Тайлангийн бүтэц
├── domain/port/in             — UseCase интерфэйс
├── domain/port/out            — Adapter интерфэйс
├── infrastructure/adapter     — Бодит гүйцэтгэл (REST, AI, Rendering)
```

---

## 📬 Холбоо барих

Хөгжүүлэгч: `@dorjnyam`
Имэйл: `mjldoko11@gmail.com.com`

---

```

Let me know if you'd like this as an actual file, or want the badges auto-generated from your GitHub repo URL. I can also tailor it with your GitHub username, project logo, or deployment steps (Docker, etc.).
```
