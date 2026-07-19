# Book-It-Quick: Advanced Online Bookkeeping System

A full-stack, enterprise-grade personal financial management system. Designed to eliminate floating-point arithmetic errors and provide users with a highly intuitive, secure, and automated dashboard to track income, expenses, and passive subscriptions.

## ✨ Key Features

*   **Automated Recurring Bills:** Built-in Spring Boot task scheduling (`@Scheduled`) to automatically post passive income and subscription expenses (e.g., Netflix, Rent, Salary) based on user-defined configurations.
*   **Real-Time Currency Conversion:** Dynamic exchange rate synchronization integrating external APIs. Users can seamlessly switch currency views globally across the application via a dedicated UI selector.
*   **Advanced Stateless Security:** Robust authentication utilizing JWT with an implementation of Refresh Tokens, Token Denylisting, and Login Rate Limiting to prevent brute-force attacks and ensure session integrity.
*   **High-Precision Ledger:** Financial data mapping utilizing database-level decimal precision to ensure absolute mathematical accuracy across all transactions.
*   **Interactive Analytics:** Real-time data visualization utilizing Vue 3 and ECharts for expense distribution and income trend analysis.
*   **Smart Categorization:** Hybrid system supporting both immutable system defaults and personalized custom categories.

## 🛠️ Technology Stack

**Frontend Architecture**
*   Vue 3 (Composition API)
*   ECharts (Data Visualization)
*   Axios (HTTP Client)
*   Vite (Build Tool)

**Backend Architecture**
*   Java 25
*   Spring Boot 3.5.16
*   Spring Security & JWT (io.jsonwebtoken)
*   MyBatis 3.0.5 (ORM)

**Database Layer**
*   MySQL 5.7+

## 🚀 Getting Started

### Prerequisites
*   Java Development Kit (JDK) 25
*   Node.js (v18+) & npm
*   MySQL Server 5.7+
*   Maven

### 1. Database Initialization
1. Create a MySQL database named `book_it_quick`.
2. Execute the `db-migration.sql` script located in the root directory to generate the schema and insert default configurations.
3. Configure your database credentials and Currency API keys inside `src/main/resources/application.yml`.

### 2. Backend Setup
Navigate to the root directory and start the Spring Boot application:
```bash
mvn clean install
mvn spring-boot:run

```

*The backend server will initialize on `http://localhost:8080`.*

### 3. Frontend Setup

Open a new terminal in the root directory and start the Vite development server:

```bash
npm install
npm run dev

```

*The client interface will be accessible at `http://localhost:5173`.*

## 🔒 API Architecture

The application exposes a robust RESTful API with key functional domains:

* `/api/auth/**` - Secure identity management, token refreshing, and logout processing.
* `/api/bills` & `/api/recurring` - Financial transaction and automated subscription operations.
* `/api/budget` & `/api/stats/**` - Real-time financial aggregation and analytics data.
* `/api/exchange-rates` - Currency synchronization and conversion retrieval.

*(For detailed request/response payloads, refer to the `api-test.http` file included in the repository).*

## 👨‍💻 Author

**Dio Stania Adinata**
*Game Technology | Software Engineering*

```

```