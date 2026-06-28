# Deploying to Railway

This walks through getting the app live for other people to join, using Railway
(railway.app) connected to this GitHub repo. Pushing to `main` later will
auto-redeploy everything.

## 1. Push this repo to GitHub

This was already done locally (`git init`, committed). To push it up:

1. Go to https://github.com/new, name it (e.g. `tdf-poule-2026`), set it to
   **Private**, and do **not** check "Add a README" / .gitignore / license
   (this repo already has content -- an empty repo avoids a merge conflict).
2. GitHub will show you a remote URL on the next page. Run, from this folder:
   ```
   git remote add origin <the URL GitHub gave you>
   git push -u origin main
   ```
   If this is the first time you've pushed from this machine, a browser window
   may pop up asking you to log into GitHub -- that's Git Credential Manager,
   already installed; just approve it.

## 2. Create the Railway project

1. Sign up / log in at https://railway.app (the "Login with GitHub" option is
   easiest since you'll need to grant repo access anyway).
2. **New Project -> Deploy from GitHub repo** -> pick the repo you just pushed.
3. Railway will try to auto-detect a service from the repo root. Delete that
   first auto-created service once the project exists -- you're going to add
   three services manually instead, each pointed at the right subfolder.

## 3. Add the Postgres database

**New -> Database -> Add PostgreSQL.** Railway provisions it and exposes
connection details as variables (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`,
`PGPASSWORD`) that other services in the same project can reference.

## 4. Add the backend service

**New -> GitHub Repo** -> same repo again.
- **Settings -> Root Directory**: `backend`  (so it builds `backend/Dockerfile`)
- **Settings -> Networking -> Generate Domain** -> note the URL it gives you,
  e.g. `tdf-poule-backend.up.railway.app`. That's your API's public address.
- **Variables**, add:
  ```
  DB_HOST=${{Postgres.PGHOST}}
  DB_PORT=${{Postgres.PGPORT}}
  DB_NAME=${{Postgres.PGDATABASE}}
  DB_USER=${{Postgres.PGUSER}}
  DB_PASSWORD=${{Postgres.PGPASSWORD}}
  SERVER_PORT=${{PORT}}
  CORS_ALLOWED_ORIGINS=https://<your-frontend-domain-from-step-5>
  SITE_ADMIN_SECRET=<a long random string -- generate one, e.g. `openssl rand -hex 32`>
  ```
  (`${{Postgres.PGHOST}}` etc. reference the database service's variables --
  Railway autocompletes these. `${{PORT}}` is the port Railway assigns this
  service at runtime. `SITE_ADMIN_SECRET` is optional -- see "Cleaning up
  poules you've lost the code to" below -- but worth setting now while
  you're already in here.)
- You'll come back and fix `CORS_ALLOWED_ORIGINS` once you know the frontend's
  real domain from step 5 -- it's fine to leave it blank or guess for now and
  edit it after.

## 5. Add the frontend service

**New -> GitHub Repo** -> same repo again.
- **Settings -> Root Directory**: `frontend`
- **Settings -> Networking -> Generate Domain** -> this is the URL you'll give
  your friends, e.g. `tdf-poule.up.railway.app`.
- **Variables** tab -> add a normal variable:
  ```
  VITE_API_BASE=https://<your-backend-domain-from-step-4>
  ```
  There's no separate "build-time variables" section to find -- for
  Dockerfile-based services, Railway just looks for an `ARG` in the Dockerfile
  matching a variable name (our `frontend/Dockerfile` already declares
  `ARG VITE_API_BASE`) and passes it in as a build arg automatically. This
  has to be the backend's *public* domain (the one a browser can reach), not
  an internal Railway hostname -- Vite bakes it into the JS at build time, so
  redeploy after changing it.

No `PORT` variable needs setting here -- the Dockerfile's nginx template reads
Railway's auto-injected `PORT` automatically (see `frontend/nginx.conf.template`).

## 6. Wire the CORS setting back up

Now that both domains exist: go back to the **backend** service's variables
and make sure `CORS_ALLOWED_ORIGINS` is set to the frontend's actual
`https://...up.railway.app` URL (comma-separate if you later add a custom
domain too). Redeploy the backend service for the change to take effect.

## 7. Try it

Open the frontend's URL. Create a poule, share the join code, have a friend
open the same URL on their own device and join with it.

## Later: a custom domain

Once you're happy with it, buy a domain (Namecheap/Cloudflare/etc.) and in the
frontend service's **Settings -> Networking -> Custom Domain**, add it and
follow Railway's DNS instructions (a CNAME record, usually). Do the same for
the backend if you want a clean API subdomain, and update
`CORS_ALLOWED_ORIGINS` / `VITE_API_BASE` to match (the latter needs a rebuild
since it's baked in at build time).

## Cleaning up poules you've lost the code to

Each poule's Admin tab is gated by *that poule's own* organizer passphrase --
useless if you made a test poule and didn't keep the code or password. If you
set `SITE_ADMIN_SECRET` on the backend (step 4), visit:

```
https://<your-frontend-domain>/site-admin
```

It's not linked from anywhere in the normal UI -- you just navigate there
directly. Paste in the `SITE_ADMIN_SECRET` value and you'll see every poule on
the deployment (code, name, created date, stage, rider/team counts) with a
delete button each, no per-poule password needed. Treat that secret like a
master key: anyone who has it can delete *any* poule on the site, not just
test ones.

## Ongoing cost

Railway bills by usage (CPU/RAM/network), not a flat monthly fee. For a small
group of friends checking in occasionally during the Tour, this should be a
few dollars a month at most -- check Railway's current pricing page for exact
numbers since it does change.
