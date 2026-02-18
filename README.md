# ⚡ Mini Model Mapper

[![Maven Central](https://img.shields.io/maven-central/v/io.github.yasasbanukaofficial/mini-model-mapper.svg?label=Maven%20Central)](https://central.sonatype.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://www.oracle.com/java/technologies/downloads/)

**Mini Model Mapper** is a high-efficiency, zero-dependency Java library designed for modern developers who need fast object-to-object mapping without the bloat of traditional frameworks.

---

## ✨ Key Features

* 🚀 **High Performance:** Optimized reflection-based mapping with meta-data caching for near-native speed.
* 📦 **Zero Dependencies:** Keeps your project lightweight and avoids dependency hell.
* ☕ **Java 21 Ready:** Fully compatible with the latest LTS features.
* 🛠️ **Simple Model Mapping:** Single-line mapping for DTOs, Entities, and POJOs.

---

## 📦 Installation

Add the following dependency to your `pom.xml`, refer the mvnrepository for the latest version:

```xml
<dependency>
    <groupId>io.github.yasasbanukaofficial</groupId>
    <artifactId>mini-model-mapper</artifactId>
    <version>1.0.2</version>
</dependency>
```

## 🚀 Usage

### 1. Spring Boot Integration (Recommended)

Define the mapper as a Bean in your configuration or main application class:

```java
@SpringBootApplication
public class BackendApplication {
    @Bean
    public MiniModelMapper miniModelMapper() {
        return new MiniModelMapper();
    }
}

```

Inject it into your service and map objects with a single line:

```java
@Service
public class UserService {
    @Autowired
    private MiniModelMapper mapper;

    public UserResponseDTO createUser(UserRequestDTO request) {
        // Map DTO to Entity
        UserEntity user = mapper.map(request, UserEntity.class);
        
        // ... save to database ...
        
        return mapper.map(user, UserResponseDTO.class);
    }
}

```

### 2. Standard Java Usage

For non-Spring projects, simply create an instance. The mapper uses internal caching, so it is recommended to reuse a single instance (Singleton).

```java
MiniModelMapper mapper = new MiniModelMapper();

Source source = new Source("John Doe", 25);
Destination destination = mapper.map(source, Destination.class);

```

---

## 💡 Pro-Tips for Complex Objects

**Mini Model Mapper** is designed to be safe and efficient. To avoid infinite recursion (circular dependencies) and "LazyInitializationExceptions" in JPA, the mapper automatically skips Collections and Maps.

Handle nested collections using Java Streams:

```java
OrderDTO orderDTO = mapper.map(orderEntity, OrderDTO.class);

// Manually map nested collections to maintain control and safety
orderDTO.setItems(
    orderEntity.getItems().stream()
               .map(item -> mapper.map(item, ItemDTO.class))
               .toList()
);

```

---

## 🛠️ Requirements

* **Java 21** or higher.
* Objects must have a **no-args constructor** (can be private/protected).

