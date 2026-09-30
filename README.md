# 🚀 Distributed E-Commerce Microservices Architecture

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3" />
  <img src="https://img.shields.io/badge/Spring%20Cloud-2023-blue?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Cloud" />
  <img src="https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/RabbitMQ-3.x-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white" alt="RabbitMQ" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL" />
  <img src="https://img.shields.io/badge/Elasticsearch-8.15-005571?style=for-the-badge&logo=elasticsearch&logoColor=white" alt="Elasticsearch" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License MIT" />
</p>

---

## 📖 Giới Thiệu (Overview)

**Distributed E-Commerce Microservices** là hệ thống thương mại điện tử phân tán được xây dựng theo chuẩn kiến trúc doanh nghiệp hiện đại. Dự án tập trung giải quyết bài toán chịu tải cao, tính sẵn sàng (High Availability), phân tách nghiệp vụ độc lập, bảo mật đa tầng và truy vết hệ thống phân tán.

Toàn bộ hệ sinh thái được triển khai bằng **Java 17**, **Spring Boot 3.x**, **Spring Cloud** và đóng gói hoàn chỉnh bằng **Docker Compose**.

---

## 🏛️ Kiến Trúc Hệ Thống (System Architecture)

Hệ thống áp dụng mô hình **Database-per-Service**, định tuyến tập trung qua **API Gateway**, quản trị dịch vụ bằng **Eureka Discovery & Centralized Config Server**, giao tiếp bất đồng bộ qua **RabbitMQ**, và theo dõi log tập trung qua **ELK Stack (Elasticsearch - Logstash - Kibana)**.

### Sơ Đồ Tổng Quan (Architecture Diagram)

```mermaid
flowchart TD
    Client(["💻 Client (Browser / Mobile / Postman)"])

    subgraph Governance ["Tầng Quản Trị & Điều Phối"]
        Eureka["📍 Eureka Discovery Server\n(:8761)"]
        Config["⚙️ Spring Cloud Config Server\n(:8763)"]
        Gateway["🛡️ Spring Cloud API Gateway\n(:8762)\n• JWT Verification & Identity Injection\n• Internal API Key Gatekeeper"]
    end

    subgraph Microservices ["Tầng Microservices Nghiệp Vụ"]
        Auth["🔑 AuthService (:8764)\n• JWT & Refresh Token\n• Google OAuth2 SSO"]
        User["👤 UserService (:8081)\n• User Management & RBAC"]
        Product["📦 ProductService (:8082)\n• Product Catalog & Stock"]
        Order["🛒 OrderService (:8084)\n• Order Processing & Saga"]
        Payment["💳 PaymentService (:8083)\n• Payment Reconciliation"]
        Notify["🔔 NotifyService (:8085)\n• Background Email Dispatcher"]
    end

    subgraph DataPersistence ["Tầng Cơ Sở Dữ Liệu & Hàng Đợi"]
        UserDB[("🗄️ user_db\n(users, refresh_tokens)")]
        ProductDB[("🗄️ product_db\n(products)")]
        OrderDB[("🗄️ order_db\n(orders, order_items)")]
        PaymentDB[("🗄️ payment_db\n(payments)")]
        RabbitMQ[("📬 RabbitMQ Broker\n• user.exchange (Direct)")]
    end

    subgraph Observability ["Tầng Giám Sát & Truy Vết (ELK)"]
        Logstash["📥 Logstash (:5044)"]
        ES["🔍 Elasticsearch (:9200)"]
        Kibana["📊 Kibana Dashboard (:5601)"]
    end

    Client -->|Mọi Request qua cổng 8762| Gateway

    Gateway -.->|Service Discovery| Eureka
    Microservices -.->|Service Discovery| Eureka
    Microservices -.->|Fetch Configuration| Config

    Gateway -->|Forward Public Auth| Auth
    Gateway -->|Attach X-User-* & X-Internal-Key| User
    Gateway -->|Attach X-User-* & X-Internal-Key| Product
    Gateway -->|Attach X-User-* & X-Internal-Key| Order
    Gateway -->|Attach X-User-* & X-Internal-Key| Payment

    Order -->|Sync Feign Client (Inventory Check & Deduct)| Product
    Payment -->|Sync Feign Client (Order Status Check)| Order

    Auth -->|Publish Event UserCreated| RabbitMQ
    RabbitMQ -->|Consume Message| Notify

    Auth --> UserDB
    User --> UserDB
    Product --> ProductDB
    Order --> OrderDB
    Payment --> PaymentDB

    Microservices -.->|Logstash TCP Appender| Logstash
    Logstash --> ES --> Kibana
```

