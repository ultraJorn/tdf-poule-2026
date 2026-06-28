# Tour de France Poule 2026

A Vue 3 frontend + Java (Spring Boot) backend + PostgreSQL rebuild of the original
single-file `tour-de-france-poule-2026.html` widget. That original file is left
untouched in this repo as the behavioral reference; everything in `frontend/` and
`backend/` reimplements the same rules (team budget/size, swaps, finish-order +
jersey scoring, stage previews, admin stage-result entry, backups) on a real stack.

## Layout

```
backend/    Spring Boot 3 + Maven + Spring Data JPA + PostgreSQL + Flyway
frontend/   Vue 3 + Vite + Pinia + Vue Router
docker-compose.yml   runs all three: postgres + backend + frontend
```

## Running it (Docker only -- no local Java/Maven/Node required)

Everything builds and runs inside containers. The only thing you need installed is
Docker.

```
cp .env.example .env       # adjust ports/credentials if you want
docker compose up --build
```

That's it:
- Postgres on `localhost:5432` (Flyway runs the schema migration automatically on
  backend startup)
- API on `http://localhost:8080`
- App on `http://localhost:8081`

`backend/Dockerfile` and `frontend/Dockerfile` are both multi-stage builds -- the
JDK/Maven and Node/npm only exist inside the build stages, not on your machine, and
not in the final images either (the backend's final image is just a JRE + the jar;
the frontend's is just nginx + the built static files).

To stop everything: `docker compose down` (add `-v` to also wipe the Postgres
volume and start fresh next time).

### Changing ports

If 8080/8081/5432 are already taken on your machine, set `BACKEND_PORT`,
`FRONTEND_PORT`, or `DB_PORT` in `.env` and re-run `docker compose up --build`.
The frontend image bakes the backend's URL in at *build* time (Vite env vars are
compile-time), so if you change `BACKEND_PORT` you do need `--build`, not just
`up`, for the frontend to pick it up.

## Running it without Docker (faster iteration while actively developing)

If you'd rather run the backend/frontend directly with hot-reload instead of
rebuilding containers on every change, you'll need a local JDK 21 + Maven, and
Node 18+:

1. `docker compose up -d postgres` (just the database)
2. Backend: `cd backend && mvn spring-boot:run`
3. Frontend: `cd frontend && cp .env.example .env && npm install && npm run dev`
   (the original route map image is already extracted into
   `frontend/public/route-map.jpg`; re-run `node scripts/extract-route-map.cjs` only
   if you ever regenerate it from the original HTML file)

## Notes on the port

- **Auth model**: there's no user login system, same as the original -- each poule
  has one organizer passphrase, checked server-side per request via an
  `X-Admin-Password` header (hashed with BCrypt at rest, never stored in
  plaintext, unlike the original's client-side string compare).
- **Scoring** (`ScoringService` in the backend) is computed server-side from the
  locked stage results, not trusted from the client -- the frontend's "My team"
  and "Leaderboard" tabs just display whatever the API returns.
- **Backups**: the original used a base64 blob as its *only* safety net against
  flaky storage. With a real database that's no longer the primary persistence
  mechanism, but the export/import flow is kept as a recovery/migration tool --
  exporting a poule no longer includes the admin password hash, so restoring
  asks you to set a fresh organizer passphrase.
- **"My poules" remembered list** is genuinely `localStorage` now (it was faked
  via a second pseudo-storage namespace in the original single-file version).

## Deploying to a real host

The same images this builds locally are what you'd push to a registry and run
on any container host (a VM with Docker, ECS, Cloud Run, etc.):
- `backend` needs `DB_HOST`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`/`CORS_ALLOWED_ORIGINS`
  pointed at your real Postgres and real frontend origin.
- `frontend` needs to be rebuilt with `VITE_API_BASE` pointed at wherever the
  backend is actually reachable from a user's browser (not a docker-internal
  hostname).
