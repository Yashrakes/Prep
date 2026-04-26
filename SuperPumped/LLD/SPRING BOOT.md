

---

## Spring Boot Interview Guide — 30 Questions Deep Dive

---

### ⚙️ INTERNALS

---

**Q1. How does Spring Boot decide which auto-configuration to apply?**

Spring Boot reads `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Boot 3.x) via `SpringFactoriesLoader`. It loads all listed auto-config class names, then evaluates `@Conditional` annotations on each to decide whether to apply it.

Key conditions:

- `@ConditionalOnClass` — class must be on classpath
- `@ConditionalOnMissingBean` — only if you haven't defined the bean
- `@ConditionalOnProperty` — based on a property value
- `@ConditionalOnWebApplication` — only in web context

java

```java
@Configuration
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
public class WebMvcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InternalResourceViewResolver defaultViewResolver() {
        return new InternalResourceViewResolver();
    }
}

// Your own bean suppresses the auto-config bean:
@Bean
public InternalResourceViewResolver customViewResolver() {
    return new InternalResourceViewResolver("/views/", ".html");
}
```

**Follow-ups:**

- _How do you debug which auto-configs applied?_ → `--debug` flag or `debug=true` in properties. Spring prints a CONDITIONS EVALUATION REPORT.
- _How to exclude one?_ → `@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)`

---

**Q2. What happens internally when you add spring-boot-starter-web?**

It's a BOM POM that pulls in spring-webmvc, spring-core, jackson-databind, and embedded Tomcat. Once Tomcat/DispatcherServlet are on the classpath, `WebMvcAutoConfiguration` and `EmbeddedWebServerFactoryCustomizerAutoConfiguration` fire automatically.

java

```java
// What Spring does:
// 1. Finds Tomcat.class, DispatcherServlet.class on classpath
// 2. EmbeddedTomcat @Configuration creates TomcatServletWebServerFactory
// 3. DispatcherServletAutoConfiguration registers DispatcherServlet
// 4. WebMvcAutoConfiguration adds HandlerMappings, MessageConverters

// Swap to Jetty:
<exclusions>
  <exclusion>spring-boot-starter-tomcat</exclusion>
</exclusions>
<dependency>spring-boot-starter-jetty</dependency>
```

---

**Q5. What is the exact startup flow of a Spring Boot application?**

java

```java
// 1. main() → SpringApplication.run(App.class, args)
//
// 2. SpringApplication constructor:
//    - Detect WebApplicationType (SERVLET / REACTIVE / NONE)
//    - Load SpringApplicationRunListeners from spring.factories
//
// 3. prepareEnvironment()
//    - Load application.properties, yml
//    - Merge OS env, JVM args, command line args
//
// 4. createApplicationContext()
//    - AnnotationConfigServletWebServerApplicationContext (for web)
//
// 5. prepareContext()
//    - Register primary source (@SpringBootApplication class)
//    - Apply ApplicationContextInitializers
//
// 6. refreshContext()  ← MOST IMPORTANT
//    - @ComponentScan → discovers your beans
//    - AutoConfiguration → conditionally wires infra beans
//    - Singleton beans instantiated eagerly
//    - Embedded Tomcat started inside onRefresh()
//
// 7. afterRefresh()
//    - Runs CommandLineRunner, ApplicationRunner beans
//
// 8. ApplicationReadyEvent fired → app is LIVE
```

**Follow-ups:**

- _Where does Tomcat start?_ → Inside `refreshContext()` → `onRefresh()` → `createWebServer()` on `TomcatServletWebServerFactory`
- _StartedEvent vs ReadyEvent?_ → StartedEvent fires after context refresh, before runners. ReadyEvent fires after all runners finish.

---

**Q7. How does Spring Boot detect embedded Tomcat automatically?**

java

```java
@Configuration
@ConditionalOnClass({ Servlet.class, Tomcat.class, UpgradeProtocol.class })
@ConditionalOnMissingBean(value = ServletWebServerFactory.class)
static class EmbeddedTomcat {
    @Bean
    TomcatServletWebServerFactory tomcatServletWebServerFactory() {
        return new TomcatServletWebServerFactory();
    }
}

