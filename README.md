# Gemini Web App

A web application that securely processes user prompts with a predefined system prompt, sends them to Google's Gemini AI API, and displays the AI-generated reports back to users through a web interface.

## Features

- Anonymous user support with single request limit
- User registration via Google OAuth or email
- Secure prompt processing and validation
- AI-powered report generation using Google Gemini API
- Request logging and history tracking
- Responsive web interface

## Technology Stack

- **Backend**: Spring Boot 3.x with Java 17+
- **Database**: PostgreSQL with Flyway migrations
- **Cache**: Redis for session management (optional)
- **Security**: Spring Security with OAuth2
- **Frontend**: React.js with TypeScript (to be implemented)

## Deployment Options

### With Redis (Recommended for Production)
- **Session Management**: Redis-based distributed sessions
- **Caching**: Redis for application-level caching
- **Scalability**: Supports horizontal scaling with shared session state
- **Performance**: Better performance with caching layer

### Without Redis (Simplified Setup)
- **Session Management**: In-memory sessions (single instance only)
- **Caching**: No external caching (relies on application-level caching)
- **Scalability**: Single instance deployment
- **Performance**: Suitable for development and small-scale deployments
- **Limitations**: 
  - Sessions are lost on application restart
  - Cannot scale horizontally (no shared session state)
  - Rate limiting data is not persistent across restarts

## Prerequisites

### Required
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Google Cloud Platform account (for Gemini API and OAuth)

### Optional
- Redis 6+ (for session management and caching - can run without it using in-memory alternatives)

## Configuration

The application uses environment variables for configuration. Copy the example environment file and update the values:

### Required Environment Variables

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/gemini_web_app
DATABASE_USERNAME=gemini_user
DATABASE_PASSWORD=gemini_password

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Gemini API
GEMINI_API_KEY=your-gemini-api-key

# Security
JWT_SECRET=your-jwt-secret-key-change-in-production

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

### Optional Environment Variables (Redis Setup)

```bash
# Redis (only needed when using Redis profile)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

## Running the Application

### Development Mode

#### With Redis (Default)
```bash
# Start PostgreSQL and Redis (using Docker)
docker-compose up -d postgres redis

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Without Redis (Simplified Setup)
```bash
# Option 1: Use the convenience script
./run-without-redis.sh

# Option 2: Manual setup
# Start only PostgreSQL (using Docker)
docker-compose -f docker-compose.no-redis.yml up -d postgres

# Run the application without Redis
mvn spring-boot:run -Dspring-boot.run.profiles=no-redis
```

#### Full Stack with Docker (Without Redis)
```bash
# Start the entire application stack without Redis
docker-compose -f docker-compose.no-redis.yml up -d

# Or with frontend development server
docker-compose -f docker-compose.no-redis.yml --profile frontend up -d
```

### Production Mode

#### With Redis (Recommended)
```bash
# Build the application
mvn clean package

# Run with production profile
java -jar target/gemini-web-app-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

#### Without Redis (Simplified Production)
```bash
# Build the application
mvn clean package

# Run without Redis (uses in-memory session management)
java -jar target/gemini-web-app-0.0.1-SNAPSHOT.jar --spring.profiles.active=no-redis
```

## API Endpoints

### Authentication
- `POST /api/auth/google` - Google OAuth callback
- `POST /api/auth/email` - Email registration
- `POST /api/auth/login` - Email login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/me` - Current user info

### Core Application
- `POST /api/prompt` - Submit prompt for AI processing
- `GET /api/user/quota` - Get remaining request quota
- `GET /api/user/history` - Get request history

### Health Check
- `GET /actuator/health` - Application health status

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/example/geminiapp/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── service/         # Business logic
│   │   ├── repository/      # Data access layer
│   │   ├── entity/          # JPA entities
│   │   ├── dto/             # Data transfer objects
│   │   └── exception/       # Exception handling
│   └── resources/
│       ├── application.yml  # Main configuration
│       ├── application-dev.yml   # Development profile
│       ├── application-prod.yml  # Production profile
│       └── db/migration/    # Database migrations
└── test/                    # Test classes
```

## License

This project is licensed under the MIT License.