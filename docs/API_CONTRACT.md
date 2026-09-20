# API contract

The agreed surface between the React frontend and the Spring Boot backend.

**Agree the row before either side builds against it.** The most expensive failure available to
a three-team project is Team A's matching page calling an endpoint Team B never agreed to
build, discovered the week of the demo. Filling in a row here costs two minutes; finding out
on 10 October costs an evening.

How to use this file:

1. Whoever needs the endpoint opens a pull request adding the row, with the request and
   response shapes filled in.
2. The owning team reviews and merges it.
3. Both sides build against the merged row. A change after that is another pull request, and
   the other side gets told.

Conventions: base path `/api`, JSON in and out, `Authorization: Bearer <token>` on everything
except registration and login. Responses are DTOs, never entities. Errors use the standard
Spring problem shape. TODO: confirm and document the exact error body once Team B has the
exception handler in place.

---

## Auth and profile (Team B)

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| POST | `/api/auth/register` | TODO | TODO | Public | TODO |
| POST | `/api/auth/login` | TODO | TODO | Public | TODO |
| GET | `/api/profile/me` | none | TODO | Student | TODO |
| PUT | `/api/profile/me` | TODO | TODO | Student | TODO |
| PUT | `/api/profile/me/availability` | TODO | TODO | Student | TODO |
| GET | `/api/courses` | none | TODO | Student | TODO |

## Matching (Team A)

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| GET | `/api/matches` | TODO: **a course or a study goal is the entry point and one of the two is required**; filters (course, availability, study mode), sort, strategy and threshold are additional query parameters | TODO: ranked matches, each carrying a `MatchScore` with its per-criterion breakdown | Student | TODO |
| GET | `/api/admin/matching-config` | none | TODO: current weights, threshold, active strategy | Admin | TODO |
| PUT | `/api/admin/matching-config` | TODO | TODO | Admin | TODO |

## Requests and connections (Team C)

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| POST | `/api/match-requests` | TODO: target student id plus an **optional message** | TODO | Student | TODO |
| GET | `/api/match-requests` | TODO: incoming and outgoing | TODO | Student | TODO |
| POST | `/api/match-requests/{id}/accept` | none | TODO | Student | TODO |
| POST | `/api/match-requests/{id}/decline` | none | TODO | Student | TODO |
| GET | `/api/connections` | none | TODO: active connections | Student | TODO |
| DELETE | `/api/connections/{id}` | none | TODO: ends an active connection; the contact number must stop being visible to both sides afterwards | Student | TODO |
| GET | `/api/students/{id}/profile` | none | TODO: `PublicProfileDto` or `ConnectedProfileDto` depending on connection state | Student | TODO |
| GET | `/api/notifications` | none | TODO | Student | TODO |

## Study groups (Team C)

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| GET | `/api/groups` | TODO: browse and filter | TODO | Student | TODO |
| POST | `/api/groups` | TODO: name, description, course, study goals, preferred study mode, weekly availability, maximum group size | TODO | Student | TODO |
| GET | `/api/groups/{id}` | none | TODO | Student | TODO |
| PUT | `/api/groups/{id}` | TODO: update group information | TODO | Leader | TODO |
| POST | `/api/groups/{id}/close` | none | TODO: closes a group that is no longer active; a closed group accepts no new join requests | Leader | TODO |
| DELETE | `/api/groups/{id}/members/{studentId}` | none | TODO: leader removes a member | Leader | TODO |

### Group join requests (Team C)

Students ask to join a group; the leader accepts or rejects. This is a **second state machine**,
separate from `MatchRequest`. Do not try to reuse the same entity for both.

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| POST | `/api/groups/{id}/join-requests` | TODO: optional message | TODO | Student | TODO |
| GET | `/api/groups/{id}/join-requests` | TODO: pending requests for this group | TODO | Leader | TODO |
| POST | `/api/groups/{id}/join-requests/{requestId}/accept` | none | TODO: must respect the group's maximum size | Leader | TODO |
| POST | `/api/groups/{id}/join-requests/{requestId}/reject` | none | TODO | Leader | TODO |

## Administration (Team C)

| Method | Path | Request | Response | Auth | Status |
| ------ | ---- | ------- | -------- | ---- | ------ |
| GET | `/api/admin/users` | TODO: list and filter | TODO: must include **account status** and basic **usage information**; TODO: agree what "usage" means (last login? counts of matches, connections, groups?) | Admin | TODO |
| POST | `/api/admin/users` | TODO: create an account, including its role | TODO | Admin | TODO |
| GET | `/api/admin/users/{id}` | none | TODO: account status and usage detail | Admin | TODO |
| PUT | `/api/admin/users/{id}` | TODO: update account | TODO | Admin | TODO |
| DELETE | `/api/admin/users/{id}` | none | TODO: agree whether this is a hard delete or deactivation, and what happens to that student's connections and group memberships | Admin | TODO |

---

**Paths above are a starting proposal, not agreed.** They are here so the table has a shape to
argue with. Change them freely while the Status column still says TODO; once a row is agreed,
treat it as fixed.