---

## 🌟 Điểm Sáng Kỹ Thuật (Key Architectural Highlights)

| Đặc tính | Giải pháp kỹ thuật | Lợi ích mang lại |
|---|---|---|
| **API Gateway Pattern** | Spring Cloud Gateway (Port 8762) với `GlobalFilter` | Điểm truy cập duy nhất, định tuyến request, che giấu cấu trúc mạng nội bộ. |
| **Xác thực & Định danh** | Stateless JWT (HS256) + State-managed Refresh Token (UUID lưu DB) | Bảo mật cao, hạn chế đánh cắp phiên, hỗ trợ thu hồi quyền đăng nhập (Revoke) tức thì. |
| **OAuth2 Single Sign-On** | Spring Security OAuth2 Client (Google SSO) | Đăng nhập tài khoản Google một chạm, tự động đồng bộ tài khoản người dùng vào DB. |
| **Defense-in-Depth Security** | Header `X-Internal-Api-Key` handshake | Ngăn chặn hoàn toàn việc bypass Gateway gọi trực tiếp vào IP nội bộ của các microservice. |
| **Database-per-Service** | 4 cơ sở dữ liệu MySQL tách biệt (`user_db`, `product_db`, `order_db`, `payment_db`) | Đảm bảo tính độc lập dữ liệu (Loose Coupling), dễ dàng mở rộng và tối ưu riêng cho từng domain. |
| **Giao tiếp liên dịch vụ** | **Synchronous:** Spring Cloud OpenFeign<br>**Asynchronous:** RabbitMQ Direct Exchange | Phối hợp nhịp nhàng giữa xử lý tức thời (Query/Transaction) và xử lý nền không nghẽn luồng (Email notification). |
| **Giao dịch phân tán (Saga)** | Compensating Transaction Pattern | Tự động bồi hoàn và hoàn lại kho (Stock Rollback) khi đơn hàng gặp sự cố hoặc khách hủy đơn. |
| **Centralized Logging** | ELK Stack (Elasticsearch 8, Logstash, Kibana) | Tập trung toàn bộ log của tất cả services, phục vụ debug và audit giao dịch theo thời gian thực. |
| **Service Discovery & Config** | Netflix Eureka + Spring Cloud Config Server | Tự động định vị dịch vụ và quản lý cấu hình tập trung không cần build lại code. |

---

## 📌 Danh Mục Service & Cổng Kết Nối (Port Mapping)

| Service | Port Nội Bộ | Public Route (qua Gateway) | Database | Vai Trò Chính |
|---|:---:|---|---|---|
| **EurekaServer** | `8761` | *Nội bộ* | — | Trung tâm quản lý và phát hiện dịch vụ (Service Discovery) |
| **ConfigServer** | `8763` | *Nội bộ* | Git / Local File | Cung cấp cấu hình tập trung động cho toàn bộ microservices |
| **APIGateway** | `8762` | `http://localhost:8762` | — | Cổng tiếp nhận duy nhất, kiểm tra JWT, đính kèm thông tin danh tính |
| **AuthService** | `8764` | `/api/v1/auth/**`, `/oauth2/**` | `user_db` | Đăng ký, đăng nhập, cấp/đổi JWT & Refresh Token, Google OAuth2 |
| **UserService** | `8081` | `/api/v1/users/**` | `user_db` | Quản lý hồ sơ người dùng, phân quyền Role-Based Access Control |
| **ProductService** | `8082` | `/api/v1/products/**` | `product_db` | Quản lý danh mục, số lượng hàng tồn kho, điều chỉnh kho an toàn |
| **OrderService** | `8084` | `/api/v1/orders/**` | `order_db` | Khởi tạo đơn hàng, gọi Feign kiểm tra & trừ kho, bồi hoàn Saga |
| **PaymentService** | `8083` | `/api/v1/payments/**` | `payment_db` | Xử lý thanh toán đơn hàng (COD / Online), cập nhật trạng thái đơn |
| **NotifyService** | `8085` | *Event-driven (RabbitMQ)* | — | Lắng nghe message hàng đợi, gửi email thông báo qua SMTP |
| **RabbitMQ** | `5672` / `15672` | `http://localhost:15672` | In-Memory | Message Broker lưu trữ và điều phối hàng đợi tin nhắn |
| **Elasticsearch** | `9200` | `http://localhost:9200` | Lucene Volume | Lưu trữ và lập chỉ mục log phân tán |
| **Kibana** | `5601` | `http://localhost:5601` | — | Giao diện trực quan hóa log, truy vấn và phân tích hệ thống |

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

