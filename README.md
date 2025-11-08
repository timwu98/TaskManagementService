# Note Service

Java 17 / Spring Boot 3.x  
Features: JPA + TX + Profiles (dev/prod) + Security + JWT

## Run (dev, H2)
Run the App in dev profile
Option 1: From IntelliJ

Edit configuration in IDE
Add VM options:
`-Dspring.profiles.active=dev`
Click Run

Option 2: From Terminal
`mvn spring-boot:run -Dspring-boot.run.profiles=dev`

Open H2 console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:notesdb)

## Run (prod, PostgreSQL)
```declarative
docker run --name notes-pg -e POSTGRES_DB=notes \
-e POSTGRES_USER=notes -e POSTGRES_PASSWORD=notes_password \
-p 5432:5432 -d postgres:16
```


SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run

## Flow
1) Register: POST /api/auth/register {"username":"emma","password":"pass123"}
2) Login:    POST /api/auth/login    -> { "token": "..." }
3) Access:   GET  /api/notes with Header: Authorization: Bearer <token>

## Debug
GET /api/debug/context -> show activeProfiles & welcome message
App: http://localhost:8080/api/debug/context
→ should return:
```
{
"activeProfiles": ["dev"],
"message": "👩‍💻 Running in DEV mode"
}
```

H2 DB Console: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:notesdb
User Name: sa
Password: (leave blank)


## Test basic API flow
### Register a user
```declarative
curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{"username":"emma","password":"pass123"}'
```


### Login
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"emma","password":"pass123"}'

Then test the notes:

TOKEN=<paste your token here>

### Create note
curl -X POST http://localhost:8080/api/notes \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"title":"Hello","content":"First note"}'

### List notes
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/notes


### Unit tests
```
mvn -q -DskipTests=false test

mvn test -Dspotless.check.skip=true # skip the formatting check
```


### code coverage check
#### option 1 
```
mvn clean test verify jacoco:report
```

#### Option 2
```
mvn clean test
mvn jacoco:report
```
find the report under `HTML：target/site/jacoco/index.html` and open in browser

### Code Style check -- formatting
``` 
mvn spotless:check     # verify formatting (CI)
mvn spotless:apply     # auto-fix formatting locally

## Skip the formattting test
mvn clean verify -Dspotless.check.skip=true

```