// Customize:
@Bean
public WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer() {
    return factory -> {
        factory.setPort(9090);
        factory.addConnectorCustomizers(c ->
            c.setMaxPostSize(10 * 1024 * 1024));
    };
}
```

---

**Q10. What is the role of SpringFactoriesLoader?**

java

```java
// Reads META-INF/spring.factories from ALL JARs on classpath:
// org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
//   WebMvcAutoConfiguration,DataSourceAutoConfiguration,...

List<String> configs = SpringFactoriesLoader.loadFactoryNames(
    EnableAutoConfiguration.class, classLoader
);

// Spring Boot 3.x: auto-configs moved to:
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
// (one class per line)

// Writing your own library auto-config:
// 1. Create @Configuration class with @Conditional annotations
// 2. Register in META-INF/spring/...AutoConfiguration.imports
// 3. Any project adding your JAR gets auto-configured!
```

---

**Q12. Difference between @RestController and @Controller internally?**

`@RestController` = `@Controller` + `@ResponseBody`. The `@ResponseBody` tells Spring to write the return value directly to the HTTP response body via `HttpMessageConverter` (Jackson), instead of treating it as a view name.

java

```java
// @Controller — return = view name
@Controller
public class WebController {
    @GetMapping("/home")
    public String home(Model model) {
        return "home"; // → resolves templates/home.html
    }
}

// @RestController — return = JSON body directly
@RestController
public class UserController {
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id); // → serialized to JSON by Jackson
    }
}

// Internal flow:
// HandlerAdapter gets User object
// Detects @ResponseBody
// Iterates HttpMessageConverters
// MappingJackson2HttpMessageConverter writes JSON to response
```

**Follow-up:** _How does Spring pick the right converter?_ → Matches `Accept` header (client) to `Content-Type` the converter supports. Jackson handles `application/json`.

---

**Q28. What happens internally when you hit a REST endpoint?**

java

```java
// Complete flow:
// 1. HTTP request → Tomcat connector thread
// 2. DispatcherServlet.doDispatch()
//    a. RequestMappingHandlerMapping finds @GetMapping("/users/{id}") handler
//    b. HandlerInterceptor.preHandle() — auth checks, logging
//    c. RequestMappingHandlerAdapter.handle()
//       - Argument resolvers: @PathVariable, @RequestBody (Jackson deserialization)
//       - Your controller method executes
//    d. Return value handler detects @ResponseBody
//    e. MappingJackson2HttpMessageConverter serializes → JSON
//    f. HandlerInterceptor.postHandle(), afterCompletion()
// 3. Response flushed to client

// Custom interceptor example:
@Component
public class TimingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req,
                             HttpServletResponse res, Object handler) {
        req.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req,
                                HttpServletResponse res,
                                Object handler, Exception ex) {
        long duration = System.currentTimeMillis() -
                        (Long) req.getAttribute("startTime");
        log.info("{} {} → {}ms", req.getMethod(), req.getRequestURI(), duration);
    }
}
```

**Follow-up:** _Filter vs Interceptor?_ → Filter is Servlet-level (before DispatcherServlet), runs for all resources. Interceptor is Spring MVC-level, only for `@Controller` requests.

---

### 🔧 CONFIG

---

**Q4. How does Spring Boot load application.properties internally?**

java

```java
// ConfigDataEnvironmentPostProcessor hooks in early during startup
// Searches these locations in order:
// 1. classpath:/
// 2. classpath:/config/
// 3. file:./
// 4. file:./config/

// 3 ways to access config:

// @Value — single property
@Value("${app.timeout:5000}") // 5000 = default
private int timeout;

