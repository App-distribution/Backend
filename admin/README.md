# AppDistribution Admin

Next.js-based web admin panel for the AppDistribution backend.

## Stack

- Next.js 16
- React 19
- Tailwind CSS 4
- TanStack Query
- TanStack Table
- Recharts
- React Hook Form + Zod

## Required runtime

- Node.js 20.9+ (Next 16 requirement)

## Setup

1. Copy env:

```bash
cp .env.example .env.local
```

2. Install dependencies:

```bash
npm install
```

3. Start the admin app:

```bash
npm run dev
```

The default `dev` script uses `webpack` plus polling watchers because `next dev` with Turbopack can hit `EMFILE` watcher limits in this environment.

If you want to try Turbopack explicitly:

```bash
npm run dev:turbo
```

The app runs on `http://localhost:3000` by default and expects the backend API at `NEXT_PUBLIC_API_BASE_URL`.

## Implemented scope

- OTP login
- Overview dashboard
- Projects catalog
- Project detail with build timeline
- Builds workspace screen
- APK upload flow with progress
- Team page
- Profile page
- Audit and System placeholders wired into navigation for future backend endpoints

## Current backend gaps reflected in UI

- no public audit logs endpoint
- no analytics endpoint
- no system health endpoint
- no workspace-wide builds endpoint beyond recent feed
- `BuildDto.uploaderName` is currently empty on the backend
