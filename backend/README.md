# Backend

The Spring Boot API (Java 21, Maven). Setup, the demo data and the design are in the project
[README](../README.md); this page is a map of the folder.

```text
pom.xml, mvnw, mvnw.cmd          Maven build and wrapper (no local Maven install needed)
src/main/java/com/studybuddy/    25 feature packages, listed below
src/main/resources/
  application.yml                Every tunable setting, with defaults (docs/CONFIGURATION.md)
  db/migrations/                 Ordered SQL migrations; they own the schema (docs/DATABASE.md)
  prompts/                       Versioned prompt templates for the AI features
  demo-seed/demo-data.json       The fictional demo roster, loaded only when the seed is switched on
src/test/java/com/studybuddy/    Unit tests beside their package; HTTP tests in integration/
src/test/resources/              The test profile (application-test.yml)
```

`application-local.yml` holds your own database details and secrets. It is git-ignored and never
part of a submission; the README shows what to put in it.

## Packages

Feature packages are layered the same way, each holding what it needs of a controller, service,
entities, repository, assembler and DTOs. Controllers never touch repositories, and services
never return entities.

| Area | Packages |
| --- | --- |
| Accounts and platform | `user`, `auth`, `security`, `student`, `course`, `common` (errors, HTTP limits, locks), `seed` (demo data) |
| Matching engine | `matching` with `scoring` (the five `CriterionScorer`s), `strategy` (`MatchingStrategy`), `availability` (`TimeSlot` maths) and `config` (admin-tuned weights) |
| Requests, privacy and groups | `matchrequest`, `connection`, `profile` (`ProfileViewAssembler` decides whether a contact number is sent), `studygroup`, `notification`, `admin` |
| E1: AI study assistance | `ai` (shared `AiClient`, providers and quota), `matchexplanation`, `quiz`, `studyplan`, `assistant` (Pixel) |
| E2: collaborative study | `studyroom`, `studysession`, `weather`, `forest`, `achievement`, `music` |

Which class delivers which requirement: [requirement coverage](../docs/REQUIREMENT_COVERAGE.md).

## Commands

```bash
./mvnw spring-boot:run          # start the API on http://localhost:8080 (needs application-local.yml)
../scripts/test-backend.sh      # whole suite on a disposable PostgreSQL 17 in Docker
```

Unit tests alone, with no database, are in the [testing guide](../docs/TESTING.md#unit-tests-only-no-database).
