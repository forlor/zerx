# Zerx Spring Security Module Design

## 1. Module Overview

Zerx Spring Security is a stateless JWT authentication and RBAC authorization framework built on Spring Security. It provides out-of-the-box token management, role-based access control, key rotation, token blacklisting, and granular error handling for REST API applications.

### Design Principles

| Principle | Implementation |
|-----------|---------------|
| Stateless | No server-side session; all authentication state carried in JWT |
| API/Impl Separation | Business layer can depend on the lightweight API module without pulling in Spring Security |
| SPI Extensibility | Four optional SPI interfaces allow business-layer customization without modifying framework code |
| Zero-Configuration Defaults | Sensible defaults for every property; works out-of-the-box with a single `secret` |
| Gradual Upgrade | HS256 (simple) to RS256 (microservice-grade) migration via a single config change |

---

## 2. Module Structure

```
zerx-spring/
├── zerx-spring-security-api/          # Pure interfaces — zero Spring Security dependency
│   ├── pom.xml
│   └── src/main/java/com/zerx/spring/security/
│       ├── props/
│       │   └── ZerxSecurityProperties    @ConfigurationProperties
│       ├── token/
│       │   ├── ZerxTokenService          Core token lifecycle interface
│       │   ├── ZerxTokenClaims           Parsed JWT claims (record)
│       │   ├── ZerxTokenPair             Login response DTO (record)
│       │   ├── ZerxRoleService           SPI: role provider
│       │   └── ZerxRefreshTokenService   SPI: token rotation
│       └── service/
│           ├── ZerxPasswordValidator     SPI: password policy
│           └── ZerxLoginAttemptService   SPI: brute-force guard
│
└── zerx-spring-security/              # Implementation — Spring Security + jjwt
    ├── pom.xml
    └── src/main/java/com/zerx/spring/security/
        ├── autoconfigure/
        │   └── ZerxSecurityAutoConfiguration   Auto-config + bean registration
        ├── config/
        │   └── ZerxSecurityConfiguration        SecurityFilterChain DSL
        ├── filter/
        │   └── ZerxJwtAuthenticationFilter       JWT authentication filter
        ├── handler/
        │   ├── ZerxAuthenticationEntryPoint     401 handler
        │   └── ZerxAccessDeniedHandler           403 handler
        ├── service/
        │   └── ZerxPasswordService               BCrypt password service
        ├── token/
        │   ├── ZerxHs256TokenService             HMAC-SHA256 implementation
        │   └── ZerxRs256TokenService             RSA implementation
        └── util/
            └── ZerxSecurityUtils                  Static security utilities
```

### Dependency Graph

```
zerx-spring-security-api
  └── spring-boot (for @ConfigurationProperties only)

zerx-spring-security
  ├── zerx-spring-security-api
  ├── zerx-spring-web        (RequestContext, unified Result response)
  ├── zerx-spring-cache      (CacheOps for token blacklist)
  ├── spring-boot-starter-security
  └── io.jsonwebtoken:jjwt   (jwt-api, jwt-impl, jwt-jackson)
```

The API module has zero Spring Security dependency. Business code that only needs type references (interfaces, records, properties) can depend on `zerx-spring-security-api` alone.

---

## 3. Architecture

### 3.1 Request Authentication Flow

```
HTTP Request
    │
    ▼
┌──────────────────────────────────────┐
│  SecurityFilterChain                  │
│  ┌──────────────────────────────────┐ │
│  │  1. CORS  → delegate to web      │ │
│  │  2. CSRF  → disabled             │ │
│  │  3. Security Headers             │ │
│  │     (XSS/HSTS/CSP/Frame/Referrer)│ │
│  └──────────────────────────────────┘ │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  ZerxJwtAuthenticationFilter         │
│                                      │
│  1. Extract Bearer token              │
│  2. parseToken() → ZerxTokenClaims   │
│  3. isBlacklisted(jti)?  → reject    │
│  4. tokenType == "access"? → reject  │
│  5. Load roles:                      │
│     ├─ ZerxRoleService (SPI)         │
│     └─ fallback → JWT claims.roles   │
│  6. Set SecurityContextHolder        │
│  7. Set RequestContext.setUserId     │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  URL Authorization Rules             │
│                                      │
│  permitUrls → permitAll              │
│  roleRules  → hasRole(role)          │
│  other      → authenticated          │
│                                      │
│  401 → ZerxAuthenticationEntryPoint  │
│  403 → ZerxAccessDeniedHandler       │
└──────────────────────────────────────┘
```