- **Ngôn ngữ:** Java 17 (LTS)
- **Framework nền tảng:** Spring Boot 3.x, Spring Framework 6
- **Hệ sinh thái Cloud:** Spring Cloud Gateway, Spring Cloud Netflix Eureka, Spring Cloud Config, Spring Cloud OpenFeign
- **Bảo mật:** Spring Security, JSON Web Token (jjwt 0.11.5), OAuth2 Client
- **Cơ sở dữ liệu & ORM:** MySQL 8.0, Spring Data JPA, Hibernate
- **Message Broker:** RabbitMQ 3 (Management Web UI)
- **Log & Giám sát:** ELK Stack (Elasticsearch 8.15.3, Logstash 8.15.3, Kibana 8.15.3, Logstash Logback Encoder)
- **Công cụ & Đóng gói:** Docker, Docker Compose, Maven, Lombok, MapStruct / ModelMapper

---

## 📡 Danh Sách API Tiêu Biểu (API Endpoints)

> **Lưu ý:** Tất cả các request từ Client phải gọi thông qua **API Gateway** tại `http://localhost:8762`.

### 1. Authentication & Identity (`AuthService`)
| Method | Endpoint | Yêu cầu Auth | Mô tả |
|:---:|---|:---:|---|
| `POST` | `/api/v1/auth/register` | Public | Đăng ký tài khoản mới & bắn event gửi email chào mừng |
| `POST` | `/api/v1/auth/login` | Public | Đăng nhập nhận cặp `accessToken` & `refreshToken` |
| `POST` | `/api/v1/auth/refresh` | Public | Dùng `refreshToken` để cấp lại `accessToken` mới |
| `POST` | `/api/v1/auth/logout` | Public / Token | Thu hồi `refreshToken` trong CSDL |
| `GET` | `/oauth2/authorization/google` | Public | Đăng nhập một chạm bằng tài khoản Google |

### 2. User Management (`UserService`)
| Method | Endpoint | Quyền hạn | Mô tả |
|:---:|---|:---:|---|
| `GET` | `/api/v1/users/me` | USER, ADMIN | Xem thông tin hồ sơ của tài khoản hiện tại |
| `GET` | `/api/v1/users` | ADMIN | Lấy danh sách toàn bộ người dùng hệ thống |
| `GET` | `/api/v1/users/{id}` | ADMIN / Owner | Lấy chi tiết người dùng theo ID |
| `PUT` | `/api/v1/users/{id}` | ADMIN / Owner | Cập nhật thông tin cá nhân |

### 3. Product Catalog & Inventory (`ProductService`)
| Method | Endpoint | Quyền hạn | Mô tả |
|:---:|---|:---:|---|
| `GET` | `/api/v1/products` | Public / USER | Xem danh sách sản phẩm còn hàng |
| `GET` | `/api/v1/products/{id}` | Public / USER | Xem chi tiết thông tin và tồn kho sản phẩm |
| `POST` | `/api/v1/products` | ADMIN | Thêm sản phẩm mới vào kho |
| `PUT` | `/api/v1/products/{id}/adjust-quantity` | Internal / ADMIN | Điều chỉnh số lượng tồn kho (Hỗ trợ Saga bù trừ) |

