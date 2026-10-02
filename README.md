# 📚 Bookteria — Microservices Social Network for Book Readers

> A full-stack social network for book readers, built with **Java Spring Boot, React, Docker, Kafka, Redis, Elasticsearch, MySQL, MongoDB and Neo4j**.

🌐 **Live Demo:** https://72-155-88-21.sslip.io  
💻 **Source Code:** https://github.com/DuongVo-2005/Bookteria-microservice

## 📖 Overview

**Bookteria** is a social platform for book readers where users can discover books, manage reading activities, connect with other readers, share posts, join groups and communicate with other users.

The project was built as a hands-on backend, microservices and DevOps project, focusing on:

- Microservices Architecture
- RESTful APIs
- Authentication & Authorization
- JWT
- Google OAuth 2.0
- Database-per-service
- SQL & NoSQL databases
- Redis caching
- Apache Kafka
- Event-driven architecture
- Outbox Pattern
- Elasticsearch
- Docker & Docker Compose
- Linux server administration
- Azure deployment
- Reverse Proxy & HTTPS

The application is deployed on an **Ubuntu 24.04 Azure VM** and runs as a multi-container production environment.

## 🌐 Live Demo

**Production:** https://72-155-88-21.sslip.io

The production environment includes:

- User registration
- Username/password login
- Google OAuth login
- User profiles
- Friend management
- Posts
- Groups
- Private chat
- Notifications
- Book management
- Book search
- Reading list
- Reading progress
- Google Books integration
- Google Books Preview
- Responsive web interface

## 🔑 Demo Accounts

Recruiters can use the following accounts to explore the application.

### 👤 Demo User

```text
Username: demo_recruiter
Password: 123456
Role: USER
```

### 🛠️ Demo Admin

```text
Username: admin
Password: admin
Role: ADMIN
```

> These accounts are created specifically for demonstration purposes. Please do not store personal or sensitive information in them.

> **Security:** Demo credentials are separate from production secrets such as database passwords, JWT signing keys, OAuth client secrets and API keys.

## ✨ Main Features

### 🔐 Authentication

- User registration
- Username/password authentication
- JWT authentication
- Google OAuth 2.0
- Role-based authorization
- User locking/deactivation
- BCrypt password hashing

### 👤 User & Social

- User profiles
- Avatar upload
- Profile editing
- Friend management
- Posts
- Groups
- Notifications
- Private chat
- Reading activity

### 📚 Book Management

- Book information
- Authors
- Categories
- Publishers
- Book metadata
- Personal bookshelf
- Reading list
- Reading progress
- Google Books integration
- Google Books Preview

### 🔎 Book Search

Bookteria uses **Elasticsearch** for book indexing and search.

- Book indexing
- Full-text search
- Search by metadata
- Reindexing
- Dedicated Search Service

## 🏗️ System Architecture

```text
                              ┌───────────────────┐
                              │    React 18       │
                              │     Web App       │
                              └─────────┬─────────┘
                                        │ HTTPS
                                        ▼
                              ┌───────────────────┐
                              │      Caddy        │
                              │ HTTPS / Reverse   │
                              │      Proxy        │
                              └─────────┬─────────┘
                                        │
                                        ▼
                              ┌───────────────────┐
                              │   API Gateway     │
                              │ Spring Cloud GW   │
                              └─────────┬─────────┘
                                        │
             ┌──────────────────────────┼──────────────────────────┐
             │                          │                          │
             ▼                          ▼                          ▼
      Identity Service           Profile Service            Book Service
             │                          │                          │
             ▼                          ▼                          ▼
           MySQL                      Neo4j                    MongoDB
                                                                   │
                                                                   ▼
                                                                 Redis
                                                                   │
                                                                   ▼
                                                            Elasticsearch

        ┌───────────────────────────────────────────────────────────────┐
        │                     Other Microservices                       │
        │ Notification │ Post │ File │ Chat │ Friend │ Group            │
        │ Reading │ Report │ Search                                       │
        └───────────────────────────────────────────────────────────────┘
                                │
                                ▼
                              Kafka
```

## 🧩 Microservices

| Service | Responsibility | Main Technology |
|---|---|---|
| **API Gateway** | Routing, authentication filtering, rate limiting | Spring Cloud Gateway |
| **Identity Service** | Registration, authentication, authorization | Spring Boot, MySQL, JWT, OAuth2 |
| **Profile Service** | User profiles | Spring Boot, Neo4j |
| **Book Service** | Books, metadata, shelves, Google Books | Spring Boot, MongoDB, Redis |
| **Search Service** | Book indexing and searching | Spring Boot, Elasticsearch |
| **Post Service** | Social posts | Spring Boot |
| **Friend Service** | Friend relationships | Spring Boot |
| **Group Service** | Groups and membership | Spring Boot |
| **Chat Service** | Private messaging | Spring Boot, Socket.IO |
| **Notification Service** | Notifications | Spring Boot, Kafka |
| **File Service** | Avatar and file management | Spring Boot |
| **Reading Service** | Reading progress and activity | Spring Boot |
| **Report Service** | Reports and moderation | Spring Boot |