### 3.2 Token Lifecycle

```
Login Request
    │
    ▼
┌──────────────────────────────────┐
│ Business Controller               │
│   ├─ ZerxLoginAttemptService     │
│   │    .isLocked(username)       │
│   ├─ ZerxPasswordService         │
│   │    .matches(raw, encoded)    │
│   ├─ ZerxPasswordValidator       │
│   │    .validate(raw, username)  │
│   ├─ ZerxTokenService            │
│   │    .generateTokenPair(...)   │
│   └─ return ZerxTokenPair        │
└──────────────────────────────────┘

Access Token (default 2h)     Refresh Token (default 7d)
    │                              │
    │  → every API request          │  → /auth/refresh endpoint
    │  → filter validates           │  → ZerxRefreshTokenService.rotate()
    │  → blacklist on logout        │     returns new ZerxTokenPair
    │                              │  → blacklist old refresh token
    │                              │
    ▼                              ▼
 blacklistToken(jti, expiresAt)  revoke(jti) / revokeAll(userId)
    │                              │
    ▼                              ▼
 CacheOps.set(key, ttl)          Business storage (Redis/DB)
```

### 3.3 Error Response Flow

```
Authentication Failure
    │
    ▼
┌──────────────────────────────────────────┐
│ Filter catches exception                  │
│   ├─ ExpiredJwtException    → TOKEN_EXPIRED     │
│   ├─ JwtException           → TOKEN_INVALID     │
│   ├─ isBlacklisted          → TOKEN_BLACKLISTED │
│   └─ tokenType != "access"  → TOKEN_INVALID     │
│                                          │
│ Sets request attribute:                  │
│   ATTR_AUTH_ERROR = error_code           │
└──────────────┬───────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│ ZerxAuthenticationEntryPoint.commence()   │
│                                          │
│ Reads ATTR_AUTH_ERROR from request       │
│ Maps to human-readable message           │
│ Returns JSON: Result.fail(code, message) │
└──────────────────────────────────────────┘
```

---

## 4. Core Components

### 4.1 ZerxSecurityProperties

Configuration root: `zerx.security`

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `enabled` | boolean | `true` | Master switch. Module is fully disabled when `false` |
| `permit-urls` | `List<String>` | `[/auth/login, /auth/register, /doc.html]` | Whitelist URLs that bypass authentication |
| `role-rules` | `List<RoleRule>` | `[]` (auto-adds `/actuator/** → ADMIN`) | URL pattern to required role mapping |
| `jwt.algorithm` | String | `HS256` | Token signing algorithm: `HS256` or `RS256` |
| `jwt.secret` | String | — | HS256 signing key (required for HS256, min 256 bits) |
| `jwt.previous-secret` | String | — | Previous HS256 key for seamless rotation |
| `jwt.access-token-expire` | Duration | `2h` | Access token time-to-live |
| `jwt.refresh-token-expire` | Duration | `7d` | Refresh token time-to-live |
| `jwt.issuer` | String | `zerx` | JWT `iss` claim |
| `jwt.header-name` | String | `Authorization` | HTTP header name for token |
| `jwt.header-prefix` | String | `Bearer ` | Token prefix in header value |
| `jwt.kid` | String | `default` | Key identifier written to JWT header |
| `jwt.rsa.public-key` | String | — | RSA public key (classpath/file/base64) |
| `jwt.rsa.private-key` | String | — | RSA private key (classpath/file/base64) |
| `jwt.rsa.previous-public-key` | String | — | Previous RSA public key for rotation |

#### Configuration Example

```yaml
zerx:
  security:
    enabled: true
    permit-urls:
      - /auth/login
      - /auth/register
      - /doc.html
    role-rules:
      - path: /api/admin/**
        role: ADMIN
      - path: /api/vip/**
        role: VIP
    jwt:
      algorithm: HS256
      secret: "your-256-bit-secret-key-here"
      previous-secret: "old-secret-for-rotation"
      access-token-expire: 2h
      refresh-token-expire: 7d
      issuer: zerx
      header-name: Authorization
      header-prefix: "Bearer "
```

RSA configuration example:

```yaml
zerx:
  security:
    jwt:
      algorithm: RS256
      rsa:
        public-key: classpath:keys/pub.pem
        private-key: classpath:keys/pri.pem
        previous-public-key: classpath:keys/old-pub.pem
```

### 4.2 ZerxTokenService

Core interface for JWT token lifecycle management.

```java
public interface ZerxTokenService {
    // Generate
    String generateAccessToken(Long userId, String jti, List<String> roles);
    String generateAccessToken(Long userId, String jti);  // backward-compatible
    String generateRefreshToken(Long userId, String jti);

    // Parse
    ZerxTokenClaims parseToken(String token);

    // Validate (signature + expiry + blacklist)
    boolean validateToken(String token);

    // Blacklist
    void blacklistToken(String jti, Instant expiresAt);
    boolean isBlacklisted(String jti);
}
```

#### Implementations

| Class | Algorithm | Key Size | Use Case |
|-------|-----------|----------|----------|
| `ZerxHs256TokenService` | HMAC-SHA256 | 256-bit symmetric | Monolith, single-service |
| `ZerxRs256TokenService` | RSA-SHA256 | 2048-bit asymmetric | Microservices, key separation |

Both implementations also provide `generateTokenPair` convenience methods:

```java
ZerxTokenPair generateTokenPair(Long userId);
ZerxTokenPair generateTokenPair(Long userId, List<String> roles);
```

Returns a `ZerxTokenPair` record containing both tokens with their TTLs.

### 4.3 Key Rotation

Both algorithms support seamless key rotation using a previous-key fallback strategy.

**HS256 Rotation:**

```
Signing:   always uses current secret
Verifying: current secret → fallback to previous-secret
```

**RS256 Rotation:**

```
Signing:   always uses current private key
Verifying: current public key → fallback to previous-public-key
```

The JWT header `kid` field identifies which key was used during signing. During rotation:

1. Deploy new secret/key pair
2. Set `previous-secret` / `previous-public-key` to the old value
3. New tokens are signed with the current key
4. Old tokens (signed with the old key) continue to validate via fallback
5. After all old tokens expire, remove `previous-*` config

### 4.4 Token Blacklist

Token blacklisting is cache-backed with automatic TTL management.

```
blacklistToken(jti, expiresAt)
  → if already expired: skip (no cache write)
  → else: CacheOps.set("token:blacklist:" + jti, ttl = remaining lifetime)

isBlacklisted(jti)
  → CacheOps.get("token:blacklist:" + jti)
```

The TTL equals the remaining lifetime of the token, so blacklisted entries are automatically evicted when the token would have expired anyway. This prevents cache bloat and eliminates the need for manual cleanup.

### 4.5 RBAC Role Loading

Role loading follows a dual-strategy pattern with SPI priority and JWT fallback.

```
Filter authentication:
  ┌─────────────────────────────┐
  │ ZerxRoleService bean exists? │
  │   ├─ YES → getRoles(userId) │
  │   └─ NO  → claims.roles     │
  └─────────────────────────────┘
           │
           ▼
  Normalize roles:
    ├─ Add "ROLE_" prefix if missing
    ├─ Deduplicate
    └─ Filter empty strings
           │
           ▼
  Set as GrantedAuthority in SecurityContext
```

`ZerxRoleService` (SPI) takes precedence when present, allowing real-time role updates from the database without requiring token re-issuance. When no SPI bean is available, roles embedded in the JWT claims are used as fallback.

### 4.6 SecurityFilterChain Configuration

The filter chain is defined in `ZerxSecurityConfiguration`:

| Feature | Configuration |
|---------|---------------|
| CSRF | Disabled (stateless REST API) |
| CORS | Delegated to `zerx-spring-web` CorsFilter |
| Session | `STATELESS` |
| Security Headers | See Section 4.7 |

**URL Authorization Rules (evaluated in order):**

| Priority | Match | Action |
|----------|-------|--------|
| 1 | `permitUrls` + `/actuator/health` + `/actuator/info` | `permitAll()` |
| 2 | `roleRules` entries | `hasRole(role)` |
| 3 | `/actuator/**` (auto-added if not user-configured) | `hasRole("ADMIN")` |
| 4 | All other paths | `authenticated` |

**JWT Filter Registration:**

The filter is registered at position `BEFORE_USERNAME_PASSWORD_AUTHENTICATION_FILTER`, ensuring it runs before Spring Security's built-in authentication mechanisms.

