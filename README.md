# TradeX — Professional Trading Platform Frontend

A high-performance, modular Angular 18 frontend designed for institutional trading desks and algorithmic traders. Built with a pure black-and-white minimalist aesthetic, GSAP micro-animations, and Lenis smooth momentum scrolling.

---

## 1. Tech Stack & Design System

- **Framework**: Angular 18 (Modular Architecture with SCSS)
- **Smooth Momentum Scrolling**: `lenis` + `LenisService` integrated directly with the GSAP ticker
- **High-Performance Animations**: `gsap` + `ScrollTrigger` (hero reveals, animated candlestick charts, scrolling ticker, metrics counter)
- **Design Tokens**:
  - Background: `#000000` (Pure Black)
  - Surface: `#080808` / `#0f0f0f` (Glassmorphism borders & dark panels)
  - Text & Accents: `#FFFFFF` (Monochromatic hierarchy)
  - Subtle Accents: `#4ade80` (Active state / Bull candles), `#f87171` (Bear candles)
  - Typography: `Syne` (Headings) + `Inter` (Trading data & body)

---

## 2. Project Architecture

```
src/app/
 ├── core/
 │    ├── guards/
 │    │    └── auth.guard.ts             # Protects /dashboard from unauthorized visitors
 │    ├── interceptors/
 │    │    └── jwt.interceptor.ts        # Appends Bearer token & handles 401 session expiry
 │    ├── models/
 │    │    └── user.model.ts             # Strongly-typed User, Auth, DTO & Role interfaces
 │    └── services/
 │         ├── auth.service.ts           # Login, Register, Forgot-Password, Reset-Password
 │         ├── user.service.ts           # Profile self-service & Admin User Directory CRUD
 │         └── lenis.service.ts          # Lenis smooth scroll linked to GSAP RAF loop
 ├── features/
 │    ├── landing/                       # Full Trading Landing Page
 │    │    ├── landing.component.html    # Hero, SVG Candlesticks, Live Marquee, Features, Stats, CTA
 │    │    ├── landing.component.scss
 │    │    └── landing.component.ts      # GSAP entrance timelines & live price fluctuations
 │    └── user/
 │         ├── login/                    # Fast sign-in with quick-fill demo buttons
 │         ├── register/                 # Account registration with live regex checklist
 │         ├── forgot-password/          # Generates secure reset token
 │         ├── reset-password/           # Password update with token verification
 │         └── profile/                  # User Dashboard & Admin CRUD Management Table
 ├── shared/
 │    └── components/
 │         ├── navbar/                   # Floating glassmorphism navbar with status pill
 │         └── footer/                   # Institutional footer with live latency counter
 ├── app-routing.module.ts               # Route definitions
 ├── app.component.html                  # Global layout + noise overlay
 └── styles.scss                         # Dark trading theme design tokens
```

---

## 3. Pages & Routing

| Route | Component | Access | Description |
| :--- | :--- | :--- | :--- |
| `/` | `LandingComponent` | Public | Hero with animated candlestick chart, ticker, features, stats, CTA |
| `/login` | `LoginComponent` | Public | Monochromatic login with quick-fill buttons for demo Admin & User |
| `/register` | `RegisterComponent` | Public | Registration with real-time regex criteria indicator |
| `/forgot-password` | `ForgotPasswordComponent` | Public | Password reset request generating 15-minute token |
| `/reset-password` | `ResetPasswordComponent` | Public | Token verification & new password setup |
| `/dashboard` | `ProfileComponent` | Authenticated | Profile settings, password change, and full Admin CRUD directory |

---

## 4. How to Run

### Step 1: Start the Spring Boot Backend (Port 8082)
From `C:\Users\PC\Desktop\PI5eme`:
```powershell
.\mvnw spring-boot:run
```
*(The backend runs on `http://localhost:8082` with PostgreSQL Neon database connected and default accounts seeded)*

### Step 2: Start the Angular Frontend (Port 4200)
From `C:\Users\PC\Desktop\pi5emeFront`:
```powershell
npm start
```
Open your browser at `http://localhost:4200`.

---

## 5. Demo Credentials

Quickly test using the pre-seeded accounts:

- **Admin Account**: `admin@esprit.tn` / `Admin@123456`
  - Has access to the **User Directory & CRUD** tab on the Dashboard.
  - Can view all users, provision new users, edit details, toggle active/disabled states, and delete accounts.
- **Regular User Account**: `user@esprit.tn` / `User@123456`
  - Can trade, view profile, update personal information, and change security credentials.
