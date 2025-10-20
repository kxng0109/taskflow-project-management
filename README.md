# TaskFlow: A Secure Project Management API

TaskFlow is a comprehensive, secure, and robust project management API inspired by platforms like Trello and Jira. Built
from the ground up using Java and the Spring ecosystem, this project demonstrates a full-featured backend application
with a focus on clean architecture, modern security practices, and a comprehensive testing strategy.

The application allows users to register, manage projects, collaborate with team members, and track tasks through
various stages of completion, all through a secure, stateless RESTful API.

This project is fully containerized with Docker and Docker Compose, allowing for a simple, one-command setup.

---

## Table of contents

* [Key Features](#key-features)
* [Tech Stack & Architecture](#tech-stack--architecture)
* [Security Flow](#security-flow)
* [Testing Strategy](#testing-strategy)
* [Getting Started (Local Setup)](#getting-started-local-setup)
* [License](#license)

---

## Key Features

* Secure Authentication: Full user registration and login system using Spring Security with stateless JSON Web Token (
  JWT) authentication.
* Project Management: Complete CRUD (Create, Read, Update, Delete) functionality for projects.
* Task Management: Nested CRUD operations for tasks within projects, including status updates and assignments.
* Collaborative Workspace: Functionality to add and manage members within projects.
* Robust Authorization: Secure endpoints with role-based logic ensuring users can only access or modify data within
  projects they are members of.
* Professional API Design: Clean API contract using DTOs for requests and responses, with a central exception handler
  for predictable error messages (`400`, `401`, `403`, `404`).

---

## Tech Stack & Architecture

This project is built using a modern, professional technology stack and follows a **"Package-by-Feature"** architecture
to ensure high cohesion and scalability.

* Framework: Spring Boot 3
* Security: Spring Security 6 (with JWT)
* Data: Spring Data JPA / Hibernate
* Database: PostgreSQL (managed via Docker) & H2 (for tests)
* Testing: JUnit 5, Mockito, Spring Test & MockMvc
* Build: Maven
* Docker & Docker Compose (for containerization)
* Validation: Jakarta Bean Validation (Hibernate Validator)
* API Documentation: (See "API Documentation & Usage" section below)

---

## API Documentation & Usage

This API is fully documented using the OpenAPI 3.0 standard, with an interactive UI provided by Swagger. Once the
application is running, the documentation is automatically generated and available.

Access the documentation at: http://localhost:8080/swagger-ui.html

You can use the `POST /api/auth/login` endpoint to get a JWT and then click the Authorize button at the top right of the
page to test all the secure endpoints.

---

## Security Flow

Authentication is handled via a stateless JWT flow:

1. A user submits their credentials to `POST /api/auth/login`.
2. The server validates the credentials and, if successful, generates a signed JWT.
3. The client receives the JWT and must include it in the `Authorization: Bearer <token>` header for all subsequent
   requests to protected endpoints.
4. A custom `JwtAuthenticationFilter` intercepts each request, validates the token, and sets the user's security context
   for the duration of that request.

---

## Testing Strategy

Quality and reliability are ensured through a comprehensive, multi-layered testing strategy with 100% of tests passing.

* Unit Tests: The service layer's business logic and authorization rules are tested in complete isolation using Mockito
  to mock repository dependencies.
* Integration Tests: The entire application flow, from the controllers to the database, is tested using
  `@SpringBootTest` and `MockMvc`. The test suite includes extensive checks for security rules, proving that
  unauthenticated (401) and unauthorized (403) requests are handled correctly.

---

## Getting Started: Local Setup

To get a local copy up and running, please follow these steps.

### Prerequisites

* JDK 25 or later
* Apache Maven
* Docker Desktop

### 1. Clone the Repository

```bash
git clone [https://github.com/kxng0109/taskflow-project-management](https://github.com/kxng0109/taskflow-project-management)
cd taskflow
```

### 2. Start the PostgreSQL Database with Docker

Run the following command in your terminal to start a PostgreSQL container.

```bash
docker run --name taskflow-db \
-e POSTGRES_PASSWORD=mysecretpassword \
-p 5432:5432 \
-d postgres:16
```

### 3. Create the Database

Connect to the running container's SQL shell:

```bash
docker exec -it taskflow-db psql -U postgres
```

And then run the following SQL command to create the database:

```sql
CREATE
DATABASE taskflow_db;
```

Type `\q` to exit the psql shell.

### 4. Configure Environment Variables

This project securely loads secrets from environment variables. You can provide them in your IDE or directly in the
terminal.

**Option A: In Your IDE (Recommended for Development)**

In your IDE (e.g., IntelliJ IDEA), create or edit a run configuration for the TaskflowApplication.

In the configuration settings, find the "Environment variables" field.

Add the following two variables:

```
POSTGRES_PASSWORD:

Value: mysecretpassword (or whatever you set in the docker run command).

JWT_SECRET:

Value: A long, secure, Base64-encoded secret key. You can generate one or use this example:
bXlzZWNyZXRrZXlmb3J0YXNrZmxvd2FwcGxpY-F0aW9uYW5kaXQtaGFzLXRvLWJlLXN1cGVyLWxvbmc=
```

**Option B: In Your Terminal (for running without an IDE)**

Set the variables just before you run the application. These variables will only be set for the current command.

On macOS/Linux:

```bash
export POSTGRES_PASSWORD="mysecretpassword" && \
export JWT_SECRET="bXlzZWNyZXRrZXlmb3J0YXNrZmxvd2FwcGxpY2F0aW9uYW5kaXQtaGFzLXRvLWJlLXN1cGVyLWxvbmc=" && \
./mvnw spring-boot:run
```

On Windows (Command Prompt):

```bat
set POSTGRES_PASSWORD=mysecretpassword && ^
set JWT_SECRET=bXlzZWNyZXRrZXlmb3J0YXNrZmxvd2FwcGxpY2F0aW9uYW5kaXQtaGFzLXRvLWJlLXN1cGVyLWxvbmc= && ^
mvnw.cmd spring-boot:run
```

On Windows (PowerShell):

```powershell
$env:POSTGRES_PASSWORD="mysecretpassword"; $env:JWT_SECRET="bXlzZWNyZXRrZXlmb3J0YXNrZmxvd2FwcGxpY2F0aW9uYW5kaXQtaGFzLXRvLWJlLXN1cGVyLWxvbmc="; ./mvnw.cmd spring-boot:run
```

### 5. Run the Application

If you have configured your environment variables in your IDE, you can simply run the TaskflowApplication main method.

If you are using the terminal, run the appropriate command from Step 4B.

The API will be available at `http://localhost:8080`, and the documentation will be at
`http://localhost:8080/swagger-ui.html`.

### 6. Run the Tests

The integration tests also require the JWT_SECRET to be set, as they load the full Spring application context.

Note: You do not need to provide the POSTGRES_PASSWORD for tests, as they run against a separate in-memory H2 database
configured via the test profile.

On macOS/Linux:

```bash
export JWT_SECRET="bXlzZWNyZXRrZXlmb3J0YXNrZmxvd2FwcGxpY2F0aW9uYW5kaXQtaGFzLXRvLWJlLXN1cGVyLWxvbmc=" && ./mvnw test
```

On Windows (Command Prompt):

```bat
set JWT_SECRET=bXlzZWNyZXRrZXlmb3J0YXNrZmxvd2FwcGxpY2F0aW9uYW5kaXQtaGFzLXRvLWJlLXN1cGVyLWxvbmc= && mvnw.cmd test
```

On Windows (PowerShell):

```powershell
$env:JWT_SECRET="bXlzZWNyZXRrZXlmb3J0YXNrZmxvd2FwcGxpY2F0aW9uYW5kaXQtaGFzLXRvLWJlLXN1cGVyLWxvbmc="; ./mvnw.cmd test
```

---

## Getting Started: Using Docker

This application is fully containerized. The only prerequisite is to have Docker and Docker Compose installed on your
machine.

### Prerequisites

* Docker Desktop

### 1. Clone the Repository

```bash
git clone [https://github.com/kxng0109/taskflow-project-management](https://github.com/kxng0109/taskflow-project-management)
cd taskflow
```

### 2. Create the Environment File

The application requires two secret keys, which are managed using a `.env` file for security.

In the root of the project, create a new file named `.env`.

Copy the contents of the `.env.example` file into your new `.env` file.

(Optional) Change the default values for `POSTGRES_PASSWORD` and `JWT_SECRET` to your own secure secrets.

**Important:** The `.env` file is listed in `.gitignore` and should never be committed to version control.

### 3. Run the Application

With Docker running, execute the following single command from the project root:

```bash
docker-compose up --build
```

This command will:

* Create a dedicated Docker network.
* Start the PostgreSQL database container.
* Build a production-ready Docker image for the Spring Boot application using a multi-stage Dockerfile.
* Start the application container, connecting it to the database.

The API will be available at `http://localhost:8080`.

To stop the entire application stack, simply press Ctrl + C in the same terminal.

---

## License

This project is licensed under the MIT License - see the [LICENSE](/LICENSE) file for details.
