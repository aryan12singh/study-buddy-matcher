# Frontend

The React application (TypeScript, Vite). Setup and screenshots are in the project
[README](../README.md); this page is a map of the folder.

```text
package.json, package-lock.json   Dependencies and the commands below
vite.config.ts, tsconfig*.json    Build, test and type-check settings
.env.example                      Copy to .env; holds only the public API address, never a secret
index.html, public/               The page shell and favicon
src/
  main.tsx, App.tsx               Entry point; the router is created once in main.tsx
  routes.tsx, lazyPages.ts        The route table and the lazily loaded screens
  features/<feature>/             One folder per feature: its screens, API calls, form helpers and tests
  shared/api/                     The one API client (reads VITE_API_BASE_URL) and data hooks
  shared/auth/                    Sign-in state and role guards
  shared/components/              Reusable retro UI: RetroWindow, Button, Field, Dialog, StatePanel and more
  shared/desktop.css              The shared palette and styles
  test/                           Helpers that render the whole app for interaction tests
```

The feature folders mirror the backend packages: `auth`, `profile`, `students`, `matching`,
`matchexplanation`, `connections`, `groups`, `notifications`, `admin`, `sessions`, `studyroom`,
`music`, `forest`, `achievements`, `quiz`, `studyplan`, `assistant`, plus `home`, `landing` and
`help`. Tests sit beside the code they cover as `*.test.ts(x)`.

## Commands

```bash
npm ci           # install the locked dependencies
npm run dev      # start on http://localhost:5173 (expects the backend on port 8080)
npm test         # Vitest interaction tests
npm run lint     # oxlint
npm run build    # type check, then a production build in dist/ (git-ignored)
```

The shared components to reuse are listed in the [UI guide](../docs/UI_GUIDE.md); the rules
every screen follows (empty, loading and error states, confirmations) are in
[AGENTS.md](../AGENTS.md#rules-for-every-screen).
