# Running the project on Windows — complete walkthrough

Written for Windows 10/11 with PowerShell. Follow it top to bottom the first time.

---

## Step 0 — What you are about to run

Two programs, in two separate terminal windows, both running at the same time:

| | What it is | Where it runs |
|---|---|---|
| **Backend** | The Java API — database, business rules, WebSocket | `http://localhost:8080` |
| **Frontend** | The React website you actually look at | `http://localhost:5173` |

The frontend talks to the backend. **Both must be running.** If you close the backend
window, the website loads but shows no listings.

You do **not** need MySQL for the first run. The backend defaults to a `demo` profile
that uses an in-memory database and fills itself with 8 listings, 6 accounts, bookings,
reviews and a chat thread every time it starts.

---

## Step 1 — Install the three tools

Open PowerShell and check what you already have:

```powershell
java -version
mvn -version
node -v
```

Each should print a version. Anything that says *"is not recognized"* needs installing.

### Java 17 or newer

```powershell
winget install Microsoft.OpenJDK.17
```

The version line must say **17 or higher**. Java 8 or 11 will not work — Spring Boot 3
requires 17 as a minimum.

### Maven

```powershell
winget install Apache.Maven
```

### Node.js

```powershell
winget install OpenJS.NodeJS.LTS
```

> **Important:** after any install, **close PowerShell and open a new window.** New
> programs are only added to your PATH in terminals opened afterwards. This is the single
> most common reason "I installed it but it still says not recognized".

Verify all three again in the fresh window before continuing.

---

## Step 2 — Start the backend

In your **first** PowerShell window:

```powershell
cd C:\path\to\accommodation-finder\backend
mvn spring-boot:run
```

The first run downloads dependencies and takes **2–5 minutes**. Later runs take seconds.

You are ready when you see something like:

```
Started AccommodationFinderApplication in 4.812 seconds
Seeded 6 users, 8 rooms, 4 bookings
Sign in as tenant  -> akhil@epita.fr   / password123
```

**Leave this window open.** Closing it stops the API.

Quick check: open <http://localhost:8080/api/rooms> in a browser. You should see a wall
of JSON. That means the backend works.

---

## Step 3 — Start the frontend

Open a **second** PowerShell window (leave the first one running):

```powershell
cd C:\path\to\accommodation-finder\frontend
npm install
npm run dev
```

`npm install` only needs running the first time, and takes a minute or two.

You are ready when you see:

```
VITE v5.4.21  ready in 400 ms
➜  Local:   http://localhost:5173/
```

---

## Step 4 — Open the app

Go to **<http://localhost:5173>**.

You should see the dark blue hero, a search bar, and eight listings with a map beside them.

Sign in with any of these — the login page has one-click buttons that fill them in:

| Role | Email | Password |
|---|---|---|
| Tenant | `akhil@epita.fr` | `password123` |
| Landlord | `jean@landlord.fr` | `password123` |
| Admin | `admin@accomfinder.app` | `password123` |

---

## Step 5 — Demonstrate the real-time feature

This is the part worth showing in a demo or an interview. You need **two browser windows**:

1. Normal window → sign in as **akhil@epita.fr** (tenant). Stay on the search page.
2. Private/incognito window (Ctrl+Shift+N) → sign in as **jean@landlord.fr** (landlord).

Now:

| In Jean's window | Watch Akhil's window |
|---|---|
| Dashboard → *Mark taken* on "Bright studio 5 min from EPITA" | The badge flips to **Taken** and the card flashes — no refresh |
| — | Open that listing → **Request to book** |
| The request appears in Jean's dashboard instantly | — |
| Click **Accept** | Akhil's *My bookings* page updates the moment you click |
| Open the listing → **Message the landlord** | Chat is live in both directions |

The green dot in the navbar means the WebSocket is connected. Red means it dropped and
is retrying.

---

## Step 6 (optional) — Use MySQL instead

Only do this once the demo profile works. The demo database is wiped on every restart;
MySQL keeps your data.

```powershell
# load the schema and demo data (asks for your MySQL root password)
cd C:\path\to\accommodation-finder
mysql -u root -p < database\schema.sql
mysql -u root -p < database\seed.sql

# start the backend against MySQL
cd backend
mvn spring-boot:run -D"spring-boot.run.profiles=dev"
```

> The quotes around `spring-boot.run.profiles=dev` matter in PowerShell — without them
> PowerShell mangles the `.` and Maven never sees the argument.

If your MySQL root password is not `root`, set it first:

```powershell
$env:DB_PASSWORD = "your_actual_password"
```

---

## Other useful URLs

| URL | What it is |
|---|---|
| <http://localhost:5173> | The app |
| <http://localhost:8080/swagger-ui.html> | Interactive API docs — try every endpoint in the browser |
| <http://localhost:8080/h2-console> | Demo database browser. JDBC URL: `jdbc:h2:mem:accommodation`, user `sa`, no password |

---

## Running from VS Code instead of the terminal

1. Install the **Extension Pack for Java** (Microsoft) — it bundles Maven, so you can skip
   installing Maven separately.
2. Open the `accommodation-finder` folder.
3. Open `backend/src/main/java/com/accommodationfinder/AccommodationFinderApplication.java`
   and click **Run** above `public static void main`.
4. For the frontend, use the VS Code terminal: `cd frontend`, `npm install`, `npm run dev`.

---

## Troubleshooting

**`mvn` / `node` / `java` is not recognized**
You installed it but did not open a new terminal. Close PowerShell, open it again.

**`Web server failed to start. Port 8080 was already in use`**
Something else is on that port. Find and stop it:

```powershell
netstat -ano | findstr :8080
taskkill /PID <the number in the last column> /F
```

Or run on a different port: `$env:SERVER_PORT = "8081"` — then update
`frontend/vite.config.js` so the proxy targets 8081 too.

**Website loads but says "Cannot reach the server"**
The backend is not running, or it crashed. Check the first PowerShell window for a red
stack trace.

**`npm install` fails with a permissions or network error**
Try `npm cache clean --force`, then `npm install` again. Make sure `node -v` prints 18 or
higher.

**`JAVA_HOME not found` or Maven picks the wrong Java**
Point it at your JDK 17 explicitly:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.13.11-hotspot"
```

Adjust the folder name to match what is actually in `C:\Program Files\Microsoft\`.

**MySQL: `Access denied for user 'root'@'localhost'`**
Your password is not `root`. Set `$env:DB_PASSWORD` as shown in Step 6.

**MySQL: `Unknown database 'accommodation_finder'`**
You skipped `schema.sql`. Run it, then `seed.sql`.

**Login says "Invalid email or password"**
Check for a trailing space in the email. Every demo password is exactly `password123`.

---

## Stopping everything

Press **Ctrl+C** in each PowerShell window. On the demo profile the database disappears
when the backend stops — that is intentional, and it comes back seeded next time.