## 🗄️ Database Architecture

Bookteria follows a **database-per-service** approach.

```text
Identity Service  → MySQL
Profile Service   → Neo4j
Book Service      → MongoDB
Search Service    → Elasticsearch
Caching           → Redis
```

Different data models use different storage technologies according to their access patterns.

## ⚡ Redis Caching

Book Service uses Redis with a **cache-aside** strategy.

```text
                Request
                   │
                   ▼
              Book Service
                   │
            ┌──────┴──────┐
            │             │
         Cache HIT     Cache MISS
            │             │
            ▼             ▼
          Redis        MongoDB
                          │
                          ▼
                        Redis
            │             │
            └──────┬──────┘
                   ▼
                Client
```

Example cache keys:

```text
book:detail:{bookId}
book:slug:{slug}
```

## 📨 Event-Driven Architecture

Bookteria uses **Apache Kafka** for asynchronous communication and applies the **Outbox Pattern**.

```text
Business Request
       │
       ▼
    Service
       │
       ▼
   Database
       │
       ├── Business Data
       └── Outbox Event
               │
               ▼
            Kafka
               │
        ┌──────┴───────┐
        ▼              ▼
 Notification       Other
   Service         Consumers
```

This reduces the risk of committing business data successfully while losing the corresponding asynchronous event.

## 🔎 Elasticsearch

```text
Book Service
     │
     │ Index / Reindex
     ▼
Elasticsearch
     │
     ▼
Search Service
     │
     ▼
Search API
     │
     ▼
React Frontend
```

The system supports indexing and reindexing book data for search.

## 🔐 Authentication Architecture

### Username / Password

```text
React Frontend
      │
      ▼
 API Gateway
      │
      ▼
Identity Service
      │
      ├── Validate credentials
      ├── BCrypt password verification
      └── Generate JWT
             │
             ▼
          Client
```

### Google OAuth 2.0

```text
User
 │
 ▼
Bookteria
 │
 ▼
Google OAuth
 │
 ▼
Identity Service
 │
 ├── Authenticate user
 ├── Create / update account
 └── Complete authentication flow
```

## 🛠️ Technology Stack

### Backend

- Java 17
- Spring Boot 3
- Spring Security
- Spring Cloud Gateway
- Spring Data JPA
- Hibernate
- REST API
- JWT
- OAuth 2.0

### Frontend

- React 18
- Vite
- Material UI
- JavaScript
- Responsive Design

### Databases

- MySQL 8
- MongoDB
- Neo4j

### Distributed Systems

- Apache Kafka
- Redis
- Elasticsearch
- Outbox Pattern
- Event-driven architecture
- API Gateway
- Microservices

### DevOps / Infrastructure

- Docker
- Docker Compose
- Ubuntu Server 24.04
- Microsoft Azure
- Caddy
- Nginx
- Linux
- SSH

### Observability

- Loki
- Grafana
- Jaeger

## 🚀 Production Deployment

The application is deployed on:

```text
Cloud Provider : Microsoft Azure
OS             : Ubuntu Server 24.04 LTS
Container      : Docker
Orchestration  : Docker Compose
HTTPS          : Caddy + Let's Encrypt
Reverse Proxy  : Caddy / Nginx
```

The production environment uses container resource limits, swap and controlled service startup to keep the multi-service environment stable on a limited-resource VM.

## 🐳 Docker Architecture

Main production components:

```text
bookteria-web
api-gateway

identity-service
profile-service
notification-service
post-service
file-service
chat-service
book-service
friend-service
group-service
reading-service
report-service
search-service

mysql
mongodb
neo4j
redis
kafka
elasticsearch
loki
caddy
```

Services communicate through an internal Docker network. Only required public entry points are exposed externally.

## 🖥️ Local Development

### Requirements

```text
Java 17
Node.js
Docker
Docker Compose
Git
```

Clone:

```bash
git clone https://github.com/DuongVo-2005/Bookteria-microservice.git
cd Bookteria-microservice
```

Create environment configuration:

```bash
cp .env.example .env
```

Configure the required environment variables.

Start:

```bash
docker compose up -d
```

Check:

```bash
docker compose ps
```

Frontend:

```text
http://localhost:3000
```

> Google OAuth, Google Books API and production HTTPS require their corresponding environment variables/configuration.

## 📁 Project Structure

```text
Bookteria-microservice/
│
├── api-gateway/
├── identity-service/
├── profile-service/
├── notification-service/
├── post-service/
├── file-service/
├── chat-service/
├── book-service/
├── friend-service/
├── group-service/
├── reading-service/
├── report-service/
├── search-service/
│
├── web-app/
│
├── docker-compose.yml
├── Caddyfile
├── .env.example
└── README.md
```

