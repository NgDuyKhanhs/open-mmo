# OpenMMO AI - Docker Deployment Guide

## 📋 Prerequisites

- Docker (version 20.10+)
- Docker Compose (version 2.0+)

## ⚙️ Environment Configuration

Tạo file `.env` trong thư mục root (D:\TAI VE\ai) với các biến môi trường sau:

```env
# ===== REQUIRED =====
# JWT Secret (512-bit base64 encoded)
JWT_SECRET=dk1qrTUJeUPfeJaog/lsC7P9i27rla7514968Ib0v61mEv7t15gp1/Bvo6WSjrqyfBsTednLTrMg8VvqKLCRxw==

# Google OAuth2 Configuration
GOOGLE_CLIENT_ID=your_google_client_id_here
GOOGLE_CLIENT_SECRET=your_google_client_secret_here
GOOGLE_REDIRECT_URI=http://yourdomain.com/api/v1/auth/google-callback

# Token Encryption Key (256-bit base64 encoded)
TOKEN_ENC_KEY_BASE64=your_base64_encoded_256bit_key_here

# ===== OPTIONAL (API Keys) =====
OPENAI_API_KEY=your_openai_key_here
GEMINI_API_KEY=your_gemini_key_here

# ===== OPTIONAL (Custom URLs) =====
APP_WEB_URL=http://yourdomain.com
CORS_ORIGINS=http://yourdomain.com,http://api.yourdomain.com
VITE_API_URL=http://yourdomain.com/api
VITE_GOOGLE_CLIENT_ID=your_google_client_id_here
```

## 🚀 Quick Start

### 1. Build Images

```bash
# Build all images
docker-compose build

# Or build specific services
docker-compose build backend
docker-compose build frontend
```

### 2. Start Services

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mongodb
```

### 3. Access Application

- **Frontend:** http://localhost or http://localhost:80
- **Backend API:** http://localhost:8080
- **MongoDB:** localhost:27017

## 📦 Service Details

### Backend Service
- **Port:** 8080
- **Image:** Built from `Dockerfile.backend`
- **Dependencies:** MongoDB
- **Environment Variables:** See docker-compose.yml

### Frontend Service
- **Port:** 80
- **Image:** Built from `frontend/Dockerfile`
- **Reverse Proxy:** Nginx (routes /api to backend)
- **Features:**
  - Static file caching
  - Gzip compression
  - API proxy to backend

### MongoDB Service
- **Port:** 27017
- **Image:** mongo:7.0
- **Data Volume:** `mongodb_data` (persisted)
- **Default Database:** openmmo

## 🛑 Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (CAREFUL - deletes database)
docker-compose down -v
```

## 🔄 Useful Commands

```bash
# Restart a service
docker-compose restart backend

# Rebuild and restart
docker-compose up -d --build backend

# Execute command in container
docker-compose exec backend bash
docker-compose exec frontend sh

# Check service status
docker-compose ps

# View resource usage
docker stats

# Prune unused images/volumes
docker system prune -a
```

## 🐛 Troubleshooting

### Backend won't start
```bash
# Check logs
docker-compose logs backend

# Verify MongoDB connection
docker-compose exec backend curl http://mongodb:27017
```

### Frontend shows blank page
```bash
# Check nginx logs
docker-compose logs frontend

# Verify API connectivity
docker-compose exec frontend curl http://backend:8080/api/v1/health
```

### Port already in use
```bash
# Change ports in docker-compose.yml or use:
docker-compose up -d -p custom_project_name
```

## 📝 Production Deployment

For production, consider:

1. Use environment-specific docker-compose files:
   ```bash
   docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
   ```

2. Use a reverse proxy (Nginx, Traefik)

3. Enable HTTPS/SSL certificates

4. Use secrets management (Docker Secrets, Vault)

5. Set up monitoring (Prometheus, Grafana)

6. Configure logging (ELK Stack, Splunk)

7. Use private Docker registry

## 🔐 Security Tips

- Never commit `.env` to git
- Use strong JWT_SECRET and TOKEN_ENC_KEY_BASE64
- Rotate credentials regularly
- Use HTTPS in production
- Implement rate limiting
- Use network policies
- Run containers as non-root user

## 📚 Docker Best Practices

- Keep images small (multi-stage builds)
- Use specific base image versions
- Scan images for vulnerabilities
- Use health checks
- Set resource limits
- Use named volumes for data persistence
- Implement proper logging

