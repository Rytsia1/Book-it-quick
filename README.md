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