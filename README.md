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

1. Start MySQL 8. The `game2048` database and tables are
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

## Deploying on AWS EC2

The app currently runs on an EC2 instance (Amazon Linux 2023) as a plain jar, with MySQL 8.4 on the same machine.

1. **Security group:** allow inbound SSH (22) and the app port (TCP 8080) to the instance.
2. **Install Git, Java 17 and Maven:**

   ```bash
   sudo yum install -y git
   sudo yum install -y java-17
   sudo yum install -y maven
   ```
3. **Install MySQL server.** Amazon Linux does not ship MySQL, so add the official MySQL 8.4 repository first:

   ```bash
   sudo dnf install -y https://dev.mysql.com/get/mysql84-community-release-el9-1.noarch.rpm
   sudo rpm --import https://repo.mysql.com/RPM-GPG-KEY-mysql-2023
   dnf repolist enabled | grep -i mysql        # check the MySQL repo is enabled

   sudo dnf install -y mysql-community-server
   ```
4. **Start MySQL** and make it start on boot:

   ```bash
   systemctl list-unit-files | grep -i mysql   # the service is called mysqld
   sudo systemctl enable --now mysqld
   sudo systemctl status mysqld
   ```
5. **Secure MySQL and set the root password.** The install generates a temporary root password in the log;
   use it to run the security script, which asks you to choose a new root password:

   ```bash
   sudo grep 'temporary password' /var/log/mysqld.log
   sudo mysql_secure_installation
   mysql -u root -p                            # check you can log in with the new password
   ```

   MySQL's password policy requires upper and lower case letters, a digit and a symbol. The app's database
   (`game2048`) and tables are created automatically on first start, so nothing else is needed here.
6. **Get the code and build the jar** (tests are skipped; they run locally):

   ```bash
   git clone https://github.com/saif-15/2048.git
   cd 2048
   mvn clean package -DskipTests
   ```

   This produces `target/2048-1.0.jar`.
7. **Run it** with the MySQL root password from step 5:

   ```bash
   export DB_USERNAME=root DB_PASSWORD='your-mysql-password'
   java -jar target/2048-1.0.jar
   ```
8. Open `http://<ec2-public-ip>:8080`.

To deploy a new version: `git pull`, rebuild with `mvn clean package -DskipTests`, stop the running jar
(<kbd>Ctrl</kbd>+<kbd>C</kbd>) and start it again.

## Tests

```powershell
.\mvnw.cmd test
```

Tests use an in-memory H2 database (`application-test.properties`), so MySQL is not needed for them.

## Note on cheating

The game runs in the browser, so a determined user can post a fake score. The server rejects
impossible results (non power-of-two tiles, scores too high for the tile reached, etc.) but a fully
cheat-proof leaderboard would require replaying moves on the server.
