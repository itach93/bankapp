1. **Project Setup**

First, create a new Spring Boot project using Spring Initializer (https://start.spring.io/) with these dependencies:
- Spring Web
- Spring Data JPA
- MySQL Driver
- Spring Security
- Lombok
- JWT (you'll need to add this manually)

2. **Project Structure**
```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── example/
│   │           └── banking/
│   │               ├── BankingApplication.java
│   │               ├── config/
│   │               │   ├── SecurityConfig.java
│   │               │   └── JwtConfig.java
│   │               ├── controller/
│   │               │   ├── AuthController.java
│   │               │   ├── UserController.java
│   │               │   └── AccountController.java
│   │               ├── model/
│   │               │   ├── User.java
│   │               │   ├── Account.java
│   │               │   └── AccountingJournal.java
│   │               ├── repository/
│   │               │   ├── UserRepository.java
│   │               │   ├── AccountRepository.java
│   │               │   └── AccountingJournalRepository.java
│   │               ├── service/
│   │               │   ├── UserService.java
│   │               │   └── AccountService.java
│   │               └── dto/
│   │                   ├── LoginRequest.java
│   │                   ├── RegisterRequest.java
│   │                   └── TransactionRequest.java
│   └── resources/
│       └── application.yml
```

3. **Docker Setup**

Create a `docker-compose.yml` file in the root directory:
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: banking_db
      MYSQL_USER: user
      MYSQL_PASSWORD: password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/banking_db
      SPRING_DATASOURCE_USERNAME: user
      SPRING_DATASOURCE_PASSWORD: password

volumes:
  mysql_data:
```

Create a `Dockerfile`:
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

4. **Application Configuration**

`application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/banking_db
    username: user
    password: password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: your-secret-key
  expiration: 86400000
```

5. **Entity Classes**

`User.java`:
```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String email;
    
    @OneToMany(mappedBy = "user")
    private List<Account> accounts;
}
```
`Account.java` (continued):
```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @OneToMany(mappedBy = "account")
    private List<AccountingJournal> transactions;
}
```

`AccountingJournal.java`:
```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountingJournal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private TransactionType type; // CREDIT or DEBIT
    
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
}
```

6. **DTOs**

`LoginRequest.java`:
```java
@Data
public class LoginRequest {
    private String username;
    private String password;
}
```

`RegisterRequest.java`:
```java
@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
}
```

`TransactionRequest.java`:
```java
@Data
public class TransactionRequest {
    private String accountNumber;
    private BigDecimal amount;
}
```

7. **Security Configuration**

`SecurityConfig.java`:
```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests()
            .antMatchers("/api/register", "/api/login").permitAll()
            .anyRequest().authenticated()
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

8. **Controllers**

`AuthController.java`:
```java
@RestController
@RequestMapping("/api")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Authentication auth = userService.authenticate(request);
        String token = jwtUtil.generateToken(auth);
        return ResponseEntity.ok(new HashMap<String, String>() {{
            put("token", token);
        }});
    }
}
```

`AccountController.java`:
```java
@RestController
@RequestMapping("/api/account")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    @PostMapping("/credit")
    public ResponseEntity<?> credit(@RequestBody TransactionRequest request) {
        accountService.credit(request);
        return ResponseEntity.ok("Credit successful");
    }
    
    @PostMapping("/debit")
    public ResponseEntity<?> debit(@RequestBody TransactionRequest request) {
        accountService.debit(request);
        return ResponseEntity.ok("Debit successful");
    }
}
```
9. **Services**
`UserService.java`:
```java
@Service
@Slf4j
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    public void register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        
        userRepository.save(user);
    }
    
    public Authentication authenticate(LoginRequest request) {
        return authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );
    }
}
```

`AccountService.java`:
```java
@Service
@Slf4j
public class AccountService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private AccountingJournalRepository journalRepository;
    
    @Transactional
    public void credit(TransactionRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
            .orElseThrow(() -> new RuntimeException("Account not found"));
            
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);
        
        AccountingJournal journal = new AccountingJournal();
        journal.setAccount(account);
        journal.setAmount(request.getAmount());
        journal.setTransactionDate(LocalDateTime.now());
        journal.setType(TransactionType.CREDIT);
        journalRepository.save(journal);
    }
    
    @Transactional
    public void debit(TransactionRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
            .orElseThrow(() -> new RuntimeException("Account not found"));
            
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);
        
        AccountingJournal journal = new AccountingJournal();
        journal.setAccount(account);
        journal.setAmount(request.getAmount());
        journal.setTransactionDate(LocalDateTime.now());
        journal.setType(TransactionType.DEBIT);
        journalRepository.save(journal);
    }
}
```

10. **Repositories**

`UserRepository.java`:
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

`AccountRepository.java`:
```java
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
}
```

`AccountingJournalRepository.java`:
```java
@Repository
public interface AccountingJournalRepository extends JpaRepository<AccountingJournal, Long> {
    List<AccountingJournal> findByAccountOrderByTransactionDateDesc(Account account);
}
```

11. **JWT Implementation**

`JwtUtil.java`:
```java
@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return Jwts.builder()
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact();
    }
    
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

`JwtAuthenticationFilter.java`:
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) 
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                        
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

12. **Custom UserDetailsService**

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .authorities("USER")
            .build();
    }
}
```

13. **Exception Handling**

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new HashMap<String, String>() {{
                put("error", ex.getMessage());
            }});
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new HashMap<String, String>() {{
                put("error", "An internal server error occurred");
            }});
    }
}
```

14. **Build and Run**

1. Build the application using Maven:
```bash
./mvnw clean package -DskipTests
```

2. Start the containers using Docker Compose:
```bash
docker-compose up --build
```

15. **Testing the API**

Here's how to test each endpoint using cURL:

1. Register a new user:
```bash
curl -X POST http://localhost:8080/api/register \
-H "Content-Type: application/json" \
-d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
}'
```

2. Login:
```bash
curl -X POST http://localhost:8080/api/login \
-H "Content-Type: application/json" \
-d '{
    "username": "testuser",
    "password": "password123"
}'
```

3. Get user details (using the token received from login):
```bash
curl -X GET http://localhost:8080/api/user \
-H "Authorization: Bearer YOUR_TOKEN_HERE"
```

4. Credit account:
```bash
curl -X POST http://localhost:8080/api/account/credit \
-H "Authorization: Bearer YOUR_TOKEN_HERE" \
-H "Content-Type: application/json" \
-d '{
    "accountNumber": "123456789",
    "amount": 1000.00
}'
```

5. Debit account:
```bash
curl -X POST http://localhost:8080/api/account/debit \
-H "Authorization: Bearer YOUR_TOKEN_HERE" \
-H "Content-Type: application/json" \
-d '{
    "accountNumber": "123456789",
    "amount": 500.00
}'
```

16. **Additional Configuration**

Add these dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- JWT Dependencies -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

17. **Logging Configuration**

Add this to `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.example.banking: DEBUG
    org.springframework.security: DEBUG
```

18. **Development Tips**

1. Use VSCode extensions:
   - Spring Boot Extension Pack
   - Java Extension Pack
   - Docker
   - REST Client

2. Create a `.vscode/launch.json` for debugging:
```json
{
    "configurations": [
        {
            "type": "java",
            "name": "Spring Boot-BankingApplication",
            "request": "launch",
            "cwd": "${workspaceFolder}",
            "mainClass": "com.example.banking.BankingApplication",
            "projectName": "banking",
            "args": "",
            "envFile": "${workspaceFolder}/.env"
        }
    ]
}
```

3. Create a `.env` file for local development:
```
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/banking_db
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000
```