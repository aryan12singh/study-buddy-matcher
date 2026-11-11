"""Structure panels: technology stack, packages and layers, deployment and the database schema.

Table and column names are copied from backend/src/main/resources/db/migrations; package and
class names from backend/src/main/java/com/studybuddy.
"""

from diagram_canvas import AMBER, BLUE, GREEN, INK, MUTED, PALE, TEAL, WARM, Canvas

NOTE = "#edf3f6"
DANGER = "#9f2828"


# ---- technology stack ---------------------------------------------------------------------

def _window(c, x, y, w, h, title, color=INK):
    """A retro desktop window: bevelled frame, title bar and three square controls."""
    c.rect(x+6, y+6, w, h, "#b9c6ce", "none", 0, c.back)
    c.rect(x, y, w, h, "#e9e9e7", INK, 0, c.back)
    c.rect(x+4, y+4, w-8, 38, color, "none", 0, c.back)
    c.text(x+18, y+30, title, 18, "#ffffff", 700, layer=c.back)
    for n in range(3):
        c.rect(x+w-30-n*26, y+12, 20, 20, "#d1d2d0", "#f4f5f4", 0, c.back)


def _tiles(c, x, y, items):
    for n, (slug, name, role) in enumerate(items):
        glyph = slug[1:] if slug.startswith("*") else None
        c.tech(x, y+n*92, 510, slug, name, role, glyph=glyph)


def tech_stack():
    c = Canvas("tech-stack", "Technology stack — what runs where",
        "Every library, tool and hosted service the project uses, grouped by the tier it runs in. Versions are the ones in the README table.",
        width=2040, height=1420)
    columns = (45, 745, 1445)
    _window(c, columns[0], 170, 550, 530, "BROWSER  ·  frontend/", BLUE)
    _tiles(c, columns[0]+20, 230, [
        ("react", "React 19", "Function components and hooks, one per file"),
        ("typescript", "TypeScript 6", "DTO types mirror the API contract"),
        ("vite", "Vite 8", "Development server and production build"),
        ("reactrouter", "React Router 7", "Routes, role guards, unsaved-draft blocker"),
        ("axios", "Axios", "One shared client: token, 401, timeouts"),
    ])
    _window(c, columns[1], 170, 550, 530, "SPRING BOOT API  ·  backend/", BLUE)
    _tiles(c, columns[1]+20, 230, [
        ("openjdk", "Java 21", "Language and runtime (records, sealed types)"),
        ("springboot", "Spring Boot 4.1", "Web MVC, validation, typed configuration"),
        ("springsecurity", "Spring Security", "Stateless filter chain and role checks"),
        ("jsonwebtokens", "JSON Web Tokens (JJWT)", "Signed tokens with role and version claims"),
        ("hibernate", "Hibernate ORM / JPA", "Spring Data repositories, validate mode"),
    ])
    _window(c, columns[2], 170, 550, 530, "EXTERNAL SERVICES  ·  HTTPS", AMBER)
    _tiles(c, columns[2]+20, 230, [
        ("googlegemini", "Google Gemini API", "E1 explanations, quizzes, plans, Pixel"),
        ("spotify", "Spotify Web API", "Token exchange on the server, playback in browser"),
        ("*wave", "Freesound API", "CC0 ambient sounds for the study room"),
        ("*weather", "Open-Meteo", "Campus weather, cached with a fallback"),
    ])
    c.lines(columns[2]+24, 628, ["Keys and client secrets stay on the backend;", "the app still works when a service is down.", "Anthropic Claude is an optional second AI provider."], 16, MUTED, gap=24)

    _window(c, columns[0], 790, 550, 438, "FRONTEND TOOLING", TEAL)
    _tiles(c, columns[0]+20, 850, [
        ("npm", "npm on Node.js 22", "Locked installs (npm ci) and scripts"),
        ("vitest", "Vitest + jsdom", "Interaction tests that reuse the Vite config"),
        ("testinglibrary", "Testing Library", "Tests what a user sees and clicks"),
        ("oxc", "Oxlint", "Lint rules for React and TypeScript"),
    ])
    _window(c, columns[1], 790, 550, 438, "DATABASE", TEAL)
    _tiles(c, columns[1]+20, 850, [
        ("postgresql", "PostgreSQL", "34 tables, SQL migrations, row locks, RLS"),
        ("supabase", "Supabase", "Managed PostgreSQL shared by the team"),
        ("docker", "Docker", "Local demo database, disposable test database"),
    ])
    c.lines(columns[1]+24, 1150, ["SQL migrations own the schema;", "Hibernate only validates it at startup."], 16, MUTED, gap=24)
    _window(c, columns[2], 790, 550, 438, "BACKEND TOOLING AND DELIVERY", TEAL)
    _tiles(c, columns[2]+20, 850, [
        ("apachemaven", "Apache Maven", "Build, dependencies and the test run"),
        ("junit5", "JUnit Jupiter + Mockito", "Unit, slice and PostgreSQL integration tests"),
        ("githubactions", "GitHub Actions", "CI on every pull request and push to main"),
        ("github", "GitHub", "Branches, reviews and squash merges"),
    ])
    # Runtime links between the tiers
    c.line([(595, 435), (745, 435)], color=INK, end="solid", start="solid", width=2.5)
    c.label(670, 398, ["JSON over HTTP", "Bearer JWT"], 16, color=INK)
    c.line([(1295, 435), (1445, 435)], color=INK, end="solid", width=2.5)
    c.label(1370, 398, ["HTTPS from", "typed clients"], 16, color=INK)
    c.line([(1020, 706), (1020, 790)], color=INK, end="solid", width=2.5)
    c.label(1040, 754, "JDBC  (the browser never reaches the database)", 16, color=INK, anchor="start")
    c.note(45, 1260, 1950, [
        "Top row = what runs while a student uses the app; bottom row = what stores the data and what builds, tests and delivers the code.",
        "Brand marks: Simple Icons, CC0 1.0 (docs/diagrams/brand_icons.py). They are trademarks of their owners, shown only to name each technology.",
        "Freesound and Open-Meteo have no mark in that set, so a neutral glyph is drawn. Mockito shares the JUnit tile for the same reason.",
    ], fill=NOTE)
    return c


