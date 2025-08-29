# Java Hiring Challenge - Spring Boot Application

This Spring Boot application automatically executes the Java Hiring Challenge flow on startup. It generates a webhook, determines which SQL problem to solve based on registration number parity, and submits the final SQL query.

## Features

- **Automatic Startup Flow**: Executes automatically when the application starts
- **Smart Question Selection**: Determines Q1 or Q2 based on registration number parity
- **Robust HTTP Handling**: Configurable timeouts, retries, and exponential backoff
- **Secure Token Handling**: Never logs or exposes access tokens
- **Comprehensive Logging**: Detailed progress tracking and error reporting

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Configuration

Before running the application, update the configuration in `src/main/resources/application.yml`:

```yaml
app:
  name: "Your Name"
  regNo: "YourRegistrationNumber"
  email: "your.email@example.com"
  generateWebhookUrl: "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA"

http:
  connectTimeout: 10000  # 10 seconds
  readTimeout: 30000     # 30 seconds
  retry:
    maxAttempts: 3
    backoffBase: 1000    # 1 second base for exponential backoff
```

## SQL Query Files

Place your SQL queries in the following files:
- `src/main/resources/queries/Q1.sql` - SQL query for Question 1
- `src/main/resources/queries/Q2.sql` - SQL query for Question 2

## Building the Application

```bash
mvn clean package
```

This will create a runnable JAR file in the `target/` directory.

## Running the Application

```bash
java -jar target/java-hiring-app-1.0.0.jar
```

## How It Works

1. **Startup**: Application automatically begins execution when started
2. **Webhook Generation**: POSTs to the generate webhook endpoint with your details
3. **Question Selection**: Determines Q1 or Q2 based on registration number parity
4. **SQL Loading**: Reads the appropriate SQL query from the queries directory
5. **Submission**: POSTs the SQL query to the returned webhook URL with proper authorization
6. **Completion**: Logs success and continues running

## Question Selection Logic

- Extracts all digits from the registration number
- Takes the last two digits (or single digit if only one exists)
- If the number is odd → Question 1
- If the number is even → Question 2

## Error Handling

- **Network Issues**: Automatic retry with exponential backoff
- **Server Errors**: Retry up to configured maximum attempts
- **Client Errors**: Limited retry for authorization issues
- **Fatal Errors**: Application exits with non-zero status code

## Logging

The application provides comprehensive logging:
- Progress tracking for each step
- Error details with context
- Token redaction for security
- Timestamp information

## HTTP Configuration

- **Connect Timeout**: Time to establish connection
- **Read Timeout**: Time to read response
- **Retry Policy**: Configurable attempts with exponential backoff
- **Authorization**: Tries Bearer token first, falls back to direct token

## Security Features

- Access tokens are never logged or committed
- All sensitive information is redacted in logs
- Secure HTTP client configuration

## Troubleshooting

### Common Issues

1. **Configuration Errors**: Ensure all required fields are set in `application.yml`
2. **Network Timeouts**: Adjust timeout values in configuration
3. **Authorization Failures**: Check if the server accepts Bearer or direct token format
4. **Missing SQL Files**: Ensure both Q1.sql and Q2.sql exist in the queries directory

### Debug Mode

Enable debug logging by setting the log level in `application.yml`:

```yaml
logging:
  level:
    com.healthrx: DEBUG
```

## Build Artifacts

The build process creates:
- `target/java-hiring-app-1.0.0.jar` - Runnable Spring Boot JAR (28MB)
- Source code and resources
- Maven dependencies

**Download Link**: The JAR file is available in the `target/` directory after building.

## Submission Requirements

This application meets all the specified requirements:
- ✅ Automatic startup execution
- ✅ Webhook generation and validation
- ✅ Question selection based on registration number parity
- ✅ SQL query loading from resource files
- ✅ Final query submission with proper authorization
- ✅ Comprehensive error handling and retry logic
- ✅ Secure token handling
- ✅ Detailed logging and progress tracking

## License

This project is created for the Java Hiring Challenge.