### 4.7 Security Headers

| Header | Value | Purpose |
|--------|-------|---------|
| `X-Content-Type-Options` | `nosniff` | Prevents MIME type sniffing |
| `X-Frame-Options` | `DENY` | Prevents clickjacking |
| `X-XSS-Protection` | `1; mode=block` | Legacy XSS protection |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Enforces HTTPS |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self'; ...` | Controls resource loading |
| `Referrer-Policy` | `STRICT_ORIGIN_WHEN_CROSS_ORIGIN` | Limits referrer leakage |

### 4.8 Error Handlers

**ZerxAuthenticationEntryPoint (401 Unauthorized):**

| Request Attribute | Response Code | Response Message |
|-------------------|---------------|------------------|
| `token_expired` | `TOKEN_EXPIRED` | Token has expired |
| `token_blacklisted` | `TOKEN_BLACKLISTED` | Token has been revoked |
| `token_type_rejected` | `TOKEN_INVALID` | Token type not allowed |
| (other/missing) | `401` | Authentication failed |
| (no token) | `401` | Authentication required |

**ZerxAccessDeniedHandler (403 Forbidden):**

Returns `Result.fail("403", "没有访问权限")` as JSON.

### 4.9 ZerxSecurityUtils

Static utility class for accessing security context in business code.

```java
// Get current authenticated user ID
Long userId = ZerxSecurityUtils.getCurrentUserId();        // throws if not authenticated
Optional<Long> userId = ZerxSecurityUtils.getCurrentUserIdOptional();  // safe access

// Role checks
List<String> roles = ZerxSecurityUtils.getCurrentRoles();  // ["ADMIN", "USER"]
boolean isAdmin = ZerxSecurityUtils.hasRole("ADMIN");       // case-insensitive
boolean isAuthed = ZerxSecurityUtils.isAuthenticated();     // excludes anonymousUser
```

**Principal Type Handling:**

The utility handles multiple principal types transparently:
- `Long` → returned directly
- `Number` (Integer, etc.) → `longValue()`
- `String` → `Long.parseLong()`

---

## 5. SPI Interfaces

All SPI interfaces are defined in `zerx-spring-security-api` and are **optional**. Business applications implement them as Spring beans; the framework auto-detects and integrates them.

### 5.1 ZerxRoleService

Provides real-time role loading from external data sources (database, Redis, etc.).

```java
public interface ZerxRoleService {
    List<String> getRoles(Long userId);
}
```

**Integration:** Auto-detected by `ZerxJwtAuthenticationFilter`. When present, takes priority over JWT-embedded roles. When absent, JWT claims roles are used as fallback.

**Use Case:** When user roles change frequently (e.g., admin demotion), SPI ensures the next request uses the updated roles without waiting for token re-issuance.

### 5.2 ZerxRefreshTokenService

Manages refresh token rotation, single-device revocation, and full logout.

```java
public interface ZerxRefreshTokenService {
    Optional<ZerxTokenPair> rotate(String refreshToken, List<String> roles);
    void revoke(String jti);
    int revokeAll(Long userId);
}
```

**Integration:** Business controller implements the `/auth/refresh` and `/auth/logout` endpoints using this interface.

| Method | Purpose |
|--------|---------|
| `rotate` | Validate old refresh token, issue new token pair, blacklist old |
| `revoke` | Blacklist a single refresh token (single-device logout) |
| `revokeAll` | Revoke all refresh tokens for a user (logout all devices) |

### 5.3 ZerxPasswordValidator

Validates password complexity against configurable rules.

```java
public interface ZerxPasswordValidator {
    List<String> validate(String rawPassword, String username);
}
```

**Integration:** Business login/register controller calls this before hashing. Returns a list of validation error messages; empty list means the password is valid.

### 5.4 ZerxLoginAttemptService

Provides brute-force attack protection and account lockout.

```java
public interface ZerxLoginAttemptService {
    boolean isLocked(String username);
    void recordFailure(String username);
    void recordSuccess(String username);
    int getRemainingAttempts(String username);
}
```

**Integration:** Business login controller calls `isLocked` before authentication and `recordFailure`/`recordSuccess` after authentication attempt.

---

## 6. Auto-Configuration

### 6.1 Activation Conditions

The module auto-configures when all conditions are met:

