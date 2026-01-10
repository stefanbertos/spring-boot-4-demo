# Project Development Guidelines

This document outlines the development standards and best practices for this project.

## Core Principles

### 1. Dependency Injection
- **ALWAYS use constructor-based dependency injection**
- Never use field injection (`@Autowired` on fields)
- For `@Value` properties, inject them through constructor parameters in `@Configuration` classes
- Mark all dependencies as `final` to ensure immutability

```java
// ✅ Good
@Service
public class MyService {
    private final MyRepository repository;
    private final String configValue;

    public MyService(MyRepository repository, String configValue) {
        this.repository = repository;
        this.configValue = configValue;
    }
}

// ❌ Bad
@Service
public class MyService {
    @Autowired
    private MyRepository repository;

    @Value("${config.value}")
    private String configValue;
}
```

### 2. Virtual Threads
- **ALWAYS enable and use virtual threads** for better concurrency
- Configure in `application.yaml`:
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### 3. Java Records
- **Use Java records for DTOs and immutable data structures**
- Records provide automatic implementation of `equals()`, `hashCode()`, `toString()`, and constructor
- Use records instead of Lombok `@Data` classes for simple data carriers

```java
// ✅ Good
public record TransactionDto(
    String id,
    BigDecimal amount,
    LocalDateTime timestamp
) {}

// ❌ Bad
@Data
public class TransactionDto {
    private String id;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
```

### 3.1. Jackson 3 (JSON Serialization)
- **ALWAYS use `JsonMapper` instead of `ObjectMapper`** (Jackson 3 best practice)
- Jackson 3 introduces an immutable `JsonMapper` replacing the mutable `ObjectMapper` from Jackson 2
- Use `JsonMapper.builder()` to configure and build the mapper
- Always enable module discovery with `.findAndAddModules()` to support Java 8 date/time types

```java
// ✅ Good - Jackson 3 style
import com.fasterxml.jackson.databind.json.JsonMapper;

private final JsonMapper jsonMapper = JsonMapper.builder()
        .findAndAddModules()
        .build();

// ❌ Bad - Jackson 2 style (deprecated in Jackson 3)
import com.fasterxml.jackson.databind.ObjectMapper;

private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules();
```

**Required Dependencies:**
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` - For Java 8 date/time support
- This is included in `spring-boot-starter-jackson` but should be explicitly declared for clarity

### 4. Testing Standards

#### Unit Tests
- **100% unit test coverage is MANDATORY** for all business logic
- Every service, converter, and component must have comprehensive unit tests
- Use Mockito for mocking dependencies
- Use JUnit 5 for all tests
- Test both happy paths and error scenarios

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyRepository repository;

    private MyService service;

    @BeforeEach
    void setUp() {
        service = new MyService(repository);
    }

    @Test
    void shouldHandleHappyPath() {
        // Test implementation
    }

    @Test
    void shouldHandleErrorScenario() {
        // Test implementation
    }
}
```

#### Integration Tests
- **All interfacing components MUST have integration tests**
- Integration tests should verify:
  - MQ message consumption
  - Kafka message production
  - REST API endpoints
  - Database operations
- Use `@SpringBootTest` for integration tests
- Use embedded or testcontainers for external dependencies when possible

### 5. Metrics and Observability
- Use Micrometer annotations for metrics instead of manual counter/timer creation
- Prefer `@Counted` and `@Timed` annotations on methods
- Export metrics to Prometheus
- Provide meaningful metric names and descriptions

```java
// ✅ Good
@Counted(value = "messages.received", description = "Total messages received")
@Timed(value = "message.processing.time", description = "Message processing time")
public void processMessage(String message) {
    // Implementation
}

// ❌ Bad
public void processMessage(String message) {
    messagesCounter.increment();
    // Implementation
}
```

### 6. Code Organization

#### Package Structure
```
com.example.demo
├── config          # Configuration classes
├── controller      # REST controllers
├── converter       # Message converters
├── dto             # Data Transfer Objects (Records)
├── listener        # Message listeners (JMS, Kafka)
├── service         # Business logic services
└── repository      # Data access repositories (if needed)
```