// @ConfigurationProperties — grouped (PREFERRED)
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private int timeout;
    private String name;
    // getters + setters
}

// Environment — programmatic
@Autowired Environment env;
env.getProperty("app.timeout", Integer.class, 5000);
```

**Follow-ups:**

- _Env var vs properties — who wins?_ → Env var wins (higher priority in PropertySource chain)
- _@Value vs @ConfigurationProperties?_ → `@ConfigurationProperties` supports relaxed binding, type safety, `@Validated`, and group binding. Prefer it for anything beyond single values.

---

**Q6. Difference between @ComponentScan and @SpringBootApplication?**

java

```java
// @SpringBootApplication is a meta-annotation = 3 in 1:
@SpringBootConfiguration     // = @Configuration
@EnableAutoConfiguration     // loads auto-configs from spring.factories
@ComponentScan               // scans package of the annotated class
public @interface SpringBootApplication {}

// @ComponentScan alone — ONLY finds @Component beans. Does NOT enable auto-configuration.

// Common mistake — main class in wrong package:
// com.example.App  → scans com.example.*
// com.other.MyService → NOT found!

// Fix:
@SpringBootApplication(scanBasePackages = {"com.example", "com.other"})
public class App {}
```

---

**Q9. How does Spring Boot load profile-specific configurations?**

java

```java
// application-dev.properties — loaded when dev profile active
spring.datasource.url=jdbc:mysql://localhost/devdb
logging.level.root=DEBUG

// application-prod.properties
spring.datasource.url=jdbc:mysql://prod-server/proddb
logging.level.root=WARN

// Activate:
spring.profiles.active=dev     // in application.properties
// java -jar app.jar --spring.profiles.active=prod
// SPRING_PROFILES_ACTIVE=prod  (env var)

// Profile-specific @Bean:
@Configuration
@Profile("prod")
public class ProdConfig {
    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://prod/db");
        return ds;
    }
}

// Multiple active profiles:
spring.profiles.active=dev,metrics
```

**Follow-up:** _include vs active?_ → `spring.profiles.include` always activates those profiles regardless of which profile is active — useful for always-on cross-cutting profiles like `logging`.

---

**Q13. How does Spring Boot manage dependency versions automatically?**

xml

```xml
<!-- Inherit from parent BOM — only one version to manage -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<!-- No version needed — inherited -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- Override specific version: -->
<properties>
    <jackson.version>2.16.0</jackson.version>
</properties>

<!-- Corporate parent? Use BOM import instead: -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

**Q15 & Q16. Externalized config + yml vs properties conflict**

java

```java
// Priority (highest → lowest):
// 1. CLI args:   java -jar app.jar --server.port=9090
// 2. OS env var: SERVER_PORT=9090  (relaxed binding)
// 3. application-{profile}.properties
// 4. application.properties / yml
// 5. Default values

// Both files present — properties WINS over yml for same keys
// Unique keys from yml still load

// Relaxed binding — all equivalent:
// application.properties:  spring.datasource.url
// Env variable:             SPRING_DATASOURCE_URL
// System property:          spring.datasource.url

// YAML multi-doc (profiles in one file):
server:
  port: 8080
---
spring:
  config:
    activate:
      on-profile: dev
server:
  port: 9090
```

---

**Q18. Difference between @Configuration class and normal class?**

java

```java
// @Configuration → CGLIB proxied at runtime!
@Configuration
public class AppConfig {
    @Bean
    public Service service() {
        return new Service(repository()); // calls method below
    }

    @Bean
    public Repository repository() {
        return new Repository();
    }
}
// CGLIB intercepts repository() call inside service() 
// → returns existing singleton, NOT a new instance!
// service and context share the SAME Repository bean.

// @Component (no CGLIB):
@Component  // NOT @Configuration
public class AppComponent {
    @Bean
    public Service service() {
        return new Service(repository()); // raw Java call!
    }
    @Bean
    public Repository repository() {
        return new Repository(); // NEW object each call!
    }
}
// PROBLEM: service has a different Repository than Spring context!

// Lite mode (faster startup, no inter-bean calls):
@Configuration(proxyBeanMethods = false)
```