| Condition | Annotation |
|-----------|------------|
| Spring Security on classpath | `@ConditionalOnClass(SecurityFilterChain.class)` |
| Property `zerx.security.enabled` is `true` | `@ConditionalOnProperty(name = "zerx.security.enabled", havingValue = "true", matchIfMissing = true)` |

### 6.2 Registered Beans

| Bean | Condition | Class |
|------|-----------|-------|
| `ZerxTokenService` | `algorithm == RS256` | `ZerxRs256TokenService` |
| `ZerxTokenService` | `algorithm != RS256` | `ZerxHs256TokenService` |
| `ZerxTokenService` | Already defined by business | `@ConditionalOnMissingBean` (skipped) |

### 6.3 Static Initialization

On class loading, the auto-configuration sets:

```java
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
```

This ensures `@Async` methods and child threads automatically inherit the `SecurityContext` from the parent thread.

### 6.4 Imports

The `AutoConfiguration.imports` file registers:

```
com.zerx.spring.security.autoconfigure.ZerxSecurityAutoConfiguration
```

Which imports `ZerxSecurityConfiguration` for SecurityFilterChain setup.

---

## 7. Security Headers

Security headers are configured in `ZerxSecurityConfiguration` using `StaticHeadersWriter`:

| Header | Value |
|--------|-------|
| X-Content-Type-Options | `nosniff` |
| X-Frame-Options | `DENY` |
| X-XSS-Protection | `1; mode=block` |
| Strict-Transport-Security | `max-age=31536000; includeSubDomains` |
| Content-Security-Policy | `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'` |
| Referrer-Policy | `STRICT_ORIGIN_WHEN_CROSS_ORIGIN` |

Note: `X-XSS-Protection` and `Content-Security-Policy` use custom `StaticHeadersWriter` instances because Spring Security deprecated the corresponding DSL methods.

---

## 8. Method-Level Security

`@EnableMethodSecurity` is declared on `ZerxSecurityConfiguration`, enabling the following annotations on any Spring bean:

```java
@PreAuthorize("hasRole('ADMIN')")
public void adminOnly() { ... }

@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public void adminOrManager() { ... }

@PreAuthorize("#userId == authentication.principal")
public void ownDataOnly(Long userId) { ... }

@Secured("ROLE_ADMIN")
public void legacyRoleCheck() { ... }
```

---

## 9. Password Service

`ZerxPasswordService` wraps Spring Security's `BCryptPasswordEncoder`:

```java
@Bean
@ConditionalOnMissingBean
public ZerxPasswordService zerxPasswordService(PasswordEncoder passwordEncoder) {
    return new ZerxPasswordService(passwordEncoder);
}

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

- BCrypt strength: **12** (industry standard balance of security and performance)
- Thread-safe (BCrypt is inherently thread-safe)
- Overridable via `@ConditionalOnMissingBean`

---

## 10. Async SecurityContext Propagation

The module sets `SecurityContextHolder` strategy to `MODE_INHERITABLETHREADLOCAL` in a static initializer block within `ZerxSecurityAutoConfiguration`. This ensures:

| Scenario | SecurityContext Inherited? |
|----------|--------------------------|
| `@Async` methods | Yes |
| `CompletableFuture` with common ForkJoinPool | Yes (if thread is a child) |
| Explicit `new Thread()` | No |
| Java 21 Virtual Threads | No (known limitation) |

---

## 11. RSA Key Loading

`ZerxRs256TokenService` supports three key format with automatic detection:

| Format | Prefix | Example |
|--------|--------|---------|
| Classpath resource | `classpath:` | `classpath:keys/pub.pem` |
| Filesystem path | `file:` | `file:/etc/ssl/pub.pem` |
| Raw Base64 DER | (no prefix) | `MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A...` |

PEM format is auto-detected: `-----BEGIN PUBLIC KEY-----` / `-----END PUBLIC KEY-----` markers are stripped before Base64 decoding.

---

## 12. Test Coverage

| Test Class | Scope | Assertions |
|------------|-------|------------|
| `ZerxSecurityPropertiesTest` | Default values + custom YAML | 19 |
| `ZerxSecurityConfigurationTest` | SecurityFilterChain integration (MockMvc) | 5+ |
| `ZerxTokenServiceTest` | HS256 full lifecycle | 20+ |
| `ZerxRs256TokenServiceTest` | RS256 full lifecycle + key loading | 25+ |
| `ZerxJwtAuthenticationFilterTest` | Token extraction, validation, RBAC, type check | 20+ |
| `ZerxAuthenticationEntryPointTest` | Error granularity (5 error types) | 5+ |
| `ZerxAccessDeniedHandlerTest` | 403 JSON response | 3+ |
| `ZerxPasswordServiceTest` | BCrypt hash/matches | 5+ |
| `ZerxSecurityUtilsTest` | userId/roles/hasRole/isAuthenticated | 10+ |

All tests use `InMemoryCacheOps` for cache dependency, avoiding external infrastructure.

---

## 13. Usage Examples

### 13.1 Minimal Configuration (HS256)

```yaml
zerx:
  security:
    jwt:
      secret: "my-256-bit-secret-key-must-be-long-enough"
