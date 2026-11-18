# 📚 Kütüphane Yönetim Sistemi — Mikroservis Mimarisi

Bu proje, modern **Spring Boot** ve **Spring Cloud** ekosistemini kullanarak geliştirilmiş, uçtan uca bir **Kütüphane Yönetim Sistemi** mikroservis uygulamasıdır. Sistem; kitap envanterinin yönetilmesi, stok takibi, kitap ödünç alma ve iade süreçlerini birbirinden bağımsız ancak uyumlu çalışan **4 ayrı mikroservis** üzerinden gerçekleştirir.

Projede yalnızca temel CRUD işlemleri değil; gerçek dünya mikroservis mimarilerinde karşılaşılan **Servis Keşfi (Service Discovery)**, **Tek Giriş Noktası (API Gateway)**, **Yük Dengeleme (Load Balancing)**, **Devre Kesici (Circuit Breaker)**, **Dağıtık İşlem Telafisi (Saga Compensation)**, **Eşzamanlılık / Yarış Durumu Koruması (Atomic Stock Update)** ve **Merkezi Hata Yönetimi** gibi kritik mühendislik desenleri uygulanmıştır.
.
---

## 🧭 İçindekiler

1. [Sistem Mimarisi ve Genel Bakış](#-1-sistem-mimarisi-ve-genel-bakış)
2. [Kullanılan Teknolojiler](#-2-kullanılan-teknolojiler)
3. [Mikroservislerin Detaylı Açıklaması](#-3-mikroservislerin-detaylı-açıklaması)
   - [3.1. Discovery Server (Eureka)](#31-discovery-server-eureka--port-8761)
   - [3.2. API Gateway](#32-api-gateway--port-8080)
   - [3.3. Book Service (Kitap Servisi)](#33-book-service-kitap-servisi--port-8081)
   - [3.4. Borrowing Service (Ödünç Alma Servisi)](#34-borrowing-service-ödünç-alma-servisi--port-8082)
4. [Uygulanan Mikroservis Tasarım Desenleri ve Mühendislik Çözümleri](#-4-uygulanan-mikroservis-tasarım-desenleri-ve-mühendislik-çözümleri)
5. [İş Akışı Diyagramları](#-5-iş-akışı-diyagramları)
6. [Proje Dizin Yapısı](#-6-proje-dizin-yapısı)
7. [Kurulum ve Çalıştırma Rehberi](#-7-kurulum-ve-çalıştırma-rehberi)
8. [API Dokümantasyonu ve Örnek İstekler (cURL)](#-8-api-dokümantasyonu-ve-örnek-istekler-curl)
9. [Veritabanı ve İzleme Panelleri](#-9-veritabanı-ve-izleme-panelleri)
10. [Hata Yönetimi ve HTTP Durum Kodları](#-10-hata-yönetimi-ve-http-durum-kodları)

---

## 🏗️ 1. Sistem Mimarisi ve Genel Bakış

Sistem 4 ana bileşenden oluşur:

| Servis Adı | Port | Veritabanı | Rolü ve Sorumluluğu |
| :--- | :---: | :---: | :--- |
| **`discovery-server`** | `8761` | Yok | **Netflix Eureka Server** — Tüm mikroservislerin kayıt olduğu ve birbirlerinin adreslerini dinamik olarak öğrendiği telefon rehberi. |
| **`api-gateway`** | `8080` | Yok | **Spring Cloud Gateway (WebFlux)** — Dış dünyadan gelen tüm isteklerin karşılandığı tek giriş kapısı. İstekleri ilgili servise yönlendirir. |
| **`book-service`** | `8081` | `H2 (bookdb)` | Kitap kayıtlarını, bilgilerini ve stok miktarlarını yöneten servis. |
| **`borrowing-service`** | `8082` | `H2 (borrowingdb)` | Kitap ödünç alma ve iade işlemlerini yürüten, `OpenFeign` ile `book-service`'le haberleşen servis. |

```mermaid
flowchart TB
    Client(["👤 İstemci (Postman / Tarayıcı / Frontend)"])

    subgraph GatewayLayer ["Giriş Katmanı"]
        GW["🚪 API Gateway\n(Port: 8080)"]
    end

    subgraph DiscoveryLayer ["Servis Keşif Katmanı"]
        Eureka["🧭 Discovery Server (Eureka)\n(Port: 8761)"]
    end

    subgraph BusinessLayer ["İş Mantığı ve Veri Katmanı"]
        BS["📘 Book Service\n(Port: 8081)"]
        BRS["📙 Borrowing Service\n(Port: 8082)"]
        BookDB[("🗄️ H2: bookdb\n(Bellek İçi)")]
        BorrowDB[("🗄️ H2: borrowingdb\n(Bellek İçi)")]
    end

    Client -->|"HTTP İstekleri\n(/api/books/** veya /api/borrowings/**)"| GW
    GW -->|"lb://book-service"| BS
    GW -->|"lb://borrowing-service"| BRS
    BRS -->|"OpenFeign + CircuitBreaker\n(PUT /internal/books/{id}/decrease-stock, PUT /internal/books/{id}/increase-stock)"| BS

    BS --- BookDB
    BRS --- BorrowDB

    GW -.->|"Kayıt & Adres Çözümleme"| Eureka
    BS -.->|"Kayıt (Register)"| Eureka
    BRS -.->|"Kayıt & Keşif (Register & Fetch)"| Eureka
```

---

## 🛠️ 2. Kullanılan Teknolojiler

* **Dil:** Java 21
* **Çatı (Framework):** Spring Boot `4.1.x`
* **Mikroservis Ekosistemi:** Spring Cloud `2025.1.x`
  * **Servis Keşfi:** Spring Cloud Netflix Eureka (`Server` & `Client`)
  * **API Ağ Geçidi:** Spring Cloud Gateway Server (`WebFlux` / Reaktif)
  * **Servisler Arası İletişim:** Spring Cloud OpenFeign + Spring Cloud LoadBalancer
  * **Dayanıklılık (Fault Tolerance):** Spring Cloud CircuitBreaker (`Resilience4j` CircuitBreaker & TimeLimiter)
* **Veri Erişimi:** Spring Data JPA (Hibernate)
* **Veritabanı:** H2 In-Memory Database (Her servis için izole edilmiş ayrı şema)
* **Doğrulama:** Jakarta Bean Validation (`spring-boot-starter-validation`)
* **Gözlemlenebilirlik:** Spring Boot Actuator
* **Yardımcı Kütüphaneler:** Project Lombok, SLF4J Logging
* **Derleme ve Paketleme:** Apache Maven (`mvnw` / Maven Wrapper dahil)

---

## 🔍 3. Mikroservislerin Detaylı Açıklaması

### 3.1. `discovery-server` (Eureka) — Port: `8761`

Mikroservis mimarisinde servislerin IP adresleri ve portları dinamik olarak değişebilir veya aynı servisten birden fazla kopya (instance) ayağa kaldırılabilir. `discovery-server`, **Netflix Eureka Server** olarak çalışır ve sistemdeki tüm servislerin adres defterini tutar.

* **Ana Sınıf:** `DiscoveryServerApplication.java` (`@EnableEurekaServer` anotasyonu ile aktif edilir).
* **Önemli Ayarlar (`application.properties`):**
  * `server.port=8761` (Eureka'nın standart portu).
  * `eureka.client.register-with-eureka=false` ve `eureka.client.fetch-registry=false`: Sunucunun kendi kendisine bir istemci gibi kayıt olmaya çalışmasını engeller.

---

### 3.2. `api-gateway` — Port: `8080`

İstemcilerin (`Frontend`, `Mobil Uygulama`, `Postman` vb.) arkadaki mikroservislerin portlarını (`8081`, `8082`) bilmesine gerek kalmadan **tek bir kapıdan (`http://localhost:8080`)** sisteme erişmesini sağlar.

* **Reaktif Mimari:** `spring-cloud-starter-gateway-server-webflux` bağımlılığı ile bloklamayan (non-blocking) yapıda çalışır.
* **Dinamik Yönlendirme ve Yük Dengeleme (`application.yml`):**
  * `/api/books/**` adresine gelen istekleri `lb://book-service` üzerinden `book-service` örneklerine yönlendirir.
  * `/api/borrowings/**` adresine gelen istekleri `lb://borrowing-service` üzerinden `borrowing-service` örneklerine yönlendirir.
  * `lb://` (Load Balanced) öneki sayesinde Gateway, hedef servisin adresini Eureka'dan öğrenir ve birden fazla örnek varsa yükü aralarında paylaştırır.
* **Global CORS Yapılandırması:** Tarayıcı tabanlı önyüz uygulamalarının (`http://localhost:*`) `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS` isteklerini sorunsuz gönderebilmesi için CORS izinleri tanımlanmıştır.
* **Actuator Desteği:** `/actuator/health`, `/actuator/info` ve `/actuator/gateway/routes` uç noktaları açıktır.
* **İç Uç Noktaların Gizlenmesi:** Servisler arası uç noktalar (`/internal/**`) için bilerek **hiçbir rota tanımlanmamıştır**. Böylece stok değiştiren uç noktalar dış dünyadan (`8080`) çağrılamaz; `404` döner.
* **`PreserveHostHeader` Filtresi:** İstemcinin `Host` başlığı servislere aynen iletilir. Böylece `201 Created` yanıtlarındaki `Location` adresi servisin kendi adresini değil, Gateway adresini (`http://localhost:8080/...`) gösterir.

---

### 3.3. `book-service` (Kitap Servisi) — Port: `8081`

Kütüphanedeki kitapların kaydını ve mevcut stok durumunu yönetir. Temiz ve modüler bir **Katmanlı Mimari (Layered Architecture)** ile tasarlanmıştır:

* **`entity/Book.java`**: `books` tablosunu temsil eder (`id`, `title`, `author`, `stock`).
* **`dto/BookRequest.java`**: Kitap ekleme isteklerinde dışarıdan yalnızca `title`, `author` ve `stock` alanlarını kabul eder. Doğrudan `Book` entity'sini dışarıya açmayarak istemcinin `id` göndermesini engeller ve `@NotBlank`, `@NotNull`, `@Min(0)` anotasyonlarıyla veriyi daha kontrol katmanında doğrular.
* **`repository/BookRepository.java`**:
  * `decreaseStockIfAvailable(Long id)`: `UPDATE Book b SET b.stock = b.stock - 1 WHERE b.id = :id AND b.stock > 0` sorgusu ile stok kontrolünü ve stok düşürme işlemini **tek bir atomik SQL sorgusunda** gerçekleştirir.
  * `increaseStock(Long id)`: İade veya telafi durumlarında stoğu 1 artırır.
* **`service/BookService.java`**: İş kurallarını barındırır. Kitap bulunamadığında `BookNotFoundException` (`404`), stok tükendiğinde `OutOfStockException` (`409`) fırlatır.
* **`dto/BookResponse.java`**: İstemciye dönen kitap verisi (`record`). Entity dışarıya açılmaz; veritabanı yapısı ile API sözleşmesi birbirinden bağımsızdır.
* **`controller/BookController.java`**: `/api/books` altındaki **dışa açık** uç noktalar (kitap ekleme, listeleme, getirme). Kitap ekleme `201 Created` + `Location` başlığı döner.
* **`controller/InternalBookController.java`**: `/internal/books` altındaki **servisler arası** stok uç noktaları (`decrease-stock`, `increase-stock`). Yalnızca `borrowing-service` tarafından çağrılır; API Gateway üzerinden erişilemez.
* **`exception/GlobalExceptionHandler.java`**: Servis genelinde fırlatılan özel istisnaları ve doğrulama hatalarını yakalayarak standart `ErrorResponse` JSON formatında döner.

---

### 3.4. `borrowing-service` (Ödünç Alma Servisi) — Port: `8082`

Kullanıcıların kitap ödünç alma ve iade etme süreçlerini koordine eder.

* **Bağımsız Veritabanı (`borrowingdb`):** Mikroservis prensiplerine uygun olarak `book-service`'in veritabanına doğrudan bağlanmaz ve `Book` tablosuna SQL `Foreign Key` kurmaz. Yalnızca `bookId` ve sorgu kolaylığı için `bookTitle` bilgisini kendi `borrowings` tablosunda saklar.
* **Durum Takibi (`BorrowingStatus`):** Her ödünç kaydı `BORROWED` (Ödünç Alındı) veya `RETURNED` (İade Edildi) durumlarından birine sahiptir. Zaten iade edilmiş bir kitabın tekrar iade edilerek stoğu haksız yere artırması `BookAlreadyReturnedException` (`409 Conflict`) ile engellenir.
* **`BookClient` (OpenFeign):** `@FeignClient(name = "book-service", path = "/internal/books", fallbackFactory = BookClientFallbackFactory.class)` tanımıyla, hiçbir sabit IP veya port yazmadan Eureka üzerinden `book-service` ile HTTP üzerinden haberleşir. Ödünç alırken **tek bir** `decrease-stock` çağrısı yapılır: bu çağrı hem kitabın varlığını hem stoğu kontrol eder ve güncel kitap bilgisini döner (ayrı bir "kitabı getir" çağrısına gerek kalmaz).
* **`dto/BorrowingResponse.java`**: İstemciye dönen ödünç kaydı verisi (`record`). Entity dışarıya açılmaz.
* **`BookClientErrorDecoder`:** `book-service`'ten dönen `404` ve `409` HTTP yanıtlarını yakalar, JSON gövdesindeki Türkçe hata mesajını okur ve `BookNotFoundException` veya `OutOfStockException` olarak yeniden fırlatır.
* **`ResilienceConfig` & `BookClientFallbackFactory`:** `book-service` çöktüğünde, yavaşladığında veya ağ hatası oluştuğunda devreye girerek sistemi korur ve kullanıcıya anlamlı bir `503 Service Unavailable` mesajı döner.

---

## 🧠 4. Uygulanan Mikroservis Tasarım Desenleri ve Mühendislik Çözümleri

### 1. Database per Service (Servis Başına Veritabanı)
Her mikroservis kendi verisinin tek sahibidir:
* `book-service` ➡️ `jdbc:h2:mem:bookdb`
* `borrowing-service` ➡️ `jdbc:h2:mem:borrowingdb`
İki servis birbirinin tablolarına doğrudan erişemez; veri alışverişi yalnızca iyi tanımlanmış REST API sözleşmeleri üzerinden yapılır.

### 2. Atomik Stok Güncellemesi (Race Condition Önleme)
Klasik "Önce kitabı oku (`SELECT`) ➡️ Stok > 0 mı kontrol et ➡️ Bellekte `stock - 1` yap ➡️ Kaydet (`UPDATE`)" yaklaşımında, aynı anda son kitabı ödünç almak isteyen iki kişiden ikisi de stoğu `1` görüp stoğu `-1`'e düşürebilir (**Race Condition**).

Bu projede stok kontrolü ve azaltma işlemi veritabanı seviyesinde **atomik** olarak yapılır:
```sql
UPDATE Book b SET b.stock = b.stock - 1 WHERE b.id = :id AND b.stock > 0
```
Veritabanı bu satır güncellemesini kilitler (row-level locking). Etkilenen satır sayısı `0` dönerse, stok kalmadığı kesin olarak anlaşılır ve `409 Conflict` (`OutOfStockException`) fırlatılır.

### 3. Saga Pattern — Telafi Edici İşlemler (Compensating Transactions)
Mikroservislerde her servisin kendi veritabanı olduğu için tek bir `@Transactional` anotasyonu ile iki ayrı veritabanındaki işlemi geri almak (rollback) mümkün değildir.
`BorrowingService` içinde **Saga Orchestration / Telafi (Compensation)** yaklaşımı uygulanmıştır:
* **Ödünç Alma Telafisi:** `book-service`'te stok başarıyla düşürüldükten sonra `borrowing-service` kendi veritabanına ödünç kaydını yazarken bir hata alırsa, `compensateIncreaseStock(bookId)` metodu çağrılarak `book-service`'teki stok otomatik olarak geri artırılır.
* **İade Etme Telafisi:** İade sırasında `book-service`'te stok artırıldıktan sonra `borrowing-service`'teki kayıt `RETURNED` olarak güncellenemezse, `compensateDecreaseStock(bookId)` çağrılarak artırılan stok geri düşürülür.

### 4. Circuit Breaker (Devre Kesici) ve Zaman Aşımı (Timeouts)
`borrowing-service`, `book-service`'i çağırırken `book-service` yanıt vermezse tüm sistemin kilitlenmesini önlemek için çok katmanlı koruma kullanır:
* **Feign Zaman Aşımları:** Bağlantı kurma süresi (`connect-timeout`) en fazla **2 saniye**, yanıt okuma süresi (`read-timeout`) en fazla **3 saniye** ile sınırlandırılmıştır.
* **Resilience4j Devre Kesici (`ResilienceConfig.java`):**
  * Son **10 çağrıyı** (`slidingWindowSize = 10`) izler (en az **5 çağrı** yapıldıktan sonra karar verir).
  * Çağrıların **%50 veya daha fazlası** başarısız olursa (veya **4 saniyelik** `TimeLimiter` süresini aşarsa) devre **AÇIK (OPEN)** konumuna geçer ("sigorta atar").
  * **AÇIK** konumda **10 saniye** boyunca `book-service`'e hiç istek gönderilmez; anında `BookClientFallbackFactory` devreye girer.
  * 10 saniye sonunda devre **YARI AÇIK (HALF-OPEN)** durumuna geçer ve **3 deneme isteğine** izin verir. Başarılı olursa devre tekrar **KAPALI (CLOSED)** (normal) duruma döner.
  * **Akıllı İstisna Filtreleme (`ignoreExceptions`):** `BookNotFoundException` (`404`) ve `OutOfStockException` (`409`) birer sistem arızası değil, normal iş kuralı sonucu olduğu için devre kesicinin hata oranına **dahil edilmez**.

---

## 🔄 5. İş Akışı Diyagramları

### 5.1. Kitap Ödünç Alma ve Saga Telafi Akışı

```mermaid
sequenceDiagram
    autonumber
    actor User as 👤 Kullanıcı
    participant GW as 🚪 API Gateway (8080)
    participant BRS as 📙 Borrowing Service (8082)
    participant BS as 📘 Book Service (8081)
    participant BookDB as 🗄️ bookdb
    participant BorrowDB as 🗄️ borrowingdb

    User->>GW: POST /api/borrowings?bookId=1&borrowerName=Ahmet
    GW->>BRS: İsteği Yönlendir (lb://borrowing-service)
    
    BRS->>BS: 1. PUT /internal/books/1/decrease-stock (Feign Client)
    BS->>BookDB: SELECT (kitap var mı?) + UPDATE books SET stock = stock - 1 WHERE id = 1 AND stock > 0
    alt Kitap Bulunamadı
        BS-->>BRS: 404 Not Found
        BRS-->>GW: 404 Not Found (BookNotFoundException)
        GW-->>User: 404 Kitap bulunamadı
    else Stok Yetersiz (updatedRows == 0)
        BS-->>BRS: 409 Conflict
        BRS-->>GW: 409 Conflict (OutOfStockException)
        GW-->>User: 409 Kitabın stoğu tükenmiş
    else Stok Başarıyla Düşürüldü
        BS-->>BRS: 200 OK (güncel BookDto)
    end

    BRS->>BorrowDB: 2. INSERT INTO borrowings (status = BORROWED)
    alt Kayıt Başarılı
        BorrowDB-->>BRS: Kaydedilen Borrowing nesnesi
        BRS-->>GW: 201 Created (BorrowingResponse + Location)
        GW-->>User: 201 Created
    else Veritabanı Hatası (Saga Telafi Adımı)
        BorrowDB-->>BRS: Exception!
        Note over BRS,BS: ⚠️ TELAFİ (Compensation): Düşürülen stok geri verilir
        BRS->>BS: 3. PUT /internal/books/1/increase-stock
        BS->>BookDB: UPDATE books SET stock = stock + 1 WHERE id = 1
        BRS-->>GW: 500 Internal Server Error (BorrowingFailedException)
        GW-->>User: 500 Ödünç alma kaydı oluşturulamadı, işlem geri alındı
    end
```

### 5.2. Devre Kesici (Circuit Breaker) Durum Diyagramı

```mermaid
stateDiagram-v2
    [*] --> KAPALI
    KAPALI --> ACIK : Son 10 çağrıda (min 5 çağrı)\nHata Oranı >= %50 veya Süre > 4sn
    ACIK --> YARI_ACIK : 10 saniye bekleme süresi dolduğunda
    YARI_ACIK --> KAPALI : 3 deneme çağrısı başarılı olursa
    YARI_ACIK --> ACIK : Deneme çağrıları başarısız olursa

    note right of KAPALI
      Normal çalışma durumu.
      404 ve 409 hataları iş kuralı
      olduğu için hata sayılmaz.
    end note

    note right of ACIK
      Book-Service'e istek atılmaz.
      Anında 503 Service Unavailable döner.
    end note
```

---

## 📂 6. Proje Dizin Yapısı

```text
kutuphane-mikroservis/
├── discovery-server/                  # Eureka Server (Port: 8761)
│   ├── src/main/java/com/kutuphane/discoveryserver/
│   │   └── DiscoveryServerApplication.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
├── api-gateway/                       # Spring Cloud Gateway (Port: 8080)
│   ├── src/main/java/com/kutuphane/apigateway/
│   │   └── ApiGatewayApplication.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── book-service/                      # Kitap Mikroservisi (Port: 8081)
│   ├── src/main/java/com/kutuphane/bookservice/
│   │   ├── controller/
│   │   │   ├── BookController.java           # Dışa açık: /api/books
│   │   │   └── InternalBookController.java   # Servisler arası: /internal/books (stok)
│   │   ├── dto/
│   │   │   ├── BookRequest.java
│   │   │   └── BookResponse.java
│   │   ├── entity/Book.java
│   │   ├── exception/
│   │   │   ├── BookNotFoundException.java
│   │   │   ├── ErrorResponse.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── OutOfStockException.java
│   │   ├── repository/BookRepository.java
│   │   ├── service/BookService.java
│   │   └── BookServiceApplication.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
└── borrowing-service/                 # Ödünç Alma Mikroservisi (Port: 8082)
    ├── src/main/java/com/kutuphane/borrowingservice/
    │   ├── client/
    │   │   ├── BookClient.java
    │   │   ├── BookClientErrorDecoder.java
    │   │   └── BookClientFallbackFactory.java
    │   ├── config/ResilienceConfig.java
    │   ├── controller/BorrowingController.java
    │   ├── dto/
    │   │   ├── BookDto.java
    │   │   └── BorrowingResponse.java
    │   ├── entity/
    │   │   ├── Borrowing.java
    │   │   └── BorrowingStatus.java
    │   ├── exception/
    │   │   ├── BookAlreadyReturnedException.java
    │   │   ├── BookNotFoundException.java
    │   │   ├── BookServiceUnavailableException.java
    │   │   ├── BorrowingFailedException.java
    │   │   ├── BorrowingNotFoundException.java
    │   │   ├── ErrorResponse.java
    │   │   ├── GlobalExceptionHandler.java
    │   │   └── OutOfStockException.java
    │   ├── repository/BorrowingRepository.java
    │   ├── service/BorrowingService.java
    │   └── BorrowingServiceApplication.java
    ├── src/main/resources/
    │   └── application.properties
    └── pom.xml
```

---

## 🚀 7. Kurulum ve Çalıştırma Rehberi

### Ön Gereksinimler
* **Java 21 (JDK 21)** kurulu olmalıdır (`java -version` ile kontrol edebilirsiniz).
* Her proje klasöründe **Maven Wrapper (`mvnw` / `mvnw.cmd`)** yer aldığı için bilgisayarınızda ayrıca Maven kurulu olmasına gerek yoktur.

### ⚠️ Çalıştırma Sırası (Çok Önemli!)
Servislerin birbirini bulabilmesi için **mutlaka aşağıdaki sırayla** ayağa kaldırılması önerilir:

#### 1. Adım: Discovery Server'ı Başlatın (`Port: 8761`)
```powershell
cd discovery-server
.\mvnw.cmd spring-boot:run
```
> Tarayıcıdan `http://localhost:8761` adresine giderek Eureka panelinin açıldığını doğrulayın.

#### 2. Adım: Book Service'i Başlatın (`Port: 8081`)
Yeni bir terminal açın:
```powershell
cd book-service
.\mvnw.cmd spring-boot:run
```

#### 3. Adım: Borrowing Service'i Başlatın (`Port: 8082`)
Yeni bir terminal açın:
```powershell
cd borrowing-service
.\mvnw.cmd spring-boot:run
```

#### 4. Adım: API Gateway'i Başlatın (`Port: 8080`)
Yeni bir terminal açın:
```powershell
cd api-gateway
.\mvnw.cmd spring-boot:run
```

> Tüm servisler başladıktan sonra `http://localhost:8761` adresindeki Eureka panelinde **`API-GATEWAY`**, **`BOOK-SERVICE`** ve **`BORROWING-SERVICE`** uygulamalarının **`UP`** durumunda göründüğünden emin olun.

---

## 📡 8. API Dokümantasyonu ve Örnek İstekler (cURL)

Tüm istekler **API Gateway (`http://localhost:8080`)** üzerinden yapılır.

### 📘 Kitap İşlemleri (`book-service`)

#### 1. Yeni Kitap Ekle
* **Endpoint:** `POST /api/books`
* **Örnek İstek:**
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Suç ve Ceza",
    "author": "Fyodor Dostoyevski",
    "stock": 3
  }'
```
* **Örnek Yanıt (`201 Created`):** *(Yanıt başlığında `Location: http://localhost:8080/api/books/1` döner.)*
```json
{
  "id": 1,
  "title": "Suç ve Ceza",
  "author": "Fyodor Dostoyevski",
  "stock": 3
}
```

#### 2. Tüm Kitapları Listele
* **Endpoint:** `GET /api/books`
```bash
curl -X GET http://localhost:8080/api/books
```

#### 3. ID'ye Göre Kitap Getir
* **Endpoint:** `GET /api/books/{id}`
```bash
curl -X GET http://localhost:8080/api/books/1
```

> [!NOTE]
> Stok değiştiren uç noktalar (`PUT /internal/books/{id}/decrease-stock` ve `PUT /internal/books/{id}/increase-stock`) **servisler arası** kullanım içindir. API Gateway'de bu yol için rota tanımlı olmadığından `8080` üzerinden çağrılamaz (`404`). Stok yalnızca ödünç alma / iade işlemleriyle değişir.

---

### 📙 Ödünç Alma ve İade İşlemleri (`borrowing-service`)

#### 1. Kitap Ödünç Al
* **Endpoint:** `POST /api/borrowings`
* **Sorgu Parametreleri (Query Params):**
  * `bookId` (Zorunlu, Pozitif tam sayı)
  * `borrowerName` (Zorunlu, Boş olamaz)
* **Örnek İstek:**
```bash
curl -X POST "http://localhost:8080/api/borrowings?bookId=1&borrowerName=Ahmet%20Yilmaz"
```
* **Örnek Yanıt (`201 Created`):** *(Yanıt başlığında `Location: http://localhost:8080/api/borrowings/1` döner.)*
```json
{
  "id": 1,
  "bookId": 1,
  "bookTitle": "Suç ve Ceza",
  "borrowerName": "Ahmet Yilmaz",
  "borrowDate": "2026-11-15",
  "returnDate": null,
  "status": "BORROWED"
}
```
*(Bu işlem sonunda `book-service`'teki ilgili kitabın stoğu otomatik olarak `3` ➡️ `2` olur.)*

#### 2. Kitap İade Et
* **Endpoint:** `PUT /api/borrowings/{id}/return`
* **Örnek İstek:**
```bash
curl -X PUT http://localhost:8080/api/borrowings/1/return
```
* **Örnek Yanıt (`200 OK`):**
```json
{
  "id": 1,
  "bookId": 1,
  "bookTitle": "Suç ve Ceza",
  "borrowerName": "Ahmet Yilmaz",
  "borrowDate": "2026-11-15",
  "returnDate": "2026-11-15",
  "status": "RETURNED"
}
```
*(Bu işlem sonunda `book-service`'teki ilgili kitabın stoğu otomatik olarak `2` ➡️ `3` olur ve `returnDate` bugünün tarihiyle güncellenir.)*

#### 3. Tüm Ödünç Kayıtlarını Listele
* **Endpoint:** `GET /api/borrowings`
```bash
curl -X GET http://localhost:8080/api/borrowings
```

#### 4. ID'ye Göre Ödünç Kaydı Getir
* **Endpoint:** `GET /api/borrowings/{id}`
```bash
curl -X GET http://localhost:8080/api/borrowings/1
```

---

## 🖥️ 9. Veritabanı ve İzleme Panelleri

Uygulama çalışırken aşağıdaki yönetim ve izleme ekranlarına tarayıcı üzerinden ulaşabilirsiniz:

| Panel / Araç | URL | Ek Bilgiler |
| :--- | :--- | :--- |
| **Eureka Dashboard** | `http://localhost:8761` | Kayıtlı tüm mikroservisleri ve durumlarını gösterir. |
| **API Gateway Rotaları** | `http://localhost:8080/actuator/gateway/routes` | Gateway üzerinde tanımlı aktif yönlendirme kurallarını JSON olarak listeler. |
| **API Gateway Sağlık Durumu** | `http://localhost:8080/actuator/health` | Gateway'in çalışma durumunu gösterir. |
| **Book-Service Sağlık Durumu** | `http://localhost:8081/actuator/health` | Servis ve veritabanı (`db`) sağlık detaylarını gösterir. |
| **Borrowing-Service Sağlık Durumu** | `http://localhost:8082/actuator/health` | Servis ve veritabanı (`db`) sağlık detaylarını gösterir. |
| **Book-Service H2 Console** | `http://localhost:8081/h2-console` | **JDBC URL:** `jdbc:h2:mem:bookdb`<br>**User Name:** `sa`<br>**Password:** *(boş bırakın)* |
| **Borrowing-Service H2 Console** | `http://localhost:8082/h2-console` | **JDBC URL:** `jdbc:h2:mem:borrowingdb`<br>**User Name:** `sa`<br>**Password:** *(boş bırakın)* |

---

## ⚠️ 10. Hata Yönetimi ve HTTP Durum Kodları

Hem `book-service` hem de `borrowing-service` üzerinde `@RestControllerAdvice` tabanlı merkezi hata yakalayıcı (`GlobalExceptionHandler`) bulunur. Tüm hatalar istemciye aşağıdaki standart JSON şemasıyla (`ErrorResponse`) döndürülür:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "'Suç ve Ceza' adlı kitabın stoğu tükenmiş!",
  "timestamp": "2026-11-15T16:45:12.345678"
}
```

| HTTP Kodu | Durum | Fırlatıldığı Senaryolar |
| :---: | :--- | :--- |
| **`400`** | `Bad Request` | Eksik veya geçersiz parametre gönderildiğinde (Örn: Boş kitap adı, negatif stok, negatif `bookId`, boş `borrowerName`). |
| **`404`** | `Not Found` | İstenen `bookId` veya `borrowingId` veritabanında bulunamadığında (`BookNotFoundException`, `BorrowingNotFoundException`). |
| **`409`** | `Conflict` | Kitabın stoğu `0` iken ödünç alınmaya çalışıldığında (`OutOfStockException`) veya zaten iade edilmiş bir kayıt tekrar iade edilmeye çalışıldığında (`BookAlreadyReturnedException`). |
| **`500`** | `Internal Server Error` | Ödünç veya iade kaydı veritabanına yazılamayıp Saga telafi adımı çalıştırıldığında (`BorrowingFailedException`). |
| **`503`** | `Service Unavailable` | `book-service` kapalı olduğunda, zaman aşımına uğradığında veya **Circuit Breaker (Devre Kesici)** açık olduğunda (`BookServiceUnavailableException`). |
.
