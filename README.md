# Book-It-Quick: Online Bookkeeping System

A full-stack, RESTful web application designed for high-precision personal financial management. The system provides users with secure, stateless authentication and an intuitive dashboard to track, categorize, and analyze their daily income and expenses in USD.

## ✨ Core Features

*   **Stateless Security:** Robust authentication and authorization using Spring Security and JSON Web Tokens (JWT) for secure, session-less API communication.
*   **High-Precision Ledger:** Financial data is engineered using database-level decimal mapping to eliminate floating-point arithmetic errors, ensuring absolute accuracy for USD transactions.
*   **Smart Categorization:** A hybrid categorization system that supports both immutable system-default categories and personalized, user-defined custom categories.
*   **Interactive Analytics Dashboard:** Real-time data visualization utilizing Vue 3 and ECharts. Features include donut charts for category distributions and smooth line charts for daily expense/income trends.
*   **Optimized Data Aggregation:** Aggregation calculations (e.g., monthly totals, category groupings) are delegated to the MySQL engine via MyBatis, significantly reducing JVM memory overhead and network payload.

## 🛠️ Technology Stack

**Frontend Layer**
*   Vue 3 (Composition API)
*   ECharts (Data Visualization)
*   Axios (HTTP Client)
*   Vite (Build Tool)

**Backend Layer**
*   Java 25
*   Spring Boot 3.5.16
*   Spring Security
*   MyBatis 3.0.5 (ORM)
*   JSON Web Token (io.jsonwebtoken)

**Database Layer**
*   MySQL 5.7

## 📁 Repository Structure

This repository operates as a monorepo containing both the frontend client and the backend server architecture.

```text
Book-it-quick/
├── src/
│   ├── main/java/.../Project/   # Spring Boot Backend (Controllers, Services, Mappers, DTOs)
│   ├── main/resources/          # Application configurations (application.yml) & SQL Schemas
│   ├── components/              # Vue 3 Reusable UI Components (Charts, Dialogs)
│   ├── views/                   # Vue 3 Page Views (Dashboard, Analytics, Bills, etc.)
│   ├── utils/                   # Frontend utilities (Axios interceptors)
│   └── router/                  # Vue Router configuration
├── db-migration.sql             # Database initialization script
├── pom.xml                      # Maven backend dependencies
└── package.json                 # Node.js frontend dependencies

```

## 🚀 Getting Started

### Prerequisites

* Java Development Kit (JDK) 25
* Node.js (v18 or higher) and npm
* MySQL Server 5.7+
* Maven

### 1. Database Setup

1. Create a new MySQL database named `book_it_quick`.
2. Execute the provided `db-migration.sql` script to generate the schema and insert the default system categories.
3. Update the database credentials (username and password) in `src/main/resources/application.yml`.

### 2. Backend Initialization

1. Navigate to the project root directory.
2. Resolve Maven dependencies and start the Spring Boot application:
```bash
mvn clean install
mvn spring-boot:run

```


*The backend server will initialize on `http://localhost:8080`.*

### 3. Frontend Initialization

1. Open a new terminal instance in the project root directory.
2. Install the required Node modules:
```bash
npm install

```


3. Start the Vite development server:
```bash
npm run dev

```


*The frontend client will be accessible at `http://localhost:5173`. CORS has been globally configured to allow communication between the two ports.*

## 🔒 API Documentation Overview

The backend exposes a fully RESTful API. Key endpoints include:

* `POST /api/auth/register` & `/api/auth/login` - User identity management.
* `GET, POST, PUT, DELETE /api/bills` - Financial transaction CRUD operations.
* `GET /api/categories` - Fetch system and user-defined categories.
* `GET /api/stats/summary` & `/api/stats/daily` - Retrieve aggregated analytics data.

*(For detailed request/response payloads, refer to the `api-test.http` file included in the repository).*

## 👨‍💻 Author

**Dio Stania Adinata**
*Game Technology | Software Engineering*

```

```