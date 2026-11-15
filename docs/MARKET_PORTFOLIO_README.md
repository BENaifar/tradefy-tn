# Market data & portfolio modules (frontend)

Built from `docs/FRONTEND_PRODUCT_PLAN.md`. The existing `README.md` is untouched.

## Run

1. Backend (from `PIM`), sandbox profile (simulated prices every 10 s, private H2 database):
   ```bash
   ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
   ```
2. Frontend: `npm install` then `npm start` → http://localhost:4200
3. Seed logins are listed in `PIM/docs/FRONTEND_PLAN.md` §0.

Daily candles / SMA need history: an admin can import a CSV (`date,ticker,open,high,low,close,volume`) in **/admin → Market data**.

## Routes (all behind `AuthGuard`)

| Path | Page |
|---|---|
| `/markets`, `/markets/:ticker` | Markets, instrument |
| `/chart`, `/chart/:ticker` | Candle chart + indicators (`/chart` → last ticker, first watchlist ticker, else TJARI) |
| `/watchlist` | Watchlist |
| `/portfolio`, `/portfolio?trade=open` | Overview (the query opens the trade ticket) |
| `/portfolio/positions` (`?position=TICKER`), `/portfolio/activity`, `/portfolio/allocation`, `/portfolio/performance` | Portfolio pages |
| `/leaderboard` | Leaderboard |

Admin features live inside `/admin` (views *Market data* and *Portfolios*). Dashboard content is on `/dashboard` Home.

## Structure

`src/app/features/market-data`, `src/app/features/portfolio`, `src/app/shared/ui` (shared components, SVG charts, styles, validators).
Each module has its own `*ApiResponse` / error type and error handler. `lightweight-charts` is imported only by `candle-chart.component.ts` (lazy chunk).

## Touch points in existing files (insertions marked `MARKET-PORTFOLIO`)

`app-routing.module.ts`, `app.module.ts`, navbar html, `profile.component.html`, `admin-dashboard.component.html/.ts`, `package.json`, `package-lock.json`.

## Tests

`npx ng test --watch=false --browsers=ChromeHeadless` (validators, both error handlers, QuoteStore merge order, chart-prefs validation).