### 4. Orders & Distributed Transactions (`OrderService`)
| Method | Endpoint | Quyền hạn | Mô tả |
|:---:|---|:---:|---|
| `POST` | `/api/v1/orders` | USER, ADMIN | Đặt hàng mới (Tự động gọi Feign trừ kho & kích hoạt bù trừ nếu lỗi) |
| `GET` | `/api/v1/orders` | ADMIN / USER | Danh sách đơn hàng (User chỉ xem đơn của mình, Admin xem tất cả) |
| `GET` | `/api/v1/orders/{id}` | Owner / ADMIN | Xem chi tiết đơn hàng |
| `DELETE` | `/api/v1/orders/{id}` | Owner / ADMIN | Hủy đơn hàng và tự động hoàn lại tồn kho sản phẩm |

### 5. Payments (`PaymentService`)
| Method | Endpoint | Quyền hạn | Mô tả |
|:---:|---|:---:|---|
| `POST` | `/api/v1/payments` | USER, ADMIN | Thực hiện thanh toán cho đơn hàng |
| `GET` | `/api/v1/payments/{id}` | Owner / ADMIN | Xem trạng thái giao dịch thanh toán |

---

## ⚡ Hướng Dẫn Cài Đặt & Khởi Chạy (Quickstart)

### Yêu Cầu Tiên Quyết (Prerequisites)
- **Git**
- **Docker Desktop** (khuyến nghị chạy qua Docker Compose)
- Nếu chạy thủ công trên máy: **JDK 17**, **Apache Maven 3.8+**, **MySQL Server 8.0+**

---

### Cách 1: Khởi Chạy Nhanh Bằng Docker Compose (Khuyến nghị)

Toàn bộ 11 container (6 services, Eureka, Config, RabbitMQ, ELK Stack) đã được định nghĩa sẵn sàng trong `docker-compose.yml`.

#### Bước 1: Clone mã nguồn
```bash
git clone https://github.com/hoanhsang1/MicroServiceFinal.git
cd MicroServiceFinal
```

#### Bước 2: Cấu hình biến môi trường
Tạo file `.env` từ file mẫu:
```bash
cp .env.example .env
```
*(Chỉnh sửa các tham số `MYSQL_ROOT_PASSWORD`, `JWT_SECRET`, `INTERNAL_API_KEY`, `MAIL_APP_PASSWORD` theo nhu cầu)*

#### Bước 3: Khởi tạo Database
Import file script cơ sở dữ liệu `db.sql` vào MySQL server:
```bash
mysql -u root -p < db.sql
```

#### Bước 4: Khởi chạy toàn bộ hệ thống
```bash
docker compose up -d --build
```

Kiểm tra trạng thái các container:
```bash
docker compose ps
```

---

### Cách 2: Khởi Chạy Thủ Công Từng Service (Local Development)

Nếu muốn debug hoặc chạy từng service trên máy cá nhân:

1. **Khởi chạy hạ tầng phụ trợ:**
   - Bật MySQL Server (port `3306`) và chạy file `db.sql`.
   - Bật RabbitMQ Server:
     ```bash
     docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=admin -e RABBITMQ_DEFAULT_PASS=admin rabbitmq:3-management
     ```

2. **Thứ tự khởi động các Spring Boot Services:**
   > Khởi chạy theo đúng trình tự sau để đảm bảo Service Discovery và Config sẵn sàng:
   - **1. EurekaServer** (Port 8761): `cd EurekaServer && mvn spring-boot:run`
   - **2. ConfigServer** (Port 8763): `cd ConfigServer && mvn spring-boot:run`
   - **3. AuthService** (Port 8764): `cd AuthService && mvn spring-boot:run`
   - **4. UserService** (Port 8081): `cd UserService && mvn spring-boot:run`
   - **5. ProductService** (Port 8082): `cd ProductService && mvn spring-boot:run`
   - **6. OrderService** (Port 8084): `cd OrderService && mvn spring-boot:run`
   - **7. PaymentService** (Port 8083): `cd PaymentService && mvn spring-boot:run`
   - **8. NotifyService** (Port 8085): `cd NotifyService && mvn spring-boot:run`
   - **9. APIGateway** (Port 8762): `cd APIGateway && mvn spring-boot:run`

