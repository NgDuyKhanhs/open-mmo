# API Configuration Guide

## Overview
Tất cả các service trong frontend đã được cấu hình để sử dụng biến môi trường thay vì hardcode URL. Điều này cho phép bạn dễ dàng thay đổi API endpoint khi deploy lên các môi trường khác nhau (dev, staging, production).

## Cấu trúc Cấu hình

### File chính: `src/config/apiConfig.ts`
```typescript
import { API_CONFIG } from '@/config/apiConfig'

// Sử dụng như:
const authUrl = API_CONFIG.AUTH.LOGIN
const gmailUrl = API_CONFIG.GMAIL.BOT_PROMPT
```

### Biến Môi Trường

#### `.env.local` (Development)
```
VITE_API_URL=http://localhost:8080
VITE_API_BASE=/api/v1
```

#### `.env.production` (Production)
```
VITE_API_URL=https://api.yourdomain.com
VITE_API_BASE=/api/v1
```

## Cách Deploy

### 1. Development (Local)
Không cần thay đổi gì, sử dụng `.env.local` mặc định

```bash
npm run dev
```

### 2. Production (Deployment)

#### Phương pháp 1: Cập nhật `.env.production`
```bash
# Sửa file .env.production với URL của bạn
VITE_API_URL=https://api.yourdomain.com
VITE_API_BASE=/api/v1

# Build và deploy
npm run build
```

#### Phương pháp 2: Sử dụng Environment Variables (Docker/CI-CD)
```bash
# Khi chạy container hoặc CI/CD pipeline
VITE_API_URL=https://api.yourdomain.com \
VITE_API_BASE=/api/v1 \
npm run build
```

#### Phương pháp 3: Build-time Configuration
```bash
# Chỉ định biến khi build
npm run build -- --mode production
```

Sau đó tạo file `.env.production`:
```
VITE_API_URL=https://api.yourdomain.com
VITE_API_BASE=/api/v1
```

## Các Service Được Cập Nhật

### 1. Authentication Service (`src/services/authService.ts`)
- Register
- Login  
- Google Login
- Refresh Token
- Get Profile
- Change Password
- Logout

### 2. Gmail Service (`src/services/gmailService.ts`)
- Connect Gmail
- Handle OAuth Callback
- Get Gmail Status
- Enable/Disable Bot
- Update Bot Config
- Update Custom Prompt
- Get Mailbox

### 3. Email AI Bot View (`src/views/EmailAiBotView.vue`)
- Load Custom Prompt

## API Endpoints Configuration

```typescript
API_CONFIG = {
  // Base URLs
  BASE_URL: string       // e.g., "https://api.yourdomain.com"
  BASE_PATH: string      // e.g., "/api/v1"
  
  // Auth Endpoints
  AUTH: {
    BASE: string
    LOGIN: string
    LOGOUT: string
    REGISTER: string
    GOOGLE_LOGIN: string
    REFRESH_TOKEN: string
    PROFILE: string
    CHANGE_PASSWORD: string
  }
  
  // Gmail Endpoints
  GMAIL: {
    BASE: string
    CONNECT_START: string
    CONNECT_CALLBACK: string
    STATUS: string
    BOT_ENABLE: string
    BOT_DISABLE: string
    BOT_CONFIG: string
    BOT_PROMPT: string
    MAILBOX: string
  }
}
```

## Docker Deployment Example

```dockerfile
# Dockerfile.frontend
FROM node:18-alpine AS builder

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# Build with environment variables
ARG VITE_API_URL
ARG VITE_API_BASE
ARG VITE_GOOGLE_CLIENT_ID

ENV VITE_API_URL=$VITE_API_URL
ENV VITE_API_BASE=$VITE_API_BASE
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID

COPY . .
RUN npm run build

# Production image
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Build Docker Image
```bash
docker build \
  --build-arg VITE_API_URL=https://api.yourdomain.com \
  --build-arg VITE_API_BASE=/api/v1 \
  --build-arg VITE_GOOGLE_CLIENT_ID=your-client-id \
  -t openmmo-frontend:latest .
```

## Environment Variable Reference

| Variable | Default | Mô tả |
|----------|---------|-------|
| VITE_API_URL | http://localhost:8080 | URL base của backend API |
| VITE_API_BASE | /api/v1 | Path base cho các endpoint |
| VITE_GOOGLE_CLIENT_ID | (required) | Google OAuth Client ID |

## Testing Configuration

Để test với các URL khác nhau:

```bash
# Test với URL khác
VITE_API_URL=http://staging-api.example.com npm run dev

# Build cho production
VITE_API_URL=https://api.yourdomain.com npm run build
```

## Lưu Ý Quan Trọng

1. **CORS Configuration**: Đảm bảo backend đã cấu hình CORS để chấp nhận request từ domain frontend của bạn
2. **HTTPS**: Sử dụng HTTPS cho production
3. **API Path**: Kiểm tra lại API path của backend (mặc định là `/api/v1`)
4. **Environment Variables**: Luôn giữ `.env.local` và `.env.production` khác nhau

## Troubleshooting

### 1. API không tìm thấy
```
Kiểm tra:
- VITE_API_URL đúng không?
- VITE_API_BASE đúng không?
- Backend server đang chạy không?
```

### 2. CORS Error
```
Kiểm tra:
- Backend có cấu hình CORS đúng không?
- Domain của frontend có được whitelist không?
```

### 3. Biến môi trường không được nhận
```
Kiểm tra:
- Tên biến có bắt đầu bằng VITE_ không?
- Build lại: npm run build
- Kiểm tra .env file ở thư mục gốc
```

