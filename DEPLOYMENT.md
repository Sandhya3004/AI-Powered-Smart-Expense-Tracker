# 🚀 Deployment Guide

Complete guide for deploying Smart Expense Tracker to production.

## 📋 Architecture

- **Backend**: Spring Boot → [Render.com](https://render.com) (Docker)
- **Frontend**: React + Vite → [Vercel](https://vercel.com)
- **Database**: PostgreSQL on [Neon](https://neon.tech) or Render

---

## 1️⃣ Backend Deployment (Render)

### Prerequisites
- GitHub account with repository
- Render account (free tier available)

### Step 1: Prepare Backend

1. **Verify Dockerfile exists** in `backend/`:
```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 5050
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:5050/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${PORT:5050}", "-jar", "app.jar"]
```

2. **Verify .dockerignore exists** in `backend/`:
```
target/
node_modules/
.git
.env
*.log
.idea/
.vscode/
```

### Step 2: Push to GitHub

```bash
git add .
git commit -m "Add Docker and production config"
git push origin main
```

### Step 3: Deploy on Render

1. Go to [Render Dashboard](https://dashboard.render.com)
2. Click **New +** → **Web Service**
3. Connect your GitHub repository
4. Configure:

| Setting | Value |
|---------|-------|
| **Name** | expense-tracker-api |
| **Environment** | Docker |
| **Branch** | main |
| **Dockerfile Path** | ./backend/Dockerfile |
| **Plan** | Free |

5. **Add Environment Variables**:

```
PORT=5050
DB_URL=postgresql://your_neon_url
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
JWT_SECRET=your_random_jwt_secret_32_chars_min
FRONTEND_URL=https://your-frontend.vercel.app
SPRING_PROFILES_ACTIVE=production
```

6. Click **Create Web Service**

7. Wait for deployment (2-3 minutes)

8. Note the URL: `https://expense-tracker-api.onrender.com`

---

## 2️⃣ Frontend Deployment (Vercel)

### Step 1: Prepare Frontend

1. **Create production .env file**:
```bash
cd frontend
cp .env.example .env
```

2. **Update .env**:
```env
VITE_API_BASE_URL=https://expense-tracker-api.onrender.com/api
VITE_APP_ENV=production
VITE_APP_NAME=Smart Expense Tracker
```

3. **Verify vite.config.js** has proper base config:
```javascript
export default defineConfig({
  plugins: [react()],
  base: '/',  // Important for Vercel
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:5050',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: true
  }
})
```

### Step 2: Push to GitHub

```bash
git add .
git commit -m "Add production frontend config"
git push origin main
```

### Step 3: Deploy on Vercel

1. Go to [Vercel Dashboard](https://vercel.com/dashboard)
2. Click **Add New...** → **Project**
3. Import your GitHub repository
4. Configure:

| Setting | Value |
|---------|-------|
| **Framework** | Vite |
| **Root Directory** | frontend |
| **Build Command** | npm run build |
| **Output Directory** | dist |
| **Install Command** | npm install |

5. **Add Environment Variables**:

```
VITE_API_BASE_URL=https://expense-tracker-api.onrender.com/api
VITE_APP_ENV=production
```

6. Click **Deploy**

7. Wait for deployment (1-2 minutes)

8. Note the URL: `https://your-project.vercel.app`

---

## 3️⃣ Post-Deployment Steps

### Update CORS on Backend

Add your Vercel URL to Render environment variables:
```
FRONTEND_URL=https://your-project.vercel.app
```

Redeploy if needed.

### Test Production

1. **Visit Frontend**: https://your-project.vercel.app
2. **Test Auth**: Register / Login
3. **Test API**: Add an expense
4. **Check Console**: No CORS errors

---

## 4️⃣ Troubleshooting

### CORS Errors

**Symptom**: `Access-Control-Allow-Origin` error in console

**Fix**: 
1. Verify `FRONTEND_URL` env var on Render matches Vercel URL
2. Check `SecurityConfig.java` has proper CORS configuration
3. Restart Render service

### 401 Unauthorized

**Symptom**: Login fails with 401

**Fix**:
1. Verify `JWT_SECRET` is set and matches
2. Check token is stored in localStorage
3. Verify `/api/auth/**` is in `permitAll()`

### Database Connection Failed

**Symptom**: 500 error on API calls

**Fix**:
1. Check `DB_URL` format: `jdbc:postgresql://host:5432/dbname`
2. Verify Neon/Postgres is running
3. Check IP allowlist (add 0.0.0.0/0 for Render)

### Frontend Blank Page

**Symptom**: White screen after deploy

**Fix**:
1. Check `vite.config.js` has `base: '/'`
2. Verify build succeeds: `npm run build` locally
3. Check Vercel build logs

---

## 5️⃣ Environment Variables Reference

### Backend (Render)

| Variable | Description | Example |
|----------|-------------|---------|
| `PORT` | Server port | 5050 |
| `DB_URL` | PostgreSQL URL | jdbc:postgresql://... |
| `DB_USERNAME` | DB user | expense_user |
| `DB_PASSWORD` | DB password | secret123 |
| `JWT_SECRET` | JWT signing key | mysecretkey32chars... |
| `FRONTEND_URL` | CORS origin | https://app.vercel.app |

### Frontend (Vercel)

| Variable | Description | Example |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API URL | https://api.onrender.com/api |
| `VITE_APP_ENV` | Environment | production |

---

## 6️⃣ Useful Commands

### Backend
```bash
# Build locally
mvn clean package -DskipTests

# Run with production profile
java -jar -Dspring.profiles.active=production target/*.jar

# Docker build locally
docker build -t expense-backend .
docker run -p 5050:5050 --env-file .env expense-backend
```

### Frontend
```bash
# Development
npm run dev

# Production build
npm run build

# Preview production build
npm run preview
```

---

## 7️⃣ Free Tier Limits

| Service | Free Tier |
|---------|-----------|
| Render Web Service | 512 MB RAM, sleeps after 15 min inactivity |
| Vercel | 100 GB bandwidth, 6000 build minutes/month |
| Neon PostgreSQL | 500 MB storage, 190 compute hours/month |

---

## 🎉 Success Checklist

- [ ] Backend deployed on Render with no errors
- [ ] Frontend deployed on Vercel with no errors
- [ ] Database connected and migrations ran
- [ ] User can register/login
- [ ] CRUD operations work
- [ ] No CORS errors in console
- [ ] Charts/data loading correctly
- [ ] Logout works properly

**Your app is now live!** 🚀
