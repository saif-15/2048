# 2048 Arena

A three-tier full-stack 2048 game: **Spring Boot 4 + Spring Security** backend, **Thymeleaf** frontend,
**MySQL** database.

| Tier | What lives there |
|---|---|
| Presentation | `templates/**` (Thymeleaf), `static/css/app.css`, `static/js/game.js` (the game itself) |
| Business | `controller/**` (MVC + JSON API), `service/**` (`UserService`, `GameService`), `security/**`, `config/SecurityConfig` |
| Data | `model/**` (JPA entities `User`, `GameScore`), `repository/**` (Spring Data JPA), MySQL |

## Features

- Sign up with validation (unique username/email, password rules, confirm password), BCrypt-hashed passwords
- Form login, "remember me" (14 days), logout, CSRF protection, disabled-account handling
- Playable 2048 (keyboard, WASD, swipe), in-progress game survives page refresh
- Every finished game is saved: score, highest tile, moves, duration, win flag
- Per-player stats: highest score, global rank, highest tile, games/wins/win-rate, average, totals,
  best-tile distribution chart, paginated game history
- Public leaderboard (paginated, tie-break on highest tile) + best single games, public player profiles
- Account settings: edit display name / email / country, change password
- Admin dashboard (`ROLE_ADMIN`): site stats, search users, enable/disable, promote/demote, delete
- JSON API: `POST /api/scores` (authenticated), `GET /api/scores/top?limit=10` (public)
- Server-side plausibility checks on submitted scores

## Running

1. Start MySQL 8 (any way you like, or `docker compose up -d`). The `game2048` database and tables are
   created automatically.
2. Configure credentials if they are not `root` / `root`:

   ```powershell
   $env:DB_USERNAME = "root"; $env:DB_PASSWORD = "your-password"
   ```
3. Run:

   ```powershell
   $env:JAVA_HOME = "C:\Program Files\jdk-19"
   .\mvnw.cmd spring-boot:run
   ```
4. Open http://localhost:8080

An administrator account is created on first start: **admin / Admin@2048**
(override with `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_EMAIL`). Change the password after logging in.

Other settings: `DB_URL`, `PORT`, `REMEMBER_ME_KEY` (set this to a long random value in production).

## Tests

```powershell
.\mvnw.cmd test
```

Tests use an in-memory H2 database (`application-test.properties`), so MySQL is not needed for them.

## Note on cheating

The game runs in the browser, so a determined user can post a fake score. The server rejects
impossible results (non power-of-two tiles, scores too high for the tile reached, etc.) but a fully
cheat-proof leaderboard would require replaying moves on the server.
