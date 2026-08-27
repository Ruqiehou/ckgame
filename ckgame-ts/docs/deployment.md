# 部署文档

本文档提供 CKGame 项目的部署指南，涵盖开发环境、生产环境、容器化部署等场景。

## 📋 目录

- [环境要求](#环境要求)
- [开发环境部署](#开发环境部署)
- [生产环境部署](#生产环境部署)
- [容器化部署](#容器化部署)
- [云平台部署](#云平台部署)
- [监控与日志](#监控与日志)
- [故障排除](#故障排除)

---

## 环境要求

### 系统要求
- **操作系统**: Linux, macOS, Windows
- **Node.js**: 18.0 或更高版本
- **内存**: 最少 512MB，推荐 2GB+
- **存储**: 最少 1GB 可用空间

### 依赖要求
```bash
# Node.js 18.0+
node --version  # 应该 >= 18.0.0

# npm 9.0+
npm --version   # 应该 >= 9.0.0

# Git
git --version   # 用于版本控制
```

---

## 开发环境部署

### 1. 克隆项目
```bash
# 克隆仓库
git clone https://github.com/your-username/ckgame-ts.git
cd ckgame-ts

# 检出开发分支
git checkout develop
```

### 2. 安装依赖
```bash
# 安装项目依赖
npm install

# 安装开发依赖
npm install --dev

# 验证安装
npm run type-check
npm run build
```

### 3. 配置环境变量
```bash
# 创建环境变量文件
cp .env.example .env

# 编辑环境变量
nano .env
```

`.env.example` 文件内容：
```env
# 应用配置
NODE_ENV=development
PORT=3000
HOST=localhost

# 游戏配置
GAME_SPEED=1
SAVE_INTERVAL=300000  # 5分钟

# 日志配置
LOG_LEVEL=debug
LOG_FILE=./logs/development.log

# 数据库配置（如果使用）
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ckgame_dev
DB_USER=devuser
DB_PASSWORD=devpass

# 缓存配置
CACHE_TTL=3600  # 1小时
CACHE_SIZE=1000
```

### 4. 启动开发服务器
```bash
# 启动开发服务器
npm run dev

# 或者启动带热重载的开发服务器
npm run watch
```

### 5. 运行测试
```bash
# 运行所有测试
npm test

# 运行特定测试
npm test -- --grep "Character"

# 运行测试覆盖率
npm run test:coverage

# 持续测试模式
npm run test:watch
```

### 6. 开发工具配置

#### VS Code 配置
创建 `.vscode/settings.json`：
```json
{
  "typescript.preferences.importModuleSpecifier": "relative",
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true
  },
  "files.associations": {
    "*.ts": "typescript"
  }
}
```

#### ESLint 配置
```json
{
  "extends": [
    "eslint:recommended",
    "@typescript-eslint/recommended"
  ],
  "parser": "@typescript-eslint/parser",
  "parserOptions": {
    "ecmaVersion": 2022,
    "sourceType": "module"
  },
  "rules": {
    "no-unused-vars": "warn",
    "no-console": "warn"
  }
}
```

---

## 生产环境部署

### 1. 构建生产版本
```bash
# 清理构建目录
npm run clean

# 构建生产版本
npm run build

# 验证构建
npm run type-check
npm test
```

### 2. 配置生产环境变量
```bash
# 创建生产环境变量文件
cp .env.example .env.production

# 编辑生产环境变量
nano .env.production
```

`.env.production` 文件内容：
```env
# 应用配置
NODE_ENV=production
PORT=3000
HOST=0.0.0.0

# 游戏配置
GAME_SPEED=1
SAVE_INTERVAL=600000  # 10分钟

# 日志配置
LOG_LEVEL=info
LOG_FILE=/var/log/ckgame/app.log

# 数据库配置
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ckgame_prod
DB_USER=ckgame_user
DB_PASSWORD=secure_password

# 缓存配置
CACHE_TTL=3600  # 1小时
CACHE_SIZE=5000

# 安全配置
JWT_SECRET=your_jwt_secret_here
API_KEY=your_api_key_here
```

### 3. 使用 PM2 部署

#### 安装 PM2
```bash
# 全局安装 PM2
npm install -g pm2

# 创建 PM2 配置文件
cat > ecosystem.config.js << EOF
module.exports = {
  apps: [{
    name: 'ckgame',
    script: 'dist/ui/Main.js',
    instances: 1,
    exec_mode: 'cluster',
    env: {
      NODE_ENV: 'production',
      PORT: 3000
    },
    error_file: '/var/log/ckgame/error.log',
    out_file: '/var/log/ckgame/out.log',
    log_file: '/var/log/ckgame/combined.log',
    time: true
  }]
};
EOF
```

#### 启动应用
```bash
# 启动应用
pm2 start ecosystem.config.js

# 查看应用状态
pm2 status

# 查看日志
pm2 logs

# 停止应用
pm2 stop ckgame

# 重启应用
pm2 restart ckgame

# 删除应用
pm2 delete ckgame
```

### 4. 使用 Nginx 反向代理

#### 安装 Nginx
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nginx

# CentOS/RHEL
sudo yum install nginx
```

#### 配置 Nginx
创建 `/etc/nginx/sites-available/ckgame`：
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 重定向到 HTTPS（可选）
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    # SSL 配置
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    
    # 静态文件
    location / {
        root /var/www/ckgame/dist;
        try_files $uri $uri/ /index.html;
    }
    
    # API 代理
    location /api/ {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
    
    # 日志
    access_log /var/log/nginx/ckgame.access.log;
    error_log /var/log/nginx/ckgame.error.log;
}
```

#### 启用配置
```bash
# 创建符号链接
sudo ln -s /etc/nginx/sites-available/ckgame /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

---

## 容器化部署

### 1. Docker 基础镜像

创建 `Dockerfile`：
```dockerfile
# 多阶段构建
FROM node:18-alpine AS builder

# 设置工作目录
WORKDIR /app

# 复制 package.json 和 package-lock.json
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production

# 复制源代码
COPY . .

# 构建应用
RUN npm run build

# 生产镜像
FROM node:18-alpine AS production

# 安装必要的系统依赖
RUN apk add --no-cache \
    dumb-init \
    && rm -rf /var/cache/apk/*

# 设置工作目录
WORKDIR /app

# 创建非 root 用户
RUN addgroup -g 1001 -S nodejs
RUN adduser -S ckgame -u 1001

# 复制构建产物
COPY --from=builder --chown=ckgame:nodejs /app/dist ./dist
COPY --from=builder --chown=ckgame:nodejs /app/node_modules ./node_modules
COPY --from=builder --chown=ckgame:nodejs /app/package.json ./package.json

# 创建日志目录
RUN mkdir -p /var/log/ckgame && chown ckgame:nodejs /var/log/ckgame

# 切换到非 root 用户
USER ckgame

# 暴露端口
EXPOSE 3000

# 启动命令
ENTRYPOINT ["dumb-init", "--"]
CMD ["node", "dist/ui/Main.js"]
```

### 2. Docker Compose

创建 `docker-compose.yml`：
```yaml
version: '3.8'

services:
  ckgame:
    build: .
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - PORT=3000
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=ckgame_prod
      - DB_USER=ckgame_user
      - DB_PASSWORD=${DB_PASSWORD}
    volumes:
      - ./data:/app/data
      - ./logs:/var/log/ckgame
    depends_on:
      - postgres
    restart: unless-stopped

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=ckgame_prod
      - POSTGRES_USER=ckgame_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    ports:
      - "5432:5432"
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - ckgame
    restart: unless-stopped

volumes:
  postgres_data:
```

### 3. 构建和运行

#### 构建镜像
```bash
# 构建镜像
docker build -t ckgame:latest .

# 使用 Docker Compose 构建
docker-compose build
```

#### 运行容器
```bash
# 运行单个容器
docker run -d -p 3000:3000 --name ckgame ckgame:latest

# 使用 Docker Compose 运行
docker-compose up -d

# 查看日志
docker logs ckgame
docker-compose logs -f ckgame
```

#### 管理容器
```bash
# 查看容器状态
docker ps
docker-compose ps

# 停止容器
docker stop ckgame
docker-compose down

# 重启容器
docker restart ckgame
docker-compose restart
```

---

## 云平台部署

### 1. AWS 部署

#### 使用 AWS Elastic Beanstalk
```bash
# 安装 EB CLI
pip install awsebcli

# 初始化 EB
eb init

# 创建环境
eb create production

# 部署
eb deploy
```

#### 使用 AWS ECS
```json
# task-definition.json
{
  "family": "ckgame",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "256",
  "memory": "512",
  "executionRoleArn": "arn:aws:iam::123456789012:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "ckgame",
      "image": "your-account.dkr.ecr.region.amazonaws.com/ckgame:latest",
      "portMappings": [
        {
          "containerPort": 3000,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "NODE_ENV",
          "value": "production"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/ckgame",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### 2. Google Cloud 部署

#### 使用 Google Cloud Run
```bash
# 构建并部署到 Cloud Run
gcloud builds submit --tag gcr.io/PROJECT-ID/ckgame
gcloud run deploy ckgame \
  --image gcr.io/PROJECT-ID/ckgame \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

### 3. Azure 部署

#### 使用 Azure App Service
```bash
# 安装 Azure CLI
npm install -g azure-cli

# 登录 Azure
az login

# 创建资源组
az group create --name ckgame-rg --location eastus

# 创建 App Service
az webapp create \
  --resource-group ckgame-rg \
  --plan ckgame-plan \
  --name ckgame-app \
  --runtime "NODE:18-lts"

# 部署代码
az webapp deploy \
  --resource-group ckgame-rg \
  --name ckgame-app \
  --src-path .
```

---

## 监控与日志

### 1. 日志管理

#### 使用 Winston 日志库
```typescript
// src/utils/logger.ts
import winston from 'winston';

const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.combine(
    winston.format.timestamp(),
    winston.format.errors({ stack: true }),
    winston.format.json()
  ),
  defaultMeta: { service: 'ckgame' },
  transports: [
    new winston.transports.File({ filename: 'error.log', level: 'error' }),
    new winston.transports.File({ filename: 'combined.log' })
  ]
});

if (process.env.NODE_ENV !== 'production') {
  logger.add(new winston.transports.Console({
    format: winston.format.simple()
  }));
}

export default logger;
```

#### 日志轮转
```bash
# 安装 logrotate
sudo apt install logrotate

# 创建 logrotate 配置
sudo nano /etc/logrotate.d/ckgame

# 配置内容
/var/log/ckgame/*.log {
    daily
    missingok
    rotate 7
    compress
    delaycompress
    notifempty
    copytruncate
}
```

### 2. 性能监控

#### 使用 PM2 监控
```bash
# 安装 PM2 监控
pm2 install pm2-web

# 启动监控
pm2 web

# 查看监控信息
pm2 monit
```

#### 使用 APM 工具
```bash
# 安置 New Relic
npm install newrelic

# 配置 New Relic
# newrelic.js
module.exports = {
  app_name: 'CKGame',
  license_key: 'your_license_key',
  logging: {
    enabled: true
  }
};
```

### 3. 健康检查

#### 添加健康检查端点
```typescript
// src/api/health.ts
import { Router } from 'express';

const router = Router();

router.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    memory: process.memoryUsage(),
    version: process.env.npm_package_version
  });
});

export default router;
```

---

## 故障排除

### 1. 常见问题

#### 内存不足
```bash
# 检查内存使用
free -h

# 增加交换空间
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 永久启用交换
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

#### 端口占用
```bash
# 查找占用端口的进程
lsof -i :3000

# 杀死进程
kill -9 <PID>

# 或者更改端口
export PORT=3001
```

#### 依赖问题
```bash
# 清理缓存
npm cache clean --force

# 删除 node_modules
rm -rf node_modules package-lock.json

# 重新安装
npm install
```

### 2. 性能优化

#### 优化构建
```bash
# 使用并行构建
npm install --save-dev thread-loader
npm run build:parallel
```

#### 优化内存使用
```typescript
// 使用对象池
class ObjectPool<T> {
  private pool: T[] = [];
  
  acquire(): T {
    return this.pool.pop() || this.create();
  }
  
  release(obj: T): void {
    this.pool.push(obj);
  }
  
  protected create(): T {
    throw new Error('Must implement create method');
  }
}
```

### 3. 备份与恢复

#### 数据备份
```bash
# 备份脚本
#!/bin/bash
BACKUP_DIR="/backup/ckgame"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p $BACKUP_DIR

# 备份数据
tar -czf $BACKUP_DIR/ckgame_$DATE.tar.gz \
  /var/log/ckgame \
  /var/www/ckgame/data \
  /var/lib/postgresql/data

# 保留最近7天的备份
find $BACKUP_DIR -name "ckgame_*.tar.gz" -mtime +7 -delete
```

#### 数据恢复
```bash
# 恢复数据
tar -xzf /backup/ckgame/ckgame_20240101_120000.tar.gz

# 恢复数据库
pg_restore -d ckgame_prod /backup/ckgame/ckgame_20240101_120000.sql
```

---

## 安全配置

### 1. SSL/TLS 配置

#### 生成 SSL 证书
```bash
# 使用 Let's Encrypt
sudo apt install certbot
sudo certbot --nginx -d your-domain.com
```

#### 配置 HTTPS
```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    
    # 安全头
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
}
```

### 2. 环境变量安全

```bash
# 设置文件权限
chmod 600 .env
chmod 600 .env.production

# 使用密钥管理服务
# AWS Secrets Manager
# Azure Key Vault
# Google Cloud Secret Manager
```

---

## 性能调优

### 1. Node.js 优化

#### 调整内存限制
```bash
# 增加 Node.js 内存限制
node --max-old-space-size=4096 dist/ui/Main.js

# 使用 PM2 设置内存限制
pm2 start ecosystem.config.js --max-memory-restart 400M
```

#### 优化事件循环
```typescript
// 使用 setImmediate 替代 setTimeout
setImmediate(() => {
  // 处理密集型任务
});

// 使用 worker_threads 处理 CPU 密集型任务
import { Worker, isMainThread, parentPort, workerData } from 'worker_threads';
```

### 2. 数据库优化

#### PostgreSQL 优化
```sql
-- 创建索引
CREATE INDEX idx_characters_dynasty ON characters(dynasty_id);
CREATE INDEX idx_counties_holder ON counties(holder_id);

-- 优化查询
EXPLAIN ANALYZE SELECT * FROM characters WHERE dynasty_id = 1;
```

#### 连接池配置
```typescript
// 数据库连接池配置
const pool = new Pool({
  host: process.env.DB_HOST,
  port: parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  max: 20,
  min: 5,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});
```

---

## 部署清单

### 部署前检查
- [ ] 代码已构建完成
- [ ] 所有测试通过
- [ ] 环境变量已配置
- [ ] SSL 证书已配置
- [ ] 备份已创建
- [ ] 监控已设置

### 部署后验证
- [ ] 应用正常启动
- [ ] 端口正确监听
- [ ] 数据库连接正常
- [ ] 日志正常记录
- [ ] 性能指标正常
- [ ] 健康检查通过

---

## 获取支持

如有部署问题，请参考：
- [开发指南](development.md)
- [API 文档](api.md)
- [问题反馈](https://github.com/your-username/ckgame-ts/issues)
- [社区论坛](https://forum.ckgame.com)