# ---- packages and layers ------------------------------------------------------------------

def _mini(c, x, y, name, color, w=185):
    c.rect(x, y-22, 74, 22, "white", color, 0)
    c.rect(x, y, w, 46, "white", color, 0)
    c.text(x+w/2, y+30, name, 18, color, 700, "middle")
    c.geo[name] = (x, y, w, 46)


def package_layers():
    c = Canvas("package-layers", "UML packages — feature packages and the layers inside each",
        "25 packages under com.studybuddy, grouped by owning team; feature packages repeat the same controller → service → domain → repository layering.",
        width=2100, height=1440)
    c.package(45, 215, 2010, 560, "com.studybuddy", BLUE, "#fbfcfd")
    groups = [
        (70, 420, "TEAM A  ·  matching engine and music", BLUE, PALE),
        (510, 660, "TEAM B  ·  platform, identity and AI study tools", AMBER, WARM),
        (1190, 840, "TEAM C  ·  requests, groups, admin, notifications, E1 and E2", TEAL, GREEN),
    ]
    for x, w, label, color, fill in groups:
        c.rect(x, 240, w, 400, fill, "#bdccd5", 10, c.back)
        c.text(x+18, 270, label, 16, color, 700, layer=c.back)
    # Team A: matching with its four sub-packages, and music
    c.rect(95, 303, 110, 24, "white", BLUE, 0)
    c.rect(95, 327, 370, 205, "white", BLUE, 0)
    c.text(105, 321, "matching", 17, BLUE, 700)
    c.geo["matching"] = (95, 327, 370, 205)
    for n, name in enumerate(("availability", "scoring", "strategy", "config")):
        _mini(c, 110+(n % 2)*178, 372+(n//2)*84, name, BLUE, 162)
    _mini(c, 95, 580, "music", BLUE)
    for n, name in enumerate(("auth", "user", "student", "course", "security", "seed", "ai", "quiz", "studyplan", "assistant")):
        _mini(c, 535+(n % 3)*212, 320+(n//3)*84, name, AMBER)
    for n, name in enumerate(("matchrequest", "connection", "profile", "studygroup", "notification", "admin",
                              "matchexplanation", "studyroom", "studysession", "weather", "achievement", "forest")):
        _mini(c, 1212+(n % 4)*204, 320+(n//4)*84, name, TEAL, 190)
    c.rect(880, 668, 90, 24, "white", INK, 0)
    c.rect(880, 692, 340, 62, "white", INK, 0)
    c.text(890, 686, "common", 17, INK, 700)
    c.text(1050, 731, "AccountAccess · error · http", 17, INK, anchor="middle")
    for route in ([(280, 640), (280, 723), (880, 723)], [(1050, 640), (1050, 692)], [(1610, 640), (1610, 723), (1220, 723)]):
        c.line(route, dashed=True, end="arrow")
    c.label(560, 708, "«import»", 16)
    c.label(1100, 664, "«import»", 16)
    c.label(1440, 708, "«import»", 16)

    c.package(45, 870, 2010, 380, "com.studybuddy.matchrequest  (one feature package, opened up)", TEAL, "#fbfcfd")
    boxes = [
        (80, 945, "MatchRequestController", "RestController"), (560, 945, "MatchRequestService", "Service"),
        (1040, 945, "MatchRequestAssembler", "Component"), (1520, 945, "MatchRequestDto", "record"),
        (560, 1110, "MatchRequest", "Entity"), (1040, 1110, "MatchRequestRepository", "Repository"),
    ]
    for x, y, name, stereotype in boxes:
        c.cls(x, y, 320, name, stereotype=stereotype, color=TEAL, size=17)
    c.line([(400, 980), (560, 980)], end="arrow")
    c.label(480, 958, "calls one method", 15)
    c.line([(880, 980), (1040, 980)], end="arrow")
    c.label(960, 958, "toDto(request)", 15)
    c.line([(1360, 980), (1520, 980)], dashed=True, end="arrow")
    c.label(1440, 958, "«create»", 15)
    c.line([(640, 1015), (640, 1110)], end="arrow")
    c.label(640, 1068, "accept()", 15)
    c.line([(800, 1015), (800, 1062), (1200, 1062), (1200, 1110)], end="arrow")
    c.label(1000, 1052, "loads and saves", 15)
    c.line([(1040, 1145), (880, 1145)], dashed=True, end="arrow")
    c.label(960, 1168, "persists", 15)
    c.line([(1680, 945), (1680, 908), (240, 908), (240, 945)], color=TEAL, end="arrow")
    c.label(960, 913, "✓  Rule 2: only the DTO travels back; the entity never leaves the service", 17, color=TEAL)
    c.line([(240, 1015), (240, 1215), (1200, 1215), (1200, 1180)], color=DANGER, dashed=True)
    c.label(720, 1220, "✗  Rule 1: a controller never touches a repository", 17, color=DANGER)
    c.note(45, 1280, 2010, [
        "Packages are organised by feature, so one folder holds everything a marker needs to follow a use case. Shared packages: common, security, user, ai and seed.",
        "The lower package shows the roles a feature package repeats. Open arrows are dependencies; the green path is what a response may carry, the red line a dependency the code forbids.",
    ], fill=NOTE)
    return c


# ---- deployment ---------------------------------------------------------------------------

def deployment():
    c = Canvas("deployment", "UML deployment — demo and team setup",
        "Three tiers and four outside services. For the demo the first two nodes are the same laptop (localhost); the team shares one hosted database.",
        width=2090, height=1240)
    c.node(60, 250, 410, 380, "device", "Client laptop")
    c.node(90, 345, 350, 250, "execution environment", "Web browser", TEAL, depth=10)
    c.artifact(115, 430, 300, "React application", ["index.html, JavaScript, CSS", "token kept in sessionStorage"], color=TEAL)
    c.node(730, 250, 620, 560, "device", "Application host")
    c.node(765, 345, 550, 200, "execution environment", "JVM 21", BLUE, depth=10)
    c.artifact(795, 425, 490, "study-buddy-matcher-backend.jar", ["Spring Boot with embedded Tomcat, port 8080"])
    c.node(765, 600, 550, 180, "execution environment", "Node.js 22", BLUE, depth=10)
    c.artifact(795, 675, 490, "Vite development server", ["serves frontend/ on port 5173"], stereotype="process")
    c.node(1560, 250, 470, 270, "device", "Database host")
    c.artifact(1585, 335, 420, "PostgreSQL database", ["Supabase for the team", "Docker container for the local demo", "34 tables created by SQL migrations"], stereotype="database", color=TEAL)
    c.node(1560, 610, 470, 300, "external system", "External services", AMBER, WARM)
    c.lines(1795, 700, ["Google Gemini API (optional: Anthropic API)", "Spotify Accounts and Web API", "Freesound API", "Open-Meteo API"], 18, gap=44, anchor="middle")
    c.node(60, 830, 410, 190, "device", "GitHub Actions runner", MUTED)
    c.lines(265, 915, ["backend: ./mvnw test with postgres:17", "frontend: test, lint, build", "runs on every pull request"], 17, gap=30, anchor="middle")
    c.line([(440, 420), (765, 420)], color=INK, width=2.5)
    c.label(608, 384, ["«HTTP» JSON + JWT", "port 8080"], 16, color=INK)
    c.line([(440, 560), (608, 560), (608, 690), (765, 690)], color=INK, width=2.5)
    c.label(608, 730, ["«HTTP» static files", "port 5173"], 16, color=INK)
    c.line([(1315, 420), (1560, 420)], color=INK, width=2.5)
    c.label(1463, 384, ["«JDBC» over TLS", "port 5432"], 16, color=INK)
    c.line([(1315, 500), (1463, 500), (1463, 760), (1560, 760)], color=INK, width=2.5)
    c.label(1463, 800, ["«HTTPS»", "server-side keys"], 16, color=INK)
    c.note(60, 1055, 1970, [
        "Solid line = communication path with its protocol. The browser talks only to the API; it has no database driver, key or connection string.",
        "Spotify playback and Freesound previews also stream straight to the browser, after the backend has exchanged the token or chosen the sound.",
        "Demo passwords, the JWT secret and provider keys come from application-local.yml or the environment and are never committed.",
    ], fill=NOTE)
    return c


# ---- database -----------------------------------------------------------------------------

def _table(c, x, y, name, rows, color=BLUE, w=310):
    columns = []
    for row in rows.split(";"):
        parts = row.split()
        key = parts[0] if parts[0] in ("PK", "FK", "UQ", "PF") else ""
        rest = parts[1:] if key else parts
        columns.append((("PK FK" if key == "PF" else key), rest[0], " ".join(rest[1:])))
    return c.table(x, y, w, name, columns, color)


def database_core():
    c = Canvas("database-core", "Entity-relationship diagram — compulsory schema",
        "The 17 tables behind accounts, profiles, matching settings, requests, connections, groups and notifications (crow's-foot notation).",
        width=1930, height=1670)
    _table(c, 60, 190, "matching_strategy_settings", "PK id bigint;active_strategy varchar;min_score double;config_version bigint;has_override boolean;updated_at timestamp")
    _table(c, 60, 470, "matching_enabled_criteria", "PF setting_id bigint;PK criterion varchar")
    _table(c, 60, 650, "matching_configs", "PK id bigint;UQ strategy varchar;UQ criterion varchar;weight double;updated_at timestamp")
    _table(c, 450, 190, "match_requests", "PK id bigint;FK sender_id bigint;FK receiver_id bigint;status varchar;message varchar;origin varchar;FK context_course_id bigint;context_study_goal varchar;created_at timestamp;responded_at timestamp", TEAL)
    _table(c, 450, 560, "connections", "PK id bigint;FK student_a_id bigint;FK student_b_id bigint;created_at timestamp;ended_at timestamp", TEAL)
    _table(c, 450, 810, "notifications", "PK id bigint;FK recipient_id bigint;type varchar;message text;read boolean;resource_type varchar;resource_id bigint;event_key varchar;created_at timestamp", TEAL)
    _table(c, 450, 1160, "group_join_requests", "PK id bigint;FK study_group_id bigint;FK student_id bigint;status varchar;message varchar;created_at timestamp;responded_at timestamp", TEAL)
    _table(c, 1000, 190, "users", "PK id bigint;UQ email varchar;password_hash varchar;role varchar;active boolean;token_version bigint;last_login_at timestamp;created_at timestamp", AMBER, 320)
    _table(c, 1000, 520, "students", "PF id bigint;name varchar;school varchar;programme varchar;year_of_study int;contact_number varchar;FK target_course_id bigint;preferred_study_mode varchar;preferred_group_size_min int;preferred_group_size_max int", AMBER, 320)
    _table(c, 1000, 900, "study_groups", "PK id bigint;name varchar;description varchar;FK course_id bigint;FK leader_id bigint;preferred_study_mode varchar;max_group_size int;active boolean;created_at timestamp", TEAL, 320)
    _table(c, 1000, 1250, "study_group_goals", "PF study_group_id bigint;PK study_goal varchar", TEAL, 320)
    _table(c, 1560, 190, "courses", "PK id bigint;UQ code varchar;name varchar", AMBER)
    _table(c, 1560, 420, "student_courses", "PF student_id bigint;PF course_id bigint", AMBER)
    _table(c, 1560, 600, "student_study_goals", "PF student_id bigint;PK study_goal varchar", AMBER)
    _table(c, 1560, 780, "availability_slots", "PK id bigint;FK student_id bigint;day_of_week varchar;start_time time;end_time time", AMBER)
    _table(c, 1560, 1040, "group_memberships", "PK id bigint;FK study_group_id bigint;FK student_id bigint;joined_at timestamp", TEAL)
    _table(c, 1560, 1270, "group_availability_slots", "PK id bigint;FK study_group_id bigint;day_of_week varchar;start_time time;end_time time", TEAL)
    r = c.relate
    r([(1160, 434), (1160, 520)], "1", "01")                                     # users - students
    r([(1320, 560), (1440, 560), (1440, 250), (1560, 250)], "0n", "01")          # students.target_course
    r([(1320, 600), (1480, 600), (1480, 470), (1560, 470)], "1", "0n")           # student_courses.student
    r([(1710, 314), (1710, 420)], "1", "0n")                                     # student_courses.course
    r([(1320, 650), (1560, 650)], "1", "0n")                                     # student_study_goals
    r([(1320, 760), (1440, 760), (1440, 860), (1560, 860)], "1", "0n")           # availability_slots
    r([(760, 270), (880, 270), (880, 560), (1000, 560)], "0n", "1")              # sender
    r([(760, 294), (850, 294), (850, 600), (1000, 600)], "0n", "1")              # receiver
    r([(760, 640), (1000, 640)], "0n", "1")                                      # student_a
    r([(760, 664), (880, 664), (880, 690), (1000, 690)], "0n", "1")                                      # student_b
    r([(760, 890), (880, 890), (880, 770), (1000, 770)], "0n", "1")              # notifications.recipient
    r([(680, 190), (680, 160), (1710, 160), (1710, 190)], "0n", "01")            # context_course
    r([(1100, 900), (1100, 812)], "0n", "1")                                     # leader
    r([(1320, 985), (1895, 985), (1895, 250), (1870, 250)], "0n", "1")           # study_groups.course
    r([(1560, 1100), (1320, 1100)], "0n", "1")                                   # membership.group
    r([(1560, 1070), (1400, 1070), (1400, 790), (1320, 790)], "0n", "1")         # membership.student
    r([(760, 1247), (880, 1247), (880, 1120), (1000, 1120)], "0n", "1")          # join request.group
    r([(760, 1271), (950, 1271), (950, 850), (1040, 850), (1040, 812)], "0n", "1")  # join request.student
    r([(1160, 1250), (1160, 1168)], "0n", "1")                                   # study_group_goals
    r([(1560, 1330), (1440, 1330), (1440, 1140), (1320, 1140)], "0n", "1")       # group_availability_slots
    r([(215, 470), (215, 386)], "0n", "1")                                       # enabled criteria
    for x, y, text in ((835, 254, "sender"), (812, 316, "receiver"), (880, 628, "student_a"), (880, 712, "student_b"),
                       (1165, 862, "leader"), (1195, 166, "context course (optional)"), (1440, 400, "target course"),
                       (1240, 482, "same id (@MapsId)")):
        c.label(x, y, text, 15)
    c.note(60, 1470, 1810, [
        "PK = primary key, FK = foreign key, UQ = unique. Line ends: two bars = exactly one, circle and bar = zero or one, crow's foot = many (circle = may be none).",
        "Amber = accounts and profiles, teal = requests, connections, groups and notifications, blue = matching settings.",
        "students shares its primary key with users, so an administrator is a users row with no profile. matching_configs holds one weight per (strategy, criterion) and has no foreign key.",
        "Partial unique indexes allow one pending request per pair of students, one active connection per pair, and one pending application per student and group.",
    ], fill=NOTE)
    return c


def database_extras():
    c = Canvas("database-extras", "Entity-relationship diagram — tables added by the two extras",
        "E2 collaborative study (rooms, sessions, forest, badges) and E1 AI study tools (explanation cache, study plans, quizzes).",
        width=2130, height=1500)
    t = lambda *args, **kw: _table(c, *args, w=330, **kw)
    t(60, 190, "study_rooms", "PK id bigint;FK study_group_id →study_groups (unique);FK host_id →students;FK co_host_id →students;version bigint;phase varchar;timer_status varchar;remaining_millis bigint;participant_limit int", color=TEAL)
    t(480, 190, "room_presences", "PK id bigint;FK study_room_id bigint;FK student_id →students;client_id uuid;expires_at timestamp;presence varchar", color=TEAL)
    t(480, 440, "room_reactions", "PK id bigint;FK study_room_id bigint;FK student_id →students;type varchar;sent_at timestamp", color=TEAL)
    t(60, 520, "focus_visits", "PK id bigint;FK student_id →students;FK study_group_id →study_groups;group_name varchar;started_at timestamp;last_seen_at timestamp;focused_millis bigint;ended_at timestamp", color=TEAL)
    t(60, 840, "earned_badges", "PK id bigint;FK student_id →students;badge varchar;best_tier varchar;first_earned_at timestamp", color=TEAL)
    t(480, 690, "match_explanations", "PK id bigint;UQ evidence_hash varchar(64);prompt_version varchar;model varchar;explanation_text text;created_at timestamp", color=BLUE)
    t(900, 190, "study_sessions", "PK id bigint;FK study_group_id →study_groups;FK connection_id →connections;FK created_by →users;title varchar;start_at timestamp;end_at timestamp;time_zone varchar;occurrence_count int;status varchar;version bigint", color=TEAL)
    t(1320, 190, "study_plans", "PK id bigint;scope varchar;FK student_id →students;FK session_id bigint;FK course_id →courses;FK created_by →users;first_block_date date;block_count int;exam_date date", color=AMBER)
    t(1740, 190, "study_plan_entries", "PK id bigint;FK plan_id bigint;position int;scheduled_date date;title varchar;planned_minutes int;completed boolean", color=AMBER)
    t(1740, 480, "study_plan_entry_tasks", "PF entry_id bigint;PK position int;description varchar", color=AMBER)
    t(1320, 520, "study_plan_source_files", "PF plan_id bigint;PK position int;filename varchar", color=AMBER)
    t(1320, 710, "study_plan_source_topics", "PF plan_id bigint;PK position int;title varchar", color=AMBER)
    t(900, 640, "quizzes", "PK id bigint;scope varchar;FK group_id →study_groups;FK student_id →students;FK session_id bigint;FK created_by →students;FK course_id →courses;title varchar;status varchar;closes_at timestamp", color=AMBER)
    t(1320, 890, "quiz_questions", "PK id bigint;FK quiz_id bigint;position int;prompt varchar;correct_option int;explanation varchar", color=AMBER)
    t(1740, 890, "quiz_question_options", "PF question_id bigint;PK position int;text varchar", color=AMBER)
    t(900, 1010, "quiz_attempts", "PK id bigint;FK quiz_id bigint;FK student_id →students;started_at timestamp;submitted_at timestamp;score int;total int", color=AMBER)
    t(1320, 1150, "quiz_attempt_answers", "PF attempt_id bigint;PF question_id bigint;chosen_option int", color=AMBER)
    r = c.relate
    r([(390, 260), (480, 260)], "1", "0n")                                        # room - presences
    r([(390, 400), (435, 400), (435, 510), (480, 510)], "1", "0n")               # room - reactions
    r([(1230, 300), (1320, 300)], "01", "0n")                                     # session - plans
    r([(1065, 506), (1065, 640)], "01", "0n")                                     # session - quizzes
    r([(1650, 260), (1740, 260)], "1", "0n")                                      # plan - entries
    r([(1905, 410), (1905, 480)], "1", "0n")                                      # entry - tasks
    r([(1485, 458), (1485, 520)], "1", "0n")                                      # plan - files
    r([(1320, 400), (1285, 400), (1285, 770), (1320, 770)], "1", "0n")            # plan - topics
    r([(1230, 900), (1275, 900), (1275, 950), (1320, 950)], "1", "0n")            # quiz - questions
    r([(1065, 932), (1065, 1010)], "1", "0n")                                     # quiz - attempts
    r([(1650, 950), (1740, 950)], "1", "0n")                                      # question - options
    r([(1230, 1200), (1320, 1200)], "1", "0n")                                    # attempt - answers
    r([(1485, 1086), (1485, 1150)], "1", "0n")                                    # question - answers
    c.note(60, 1090, 750, [
        "→ names a table in the compulsory schema.",
        "Those links are listed, not drawn, to keep",
        "this panel readable.",
    ], title="Reading the arrows in the type column", fill="white")
    c.note(60, 1320, 2010, [
        "Teal = collaborative study (E2) and its forest and badges; amber = AI study tools; blue = the E1 explanation cache, keyed only by an evidence hash, so it stores no student identity.",
        "study_sessions belongs to exactly one group or one connection (a database check). A plan or quiz may be attached to a session; deleting the parent cascades.",
    ], fill=NOTE)
    return c


BUILDERS = [tech_stack, package_layers, deployment, database_core, database_extras]