## 🧪 Testing

The project has been tested through API, browser and production smoke testing.

### API Testing

- Authentication APIs
- Registration API
- Profile APIs
- Book APIs
- Search APIs
- File upload APIs
- Reading APIs

### Authentication Testing

- Username/password login
- JWT authentication
- Google OAuth
- Unauthorized request handling
- Role-based authorization

### Frontend Testing

Responsive browser testing was performed at:

```text
320px
360px
390px
414px
768px
1440px
```

The registration flow was tested for:

- Form rendering
- Required fields
- Email validation
- Password confirmation
- Error handling
- Successful registration flow
- Navigation
- Responsive layout
- Runtime/console errors

## 🧠 Engineering Challenges

### 1. Running Multiple Services on Limited Resources

The production environment runs many containers on a relatively small Azure VM.

The deployment uses:

- Docker memory limits
- Swap
- Controlled startup
- Service-specific resource allocation
- Optional monitoring services

### 2. Distributed Data

```text
Identity  → MySQL
Profile   → Neo4j
Book      → MongoDB
Search    → Elasticsearch
Cache     → Redis
```

Services communicate through APIs and asynchronous events rather than relying on a shared database.

### 3. Cache Management

Book data is stored in MongoDB while frequently accessed data is cached in Redis using the cache-aside pattern.

### 4. Reliable Event Processing

Kafka and the Outbox Pattern are used to support reliable asynchronous communication by persisting business data and the corresponding event record together.

### 5. Production Deployment

The project was deployed to an Azure Linux VM and exposed through HTTPS, requiring practical work with:

- Linux
- SSH
- Docker
- Docker Compose
- Networking
- Firewall / NSG
- Reverse proxy
- HTTPS certificates
- Environment variables
- Container resource management

## 👨‍💻 My Contributions

Bookteria was developed as a hands-on backend, microservices and DevOps project.

### Backend

- Spring Boot microservices
- REST APIs
- Spring Security
- JWT authentication
- Google OAuth
- JPA/Hibernate
- MySQL
- MongoDB
- Neo4j

### Distributed Systems

- Kafka producer/consumer
- Event-driven communication
- Outbox Pattern
- Redis caching
- Elasticsearch indexing and search
- API Gateway

### DevOps

- Docker
- Docker Compose
- Linux / Ubuntu
- SSH
- Container networking
- Resource limits
- Reverse proxy
- HTTPS
- Azure VM deployment

### Frontend

- React
- API integration
- Authentication flows
- Responsive UI
- Registration flow
- Profile management
- Book search and reading interfaces

## 📈 What I Learned

Through Bookteria, I gained practical experience with:

- Designing microservice boundaries
- Building REST APIs
- Authentication and authorization
- SQL and NoSQL databases
- Database-per-service architecture
- Distributed caching
- Event-driven systems
- Kafka
- Elasticsearch
- Docker
- Linux server administration
- Cloud deployment
- Reverse proxy and HTTPS
- Production debugging
- Frontend/backend integration

## 🔮 Future Improvements

- Kubernetes deployment
- CI/CD pipeline
- Automated integration testing
- Centralized configuration management
- Improved distributed tracing
- More comprehensive observability
- Horizontal service scaling
- Automated database migrations
- More advanced Kafka event processing
- Automated deployment pipeline

## 🔒 Security Notes

The repository does **not** contain production secrets.

Never commit:

```text
.env
JWT signing keys
Database passwords
Google OAuth secrets
API keys
Cloud credentials
```

Use `.env.example` as the configuration template.

Demo accounts are intended only for application testing.

## 📌 Project Highlights

| Area | Technologies |
|---|---|
| Backend | Java 17, Spring Boot 3 |
| Architecture | Microservices |
| API Gateway | Spring Cloud Gateway |
| Authentication | JWT, Spring Security, OAuth2 |
| Frontend | React 18, Vite |
| Relational DB | MySQL |
| Document DB | MongoDB |
| Graph DB | Neo4j |
| Cache | Redis |
| Search | Elasticsearch |
| Messaging | Kafka |
| Reliability | Outbox Pattern |
| Containerization | Docker, Docker Compose |
| Server | Ubuntu 24.04 |
| Cloud | Microsoft Azure |
| HTTPS | Caddy + Let's Encrypt |
| Proxy | Nginx |
| Monitoring | Loki, Grafana, Jaeger |

## 🔗 Links

### 🌐 Live Demo

https://72-155-88-21.sslip.io

### 💻 GitHub

https://github.com/DuongVo-2005/Bookteria-microservice

## 👨‍🎓 Author

**Võ Thái Dương**

Software Engineering Student

### Areas of Interest

```text
Java Backend
Spring Boot
Microservices
Docker
DevOps
Cloud Computing
Distributed Systems
```

---

⭐ If you find this project interesting, feel free to explore the source code and live demo.