---

**Q22 & Q23. @EnableAutoConfiguration vs @Import + exclusion**

java

```java
// @Import — static, explicit:
@Configuration
@Import({SecurityConfig.class, CacheConfig.class})
public class AppConfig {}

// @Import with ImportSelector — dynamic:
public class MySelector implements ImportSelector {
    public String[] selectImports(AnnotationMetadata meta) {
        return new String[]{"com.example.FeatureAConfig"};
    }
}

// @EnableAutoConfiguration uses @Import internally:
@Import(AutoConfigurationImportSelector.class) // reads spring.factories
public @interface EnableAutoConfiguration {}

// Exclusion — class never instantiated:
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
public class App {}

// Or via property (useful when class may not be on classpath):
// spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

**Q27. How does Spring Boot decide server port priority?**

java

```java
// Priority (highest → lowest):
// 1. --server.port=9090 (CLI arg)
// 2. -Dserver.port=9090 (JVM property)
// 3. SERVER_PORT=9090 (env var)
// 4. application-{profile}.properties
// 5. application.properties
// 6. Default: 8080

// Random port (avoid conflicts in tests):
server.port=0

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MyTest {
    @LocalServerPort private int port;
}

// Separate management port (security best practice):
server.port=8080           // public traffic
management.server.port=8081 // internal only — block at firewall
```

---

### 🔄 LIFECYCLE

---

**Q8. What happens if two beans of same type exist without @Qualifier?**

java

```java
// Spring throws NoUniqueBeanDefinitionException at startup!

// Fix 1: @Qualifier
@Autowired
@Qualifier("emailNotifier")
private Notifier notifier;

// Fix 2: @Primary — default when no qualifier
@Component @Primary
public class EmailNotifier implements Notifier {}

// Fix 3: Inject ALL of them
@Autowired private List<Notifier> notifiers; // all beans of type

// Fix 4: Map injection
@Autowired private Map<String, Notifier> notifierMap;
// {"emailNotifier": ..., "smsNotifier": ...}
```

**Follow-up:** _@Primary vs @Qualifier — who wins?_ → `@Qualifier` always wins. `@Primary` is only the default when no qualifier is specified.

---

**Q14. Complete lifecycle of a Spring Bean**

java

```java
@Component
public class MyBean implements InitializingBean, DisposableBean {

    // 1. Constructor
    public MyBean() { System.out.println("1. Instantiated"); }

    // 2. Dependency injection (@Autowired fields set)

    // 3. BeanNameAware.setBeanName()
    // 4. ApplicationContextAware.setApplicationContext()

    // 5. BeanPostProcessor.postProcessBeforeInitialization()
    //    (AOP proxies begin here)

    // 6. @PostConstruct — USE THIS for init logic
    @PostConstruct
    public void init() { System.out.println("6. Ready"); }

    // 7. InitializingBean.afterPropertiesSet()
    @Override public void afterPropertiesSet() {}

    // 8. BeanPostProcessor.postProcessAfterInitialization()
    //    (AOP proxies finalized — bean fully ready)

    // ---- IN USE ----

    // 9. @PreDestroy — USE THIS for cleanup
    @PreDestroy
    public void cleanup() { System.out.println("9. Destroying"); }

    // 10. DisposableBean.destroy()
    @Override public void destroy() {}
}
```

**Follow-ups:**

- _Prototype bean destruction?_ → Spring doesn't manage it. You own cleanup for prototype beans.
- _@PostConstruct vs InitializingBean?_ → Always prefer `@PostConstruct` — it's JSR-250, framework-agnostic.

---

**Q20. What is the real use of CommandLineRunner?**

java

```java
// Runs AFTER full context init, before app becomes publicly available
@Component
@Order(1)
public class SeedDataRunner implements CommandLineRunner {
    @Autowired UserRepository repo;