#### Multi-Module Project Structure
- Use multi-module Maven structure
- Keep infrastructure configuration in root folder
- Share common dependencies in parent `pom.xml`

### 7. Logging
- Use SLF4J with Lombok's `@Slf4j` annotation
- Log at appropriate levels:
  - `ERROR`: Errors that need immediate attention
  - `WARN`: Warnings about potential issues
  - `INFO`: General informational messages (message received/sent, etc.)
  - `DEBUG`: Detailed debugging information
  - `TRACE`: Very detailed debugging information

### 8. Error Handling
- Always handle exceptions appropriately
- Provide meaningful error messages
- Log errors with context
- Consider using custom exceptions for business logic errors

### 9. Configuration Management
- Use `application.yaml` for configuration (not `application.properties`)
- Group related configurations together
- Use meaningful property names
- Document non-obvious configuration values

### 10. Code Quality
- Code must be clean, readable, and maintainable
- Follow Java naming conventions
- Keep methods small and focused (Single Responsibility Principle)
- Avoid code duplication (DRY principle)
- Use meaningful variable and method names

## Technology Stack

### Required Dependencies
- Spring Boot 4.x
- Java 25
- Lombok (for `@Slf4j`, `@RequiredArgsConstructor`)
- Micrometer for metrics
- Prometheus for metrics export
- JUnit 5 for testing
- Mockito for mocking
- AssertJ for assertions

### Code Quality Tools
- **SpotBugs**: Detects bugs in Java code through static analysis
- **Checkstyle**: Enforces coding standards and style guidelines
- **PMD**: Finds potential problems like unused variables, empty catch blocks, unnecessary object creation

### Infrastructure
- IBM MQ for message queuing
- Apache Kafka for event streaming
- Prometheus for metrics collection
- Grafana for metrics visualization
- Docker Compose for local development

## Code Quality Standards

### Static Analysis
All code must pass static analysis checks before merging:
- **SpotBugs** must report no bugs
- **Checkstyle** violations must be addressed
- **PMD** issues must be fixed

Run all checks with:
```bash
mvn clean verify
```

### Architecture Rules
The codebase follows layered architecture validated by ArchUnit:
- Controllers only in `controller` package
- Services only in `service` package
- Repositories only in `repository` package (if exists)
- DTOs only in `dto` package
- Converters only in `converter` package
- No cyclic dependencies between packages
- Proper dependency direction (controller → service → repository)

## Continuous Improvement
- Review and update these guidelines as the project evolves
- All team members should follow these guidelines
- Code reviews should verify compliance with these standards
- Static analysis tools should be run before committing code
- 
### Spring Boot 4
- Spring Boot 4.0 has a new modular design and now ships smaller focused modules rather than several large jars. As a result, this release has a more consistent set of “starter” POMs, and features that only relied on a third-party dependency to work can require an additional starter.
You will need to make dependency modifications if your current application doesn’t use Spring Boot “starter” POMs.
Main Code
The new modules and “starter” POMs follow a convention that lets you identify where the support for a given technology comes from:
All Spring Boot modules are named spring-boot-<technology>.
The root package of each module is org.springframework.boot.<technology>.
All “starter” POMs are named spring-boot-starter-<technology>.
To illustrate the above, support for GraphQL is in spring-boot-graphql with a root package of org.springframework.boot.graphql. The “starter” POM is spring-boot-starter-graphql.
- Test Code
  The modularity also applies to the test infrastructure. A similar convention is applied:
All Spring Boot test modules are named spring-boot-<technology>-test.
The root package of each such module is org.springframework.boot.<technology>.test.
All test “starter” POMs are named spring-boot-starter-<technology>-test.
To illustrate the above, test infrastructure for GraphQL is in spring-boot-graphql-test with a root package of org.springframework.boot.graphql.test. The “starter” POM is spring-boot-starter-graphql-test.