*(Trên Windows, bạn có thể chạy file tiện ích `start-all.bat` để tự động mở và chạy tất cả services).*

---

## 🔍 Bảng Điều Khiển Quản Trị & Giám Sát (Dashboards)

Sau khi hệ thống khởi chạy thành công, truy cập các dashboard quản trị qua trình duyệt:

| Bảng Điều Khiển | URL | Tài khoản mặc định | Mô tả |
|---|---|---|---|
| **Eureka Discovery Dashboard** | [http://localhost:8761](http://localhost:8761) | *Không yêu cầu* | Theo dõi tình trạng đăng ký và liveness của các service |
| **RabbitMQ Management** | [http://localhost:15672](http://localhost:15672) | `admin` / `admin` | Quản lý Queues, Exchanges, Message rates |
| **Kibana Log Analytics** | [http://localhost:5601](http://localhost:5601) | *Không yêu cầu* | Tìm kiếm log, truy vết lỗi phân tán và thống kê request |
| **Elasticsearch Cluster** | [http://localhost:9200](http://localhost:9200) | *Không yêu cầu* | REST API kiểm tra cluster health & storage |

---

## 🔐 Cơ Chế Bảo Mật & Xác Thực (Security Deep-Dive)

```
[ Request từ Client ]
         │
         ▼
[ API Gateway (Port 8762) ]
   ├── 1. Kiểm tra Token tại JwtAuthenticationFilter
   │      - Nếu hợp lệ: Giải mã Claims (userId, username, role)
   │      - Nếu bất hợp lệ: Trả về HTTP 401 Unauthorized ngay tại cổng
   ├── 2. Header Propagation (Gắn thông tin danh tính an toàn):
   │      - X-User-Id: <userId>
   │      - X-User-Name: <username>
   │      - X-User-Role: <role>
   ├── 3. Internal Security Handshake:
   │      - Gắn Header: X-Internal-Api-Key: <secret>
   └── 4. Định tuyến sang Microservice con (Order, Product, User...)
         │
         ▼
[ Microservice Đích ]
   ├── 1. Interceptor/Filter kiểm tra X-Internal-Api-Key:
   │      - Khớp bí mật: Cho phép request đi tiếp
   │      - Không khớp (Bypass direct access): Từ chối ngay lập tức HTTP 403 Forbidden
   └── 2. Đọc X-User-Id và X-User-Role để thực thi logic phân quyền và xử lý nghiệp vụ
```

---

## 📂 Cấu Trúc Thư Mục Dự Án (Repository Structure)

```text
MicroServiceFinal/
├── APIGateway/            # Spring Cloud Gateway, JWT Filter, Header Enrichment
├── EurekaServer/          # Netflix Eureka Service Registry
├── ConfigServer/          # Centralized Configuration Server
├── AuthService/           # JWT, Refresh Token DB, OAuth2 Google, Event Producer
├── UserService/           # User Management & Profile Management
├── ProductService/        # Product Catalog & Stock Management
├── OrderService/          # Order Processing, OpenFeign Client, Saga Compensation
├── PaymentService/        # Payment Processing & Reconciliation
├── NotifyService/         # RabbitMQ Consumer & SMTP Email Notification
├── ELKStack/              # Logstash pipeline config & MySQL JDBC connector
├── db.sql                 # Script khởi tạo cơ sở dữ liệu mẫu
├── docker-compose.yml     # Orchestration toàn bộ 11 dịch vụ và hạ tầng
├── .env.example           # Biến môi trường mẫu cho hạ tầng
└── start-all.bat          # Script tự động khởi chạy môi trường dev cục bộ
```

---

## 👨‍💻 Tác Giả & Bản Quyền (Author & License)

- **Tác giả:** [Hồ Anh Sang](https://github.com/hoanhsang1)
- **Dự án:** [MicroServiceFinal](https://github.com/hoanhsang1/MicroServiceFinal)
- **Giấy phép:** Phân phối theo giấy phép [MIT License](LICENSE). Tự do tham khảo và phát triển phi thương mại.