    @Override
    public void run(String... args) throws Exception {
        if (repo.count() == 0) {
            repo.save(new User("admin", "admin@example.com"));
        }
    }
}

// ApplicationRunner — typed args
@Component
@Order(2)
public class CacheRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean debug = args.containsOption("debug");
        cacheService.warmup();
    }
}

// Real uses: seed data, Kafka topic creation, cache warmup,
// downstream health checks, migration scripts
```

**Follow-up:** _@PostConstruct vs CommandLineRunner?_ → `@PostConstruct` runs during bean init (no `@Transactional`, partial context). `CommandLineRunner` runs after full context — safe for DB calls, external services.

---

**Q21. How does Spring Boot handle exception translation?**

java

```java
// Level 1: Controller-scoped @ExceptionHandler
@RestController
public class UserController {
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handle(UserNotFoundException ex) {
        return new ErrorResponse(404, ex.getMessage());
    }
}

// Level 2: Global @ControllerAdvice (PREFERRED)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(UserNotFoundException ex) {
        return new ErrorResponse(404, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
            .stream().map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.toList());
        return new ErrorResponse(400, "Validation failed", errors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex) {
        log.error("Unhandled", ex);
        return new ErrorResponse(500, "Internal error");
    }
}

// DB exception translation:
// @Repository beans → Spring wraps SQLException
// → DataIntegrityViolationException, EmptyResultDataAccessException, etc.
```

---

### ☁️ MICROSERVICES

---

**Q24. Why is Spring Boot perfect for microservices?**

java

````java
// 1. Self-contained fat JAR — runs anywhere
java -jar user-service.jar  // no server install

// 2. Kubernetes health probes:
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
management.endpoint.health.probes.enabled=true
// /actuator/health/liveness  → is JVM alive?
// /actuator/health/readiness → ready to serve?

// 3. Graceful shutdown:
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s

// 4. Service discovery (Spring Cloud Eureka):
@SpringBootApplication
@EnableDiscoveryClient
public class UserService {}

// 5. Feign client — declarative HTTP:
@FeignClient(name = "order-service")
public interface OrderClient {
    @GetMapping("/orders/{userId}")
    List<Order> getOrdersByUser(@PathVariable Long userId);
}

// 6. Circuit breaker (Resilience4j):
@CircuitBreaker(name = "orderService", fallbackMethod = "fallback")
public List<Order> getOrders(Long userId) {
    return orderClient.getOrdersByUser(userId);
}

public List<Order> fallback(Long userId, Exception ex) {
    return Collections.emptyList();
}
```

---

**Q25. Fat JAR vs Normal JAR**
```
// Fat JAR structure:
myapp.jar
├── BOOT-INF/
│   ├── classes/          ← your compiled code
│   └── lib/              ← ALL 100+ dependency JARs
├── META-INF/
│   └── MANIFEST.MF       ← Main-Class: JarLauncher
└── org/springframework/boot/loader/
    └── JarLauncher.class  ← custom classloader for nested JARs
````

java

```java
// MANIFEST.MF:
// Main-Class: org.springframework.boot.loader.JarLauncher
// Start-Class: com.example.App

