# MavMatch 🤝

A full-stack web application that helps University of Texas at Arlington (UTA) students find compatible study partners based on shared courses and overlapping weekly availability.

**Live Demo:** [mavmatch-production.up.railway.app](https://mavmatch-production.up.railway.app)

---

## What It Does

Students register with their UTA email, select their courses, and set their weekly availability. MavMatch's matching algorithm then finds other students who share at least one course and calculates how many hours per week their schedules overlap. Matches are ranked by overlap hours so the most compatible study partners appear first.

Once matched, students can chat in real time, schedule study sessions, and manage their meetings — all in one place.

---

## Features

- **Smart Matching Algorithm** — finds students sharing courses and ranks them by weekly schedule overlap
- **Real-Time Chat** — message your matches directly in the app
- **Study Session Scheduling** — schedule meetings with your matches, accept or decline requests
- **Availability Grid** — set your weekly free time visually
- **Notification Bell** — get notified of new messages and meeting requests
- **Admin Dashboard** — view reported messages, ban or delete accounts
- **100 Seeded Students** — pre-loaded dummy accounts for realistic demo experience

---

## Tech Stack

### Frontend
- HTML5, CSS3, JavaScript (ES6+) — no frameworks
- Google Fonts (Nunito, Inter)
- Fetch API for all backend communication

### Backend
- Java 17
- Spring Boot 3.5
- Spring Security (BCrypt password hashing)
- Spring Data JPA + Hibernate ORM
- Maven

### Database
- PostgreSQL (hosted on Railway)
- 8 tables: students, courses, student_courses, availability, matches, meeting_requests, messages, blocked_users

### Deployment
- Deployed on Railway (cloud platform)
- Dockerized build
- Auto-redeploy on GitHub push

---

## Database Schema

| Table | Description |
|---|---|
| students | User accounts with BCrypt hashed passwords |
| courses | Course catalog (code + name) |
| student_courses | Many-to-many: students ↔ courses |
| availability | Weekly time slots per student |
| matches | Computed matches with overlap hours |
| meeting_requests | Scheduled study sessions |
| messages | Chat messages between matched students |
| blocked_users | Block relationships between students |

---

## Matching Algorithm

1. Find all students who share at least one course with the user
2. For each candidate, compare availability hour-by-hour across all days
3. Calculate total weekly overlap in hours
4. Filter out blocked users
5. Rank results by overlap hours (descending)
6. Save results to database and return paginated (10 per page)

Optimized using bulk database queries to avoid N+1 performance issues.

---

## Getting Started (Local Development)

### Prerequisites
- Java 17
- Maven
- PostgreSQL database

### Setup

1. Clone the repository:
```bash
git clone https://github.com/YOUR_USERNAME/mavmatch.git
cd mavmatch
```

2. Create `src/main/resources/application-local.properties`:
```properties
spring.datasource.url=jdbc:postgresql://YOUR_HOST:PORT/YOUR_DB
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update
server.port=8080
```

3. Run with local profile in IntelliJ:
   - Set environment variable: `SPRING_PROFILES_ACTIVE=local`
   - Run `MavMatchApplication.java`

4. Open `http://localhost:8080`

---

## Project Structure

```
src/
├── main/
│   ├── java/com/mavmatch/
│   │   ├── config/          # Security, data seeding
│   │   ├── controller/      # REST API endpoints
│   │   ├── model/           # JPA entities
│   │   ├── repository/      # Database repositories
│   │   └── service/         # Business logic + matching algorithm
│   └── resources/
│       └── static/          # Frontend HTML/CSS/JS files
```

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | /api/register | Register new student |
| POST | /api/login | Authenticate student |
| GET/POST | /api/courses | Get or add courses |
| DELETE | /api/courses | Remove a course |
| GET/POST | /api/availability | Get or save availability |
| GET | /api/matches | Get matches (with optional refresh) |
| GET/POST/DELETE | /api/meetings | Manage study sessions |
| GET/POST | /api/messages | Chat messages |
| POST | /api/block | Block a user |
| GET | /api/notifications | Get unread notifications |
| GET | /api/admin/dashboard | Admin statistics |

---

## Team

Primary design and development by Ahmad Abdallah as a Senior IS Project at the University of Texas at Arlington.

Other group members were part of the assigned project team and are acknowledged for their participation and support.
---

## License

This project is for educational purposes as part of a university senior project.