```

### 13.2 Login Endpoint

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private ZerxTokenService tokenService;
    @Autowired private ZerxPasswordService passwordService;
    @Autowired private ZerxLoginAttemptService loginAttemptService;
    @Autowired private ZerxPasswordValidator passwordValidator;

    @PostMapping("/login")
    public Result<ZerxTokenPair> login(@RequestBody LoginRequest req) {
        // Brute-force check
        if (loginAttemptService.isLocked(req.getUsername())) {
            return Result.fail("ACCOUNT_LOCKED", "Account is temporarily locked");
        }

        // Authenticate
        UserDetails user = userService.loadByUsername(req.getUsername());
        if (!passwordService.matches(req.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(req.getUsername());
            return Result.fail("LOGIN_FAILED", "Invalid credentials");
        }

        // Generate tokens
        loginAttemptService.recordSuccess(req.getUsername());
        ZerxTokenPair tokenPair = tokenService.generateTokenPair(user.getId(), user.getRoles());
        return Result.success(tokenPair);
    }
}
```

### 13.3 Token Refresh Endpoint

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private ZerxRefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    public Result<ZerxTokenPair> refresh(@RequestHeader("Authorization") String refreshToken) {
        String token = refreshToken.substring(7); // strip "Bearer "
        List<String> roles = ZerxSecurityUtils.getCurrentRoles();
        return refreshTokenService.rotate(token, roles)
            .map(Result::success)
            .orElse(Result.fail("REFRESH_FAILED", "Invalid or expired refresh token"));
    }
}
```

### 13.4 Role-Based Endpoint

```java
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> adminDashboard() {
        Long userId = ZerxSecurityUtils.getCurrentUserId();
        return Result.success(adminService.getDashboard(userId));
    }
}
```

### 13.5 Implementing ZerxRoleService (SPI)

```java
@Service
public class DatabaseRoleService implements ZerxRoleService {

    @Autowired private RoleRepository roleRepository;

    @Override
    public List<String> getRoles(Long userId) {
        return roleRepository.findRoleCodesByUserId(userId);
    }
}
```

---

## 14. Design Decisions

| Decision | Rationale |
|----------|-----------|
| API/Impl module split | Business layer can depend on API only, avoiding Spring Security transitive dependency |
| Dual algorithm (HS256 + RS256) | Covers both monolith (HS256, simple) and microservice (RS256, key separation) scenarios |
| Previous-key rotation over versioned keys | Simpler configuration; only need to maintain one extra key during rotation |
| Cache-backed blacklist with auto-TTL | No manual cleanup; entries expire when the token would have expired |
| SPI over abstract class | Business layer implements interfaces; no framework class hierarchy constraints |
| Token type claim in JWT | Prevents refresh token reuse as access token at the filter level |
| Request attributes for error codes | Decouples filter (sets attribute) from EntryPoint (reads attribute); avoids tight coupling |
| `@ConditionalOnProperty` with `matchIfMissing=true` | Module works out-of-the-box; explicit `false` to disable |
| `MODE_INHERITABLETHREADLOCAL` | Enables `@Async` SecurityContext propagation without extra configuration |
| BCrypt strength 12 | Industry standard; OWASP recommended minimum is 10, 12 provides extra margin |
| `@EnableMethodSecurity` at framework level | Business code can use `@PreAuthorize` immediately without extra configuration |
| `@ConditionalOnMissingBean` on TokenService | Business can provide a custom implementation (e.g., third-party JWT provider) |