// Normal JAR — you manage classpath:
java -cp myapp.jar:lib/* com.example.App

// Fat JAR — zero setup:
java -jar myapp.jar

// Docker optimization — layered JARs:
// Layer 1: dependencies (cache-stable, rarely change)
// Layer 2: snapshot-dependencies
// Layer 3: resources
// Layer 4: application classes (changes every build)
// → Only layer 4 rebuilds on code change = fast Docker builds
```

---

**Q29. Why is Spring Boot preferred for cloud-native apps?**

java

```java
// GraalVM Native Image (Spring Boot 3.x):
// mvn -Pnative native:compile
// → Native binary: 50ms startup vs 3s JVM, 10x less memory

// Reactive stack for high concurrency (WebFlux):
@RestController
public class StreamController {
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Order> stream() {
        return orderRepo.findAll()
            .delayElements(Duration.ofMillis(100));
    }
}

// Distributed tracing (Micrometer):
// Auto-adds trace-id, span-id to logs and HTTP headers
// Works with Zipkin, Jaeger, OpenTelemetry — zero config

// Config server (externalize all config):
spring.config.import=configserver:http://config-server:8888
```

---

### ⚡ PERFORMANCE

---

**Q30. Most common Spring Boot performance mistakes**

java

```java
// MISTAKE 1: N+1 Query Problem
// BAD:
List<Order> orders = orderRepo.findAll(); // 1 query
for (Order o : orders) {
    o.getUser().getName(); // N lazy queries!
}

// FIX — JOIN FETCH:
@Query("SELECT o FROM Order o JOIN FETCH o.user")
List<Order> findAllWithUser();

// Or @EntityGraph:
@EntityGraph(attributePaths = {"user", "items"})
List<Order> findAll();

// MISTAKE 2: @Transactional without readOnly on reads
@Transactional(readOnly = true) // skips dirty checking, faster
public List<User> getUsers() { return repo.findAll(); }

// MISTAKE 3: HikariCP pool too small (default = 10)
spring.datasource.hikari.maximum-pool-size=20

// MISTAKE 4: No pagination on large tables
// BAD:
List<Order> all = repo.findAll(); // could be millions!

// GOOD:
Page<Order> page = repo.findAll(PageRequest.of(0, 20, Sort.by("id")));

// MISTAKE 5: No caching on repeated reads
@Cacheable(value = "products", key = "#id")
public Product getProduct(Long id) { return repo.findById(id).get(); }
// Add: @EnableCaching + spring-boot-starter-cache + spring-boot-starter-data-redis

// MISTAKE 6: Blocking HTTP calls with RestTemplate at scale
// BAD:
ResponseEntity<User> u = restTemplate.getForEntity(url, User.class);

// GOOD — non-blocking WebClient:
Mono<User> u = webClient.get().uri(url).retrieve().bodyToMono(User.class);

// MISTAKE 7: Slow startup from too many eager beans
spring.main.lazy-initialization=true // defer non-critical beans

// MISTAKE 8: LazyInitializationException
// BAD: accessing lazy relation after transaction closes
// FIX: use JOIN FETCH, DTO projections, or @Transactional on service
```

**Follow-ups:**

- _Find N+1 in prod?_ → `spring.jpa.properties.hibernate.generate_statistics=true` + `logging.level.org.hibernate.stat=DEBUG`
- _Fastest startup improvement?_ → Layered JARs for Docker caching, `lazy-initialization=true`, GraalVM native image (50ms startup)

---

### Quick Reference Table

|#|Topic|Key Class / Annotation|One-liner|
|---|---|---|---|
|1|Auto-config selection|`@ConditionalOnClass`|Classpath drives what applies|
|5|Startup flow|`SpringApplication.run()`|7 phases, Tomcat inside refreshContext|
|10|Factories loader|`SpringFactoriesLoader`|SPI mechanism for spring.factories|
|14|Bean lifecycle|`@PostConstruct / @PreDestroy`|10 phases, use annotations not interfaces|
|18|@Configuration|CGLIB proxy|Inter-bean calls return singletons|
|19|DataSource|`HikariCP`|Auto-created from properties|
|28|Request flow|`DispatcherServlet`|Mapping → Interceptor → Adapter → Converter|
|30|Performance|N+1, HikariCP, cache|JOIN FETCH + readOnly + @Cacheable|

This covers all 30 with full depth. Let me know which area you want to drill deeper on — I can write mock interview Q&A for any section or add system design context.

  

Sonnet 4.6