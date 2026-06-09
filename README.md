# 分布式爬虫数据采集系统

## 系统概述

基于 Spring Boot + Vue 3 的分布式爬虫数据采集平台，支持多节点并行采集、Cookie 弹窗自动处理、MHTML 页面快照保存。

**技术栈：**
- 后端：Spring Boot 4 + MyBatis-Plus + MySQL + RabbitMQ + JWT
- 爬虫节点：Playwright + Firecrawl API + Chrome DevTools Protocol
- 前端：Vue 3 + TypeScript + Element Plus + Tailwind CSS + ECharts

---

## 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Java 运行环境 |
| Maven | 3.8+ | 后端构建工具（或使用 Maven Wrapper） |
| Node.js | 22+ | 前端运行环境 |
| MySQL | 8.0+ | 数据库（端口 3306） |
| RabbitMQ | 3.x | 消息队列（端口 5672） |
| Playwright | 1.48 | 浏览器自动化（首次运行自动安装 Chromium） |

---

## 目录结构

```
DataCollectSystem/
├── server/                    # Spring Boot 服务端（调度中心 + API）
│   ├── src/main/java/
│   │   └── com/example/server/
│   │       ├── config/        # 配置（CORS、Security、RabbitMQ、MyBatis）
│   │       ├── controller/    # 20+ API 控制器
│   │       ├── service/       # 业务服务层
│   │       ├── mapper/        # MyBatis 数据访问层
│   │       ├── entity/        # 数据库实体 + DTO + 消息体
│   │       ├── Listener/      # RabbitMQ 消息监听器
│   │       └── common/util/   # JWT、Security 工具类
│   └── src/main/resources/
│       └── application.properties
│
├── crawlernode/               # 爬虫工作节点（可多实例部署）
│   ├── src/main/java/
│   │   └── com/example/crawlernode/
│   │       ├── crawler/       # 爬虫引擎（Playwright + MHTML）
│   │       ├── service/       # Firecrawl 链接发现
│   │       ├── website/       # 11 个站点的链接过滤规则
│   │       ├── config/        # RabbitMQ 配置
│   │       └── entity/        # 消息实体
│   └── src/main/resources/
│       └── application.properties
│
├── font/                      # Vue 3 前端
│   ├── src/
│   │   ├── api/               # API 接口层 + axios 配置
│   │   ├── router/            # 路由 + 导航守卫
│   │   ├── views/             # 17 个页面组件
│   │   ├── types/             # TypeScript 类型定义
│   │   └── utils/             # Token 管理、工具函数
│   └── package.json
│
├── shared/                    # 爬虫节点与服务器共享文件目录
│   └── crawl-files/           # MHTML 页面快照存储（按 taskId 分目录）
│
├── db_export/                 # 数据库导出文件
│   └── dcsysdb.sql            # 完整数据库结构与数据
│
└── README.md                  # 本文件
```

---

## 配置说明

### 1. 数据库配置

确保 MySQL 运行在 `localhost:3306`，创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS dcsysdb DEFAULT CHARACTER SET utf8mb4;
```

导入数据库（含表结构 + 示例数据）：

```bash
mysql -u root dcsysdb < db_export/dcsysdb.sql
```

默认使用 root 无密码登录，如需修改，编辑 `server/src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dcsysdb
spring.datasource.username=root
spring.datasource.password=你的密码
```

### 2. RabbitMQ 配置

确保 RabbitMQ 运行在 `localhost:5672`，默认账号 guest/guest。

如需修改，同时编辑 `server/` 和 `crawlernode/` 的 `application.properties`：

```properties
spring.rabbitmq.host=127.0.0.1
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

### 3. 爬虫节点配置

编辑 `crawlernode/src/main/resources/application.properties`：

```properties
# 每个节点必须有唯一的 node.id
node.id=node-1
node.name=node-1
node.max-concurrency=1          # 并发任务数

# MHTML 存储路径
crawler.storage.base-dir=/your/path/shared/crawl-files

# Firecrawl API Key（链接发现服务）
firecrawl.api-key=你的API密钥
```

**多节点部署**：复制一份 crawlernode，修改 `node.id` 和 `server.port` 即可。

### 4. 前端配置

编辑 `font/src/api/axiosConfig.ts`：

```typescript
export const API_BASE_URL = 'http://localhost:8080'  // 后端地址
```

Vite 开发代理已在 `vite.config.ts` 中配置，开发时自动转发。

---

## 启动步骤

按以下顺序依次启动：

### 第一步：启动基础服务

```bash
# 启动 RabbitMQ（如未运行）
brew services start rabbitmq           # macOS
# 或 systemctl start rabbitmq-server  # Linux

# 启动 MySQL（如未运行）
brew services start mysql              # macOS
```

### 第二步：启动 Server（调度中心）

```bash
cd server
./mvnw spring-boot:run                 # 或 mvn spring-boot:run
```

启动后访问：
- API 地址：`http://localhost:8080`
- Swagger 文档：`http://localhost:8080/swagger-ui/index.html`

### 第三步：启动 CrawlerNode（爬虫节点）

```bash
cd crawlernode
./mvnw spring-boot:run
```

默认端口 8090。如需启动多个节点：

```bash
# 节点 1
cd crawlernode && ./mvnw spring-boot:run

# 节点 2（新终端，修改端口和 node.id）
cd crawlernode
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8091,--node.id=node-2
```

### 第四步：启动前端

```bash
cd font
npm install        # 首次运行需安装依赖
npm run dev        # 开发模式
```

访问 `http://localhost:5173`

---

## 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端用户端 | http://localhost:5173 | Vue 3 界面 |
| 后端 API | http://localhost:8080 | Spring Boot 服务 |
| Swagger 文档 | http://localhost:8080/swagger-ui/index.html | 接口文档 |
| RabbitMQ 管理 | http://localhost:15672 | 消息队列监控（guest/guest） |

### 默认账号

执行 SQL 导入后，系统默认包含管理员账号：

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 管理员 | admin@example.com | 123456 |

---

## 支持的目标站点

1. 百度百科 (baike.baidu.com)
2. Bing (bing.com)
3. 央视新闻 (cctv.com)
4. 中国新闻网 (chinanews.com.cn)
5. 观察者网 (guancha.cn)
6. 环球网 (huanqiu.com)
7. 新浪新闻 (news.sina.com.cn)
8. 搜狐新闻 (sohu.com)
9. 腾讯新闻 (news.qq.com)
10. 澎湃新闻 (thepaper.cn)
11. 维基百科 (en.wikipedia.org)

---

## 常见问题

**Q: 启动报错 "Connection refused"**
A: 检查 RabbitMQ 和 MySQL 是否已启动。

**Q: Playwright 报错 "Browser not found"**
A: 首次使用需安装 Chromium：
```bash
npx playwright install chromium
# 或
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

**Q: 前端页面空白/接口 401**
A: 检查是否已登录，Token 是否过期（1 天有效期）。

**Q: 任务一直处于 QUEUED/PENDING 状态**
A: 检查 CrawlerNode 是否已启动，Server 的 `CrawlerStatusListener` 是否收到节点心跳（每 5 秒一次）。

**Q: MHTML 文件保存在哪里？**
A: 保存在 `shared/crawl-files/{taskId}/01.mhtml`，`02.mhtml` 等，每个页面一个文件。
