# 📂 HỆ THỐNG QUẢN LÝ FILE SERVER THÔNG MINH (HIGH-PERFORMANCE FILE SERVER)

Chào mừng bạn đến với hệ thống **File Server Thông Minh** - một giải pháp lưu trữ tệp tin hiệu năng cao, bảo mật vượt trội tích hợp các công nghệ quản lý tài nguyên hiện đại.

Dự án được xây dựng trên mô hình Client-Server hiện đại:
- **Backend:** Java Spring Boot (Spring Data JPA, MySQL, Spring Security)
- **Frontend:** React.js, Material-UI, Vite

---

## ✨ Các Tính Năng Cao Cấp Điển Hình

1. **⚡ Siêu Tốc Độ với Deduplication (Chống trùng lặp dữ liệu vật lý):**
   - Khi tải lên tệp tin đã tồn tại trên Server (so khớp qua mã MD5 Checksum), Server sẽ không ghi đè dữ liệu vật lý mà chỉ tham chiếu liên kết phiên bản. Quá trình tải lên diễn ra siêu tốc (dưới 10ms) giúp tiết kiệm tài nguyên đĩa và thời gian tối đa.
2. **🛡️ Cơ Chế Bảo Mật Chống Tấn Công RCE (Remote Code Execution):**
   - Chặn đứng tuyệt đối việc tải lên các tệp tin nguy hiểm chứa mã độc có đuôi nhạy cảm như `.jsp`, `.exe`, `.sh`, `.bat`, `.cmd`... để phòng tránh hacker chiếm quyền kiểm soát máy chủ.
3. **👥 Phân Quyền Chi Tiết Cấp Độ ACL (Access Control List):**
   - Hỗ trợ phân quyền động, chi tiết tới từng cá nhân người dùng hoặc chia sẻ công cộng cho mọi người trong hệ thống.
4. **📂 Thư Mục Ảo Phân Nhóm Người Chia Sẻ (Virtual Shared Folders):**
   - Gom nhóm tài nguyên được chia sẻ một cách khoa học. Người nhận sẽ thấy thư mục ảo dạng `(Share) [Tên_Người_Chia_Sẻ]` tại thư mục gốc giúp quản lý tập trung và tránh lộn xộn.
5. **⏳ Lịch Sử Phiên Bản (File Versioning):**
   - Hỗ trợ lưu trữ nhiều phiên bản của cùng một tệp tin khi tải lên ghi đè, cho phép xem lại lịch sử và tải xuống phiên bản cũ tùy thích.
6. **✏️ Đổi Tên Thư Mục & Đồng Bộ Materialized Path:**
   - Hỗ trợ đổi tên thư mục cực kỳ thông minh, tự động đồng bộ lại toàn bộ đường dẫn của tất cả các thư mục con cháu bên dưới.

---

## 🛠️ Yêu Cầu Hệ Thống

Trước khi bắt đầu, hãy đảm bảo máy tính của bạn đã cài đặt các công cụ sau:
- **Java Development Kit (JDK):** Phiên bản **17** trở lên
- **Node.js:** Phiên bản **18.x** trở lên (kèm theo `npm`)
- **Apache Maven:** Phiên bản **3.8.x** trở lên
- **MySQL Database Server:** Phiên bản **8.0** trở lên

---

## 🚀 Hướng Dẫn Khởi Chạy Dự Án

### Bước 1: Thiết Lập Cơ Sở Dữ Liệu (MySQL)

1. Mở MySQL Client (hoặc Navicat, DBeaver, phpMyAdmin) và chạy câu lệnh để tạo Database:
   ```sql
   CREATE DATABASE db_file_server CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Cấu hình thông tin kết nối DB trong file cấu hình dự án tại đường dẫn:
   `Backend/src/main/resources/application.yaml`
   - **URL:** `jdbc:mysql://localhost:3306/db_file_server`
   - **Username (Tài khoản):** Mặc định là `root`
   - **Password (Mật khẩu):** Mặc định là `123456`
   *(Bạn có thể thay đổi các giá trị này trực tiếp trong file hoặc truyền qua biến môi trường `DB_USERNAME` và `DB_PASSWORD`)*

---

### Bước 2: Khởi Chạy Backend (Spring Boot)

1. Mở cửa sổ Terminal/Command Prompt mới và di chuyển vào thư mục **Backend**:
   ```bash
   cd Backend
   ```
2. Thực hiện biên dịch và chạy dự án thông qua Maven:
   ```bash
   mvn spring-boot:run
   ```
3. Chờ cho đến khi màn hình Terminal hiển thị thông báo Spring Boot đã khởi động thành công (thông thường chạy ở cổng **8080**).
4. *Hệ thống sẽ tự động tạo bảng (Hibernate ddl-auto: update) và thực hiện gieo dữ liệu mẫu (Data Seeding) tự động.*

---

### Bước 3: Khởi Chạy Frontend (React/Vite)

1. Mở một cửa sổ Terminal/Command Prompt khác và di chuyển vào thư mục **Frontend**:
   ```bash
   cd Frontend
   ```
2. Thực hiện cài đặt các thư viện phụ thuộc (Dependencies):
   ```bash
   npm install
   ```
3. Khởi chạy Server phát triển (Development Server):
   ```bash
   npm run dev
   ```
4. Truy cập giao diện ứng dụng tại đường dẫn hiển thị trên màn hình (mặc định là `http://localhost:5173`).

---

## 👤 Danh Sách Tài Khoản Thử Nghiệm Mặc Định

Hệ thống đã chuẩn bị sẵn 3 nhóm tài khoản mẫu tương ứng với 3 vai trò (Roles) trong cơ sở dữ liệu để bạn kiểm thử chức năng chia sẻ:

| STT | Username | Mật khẩu | Họ và Tên | Vai Trò (Role) | Hạn Mức Quota |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | **admin** | `123456` | Super Administrator | **ADMIN** (Toàn quyền hệ thống) | 100 GB |
| 2 | **editor1** | `123456` | Editor Staff 01 | **EDITOR** (Đọc, ghi, chia sẻ, tạo thư mục) | 20 GB |
| 3 | **viewer1** | `123456` | Viewer Client 01 | **VIEWER** (Chỉ được phép xem/tải xuống) | 5 GB |

---

## 🛡️ Hướng Dẫn Kiểm Thử Chức Năng Phân Quyền (ACL)

1. **Đăng nhập bằng tài khoản `admin`:**
   - Tạo các thư mục con hoặc tải tệp tin lên.
   - Nhấn vào nút **Chia sẻ** (icon màu xanh lá) của một file hoặc folder.
   - Chọn người nhận chia sẻ là `viewer1`, chọn quyền `FILE_READ`, click cho phép và lưu lại.
2. **Đăng nhập bằng tài khoản `viewer1`:**
   - Bạn sẽ ngay lập tức thấy một thư mục ảo có tên `(Share) admin` hiển thị tại thư mục gốc.
   - Click đúp để mở thư mục ảo này, bạn sẽ thấy đầy đủ các tệp tin và thư mục mà `admin` đã chia sẻ riêng cho bạn.
   - Khi đang ở trong thư mục ảo này, các nút thao tác chỉnh sửa/tải lên sẽ tự động khóa để bảo vệ an toàn cho dữ liệu của chủ sở hữu.
