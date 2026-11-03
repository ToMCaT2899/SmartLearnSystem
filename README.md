# 智慧学工系统 (SmartLearn System)

> 智慧学工系统是一个面向高校的综合性学生事务管理平台，集成了学生信息管理、奖助学金、勤工助学、课外活动、课程成绩、纪律管理、家校沟通等功能模块。

## 功能模块

### 核心功能
- **用户认证与权限管理** — 支持学生、教师（辅导员/班主任）、管理员三种角色，JWT + Spring Security 安全认证
- **学生档案管理** — 学生基本信息、家庭成员、紧急联系人、证明材料上传（阿里云 OSS）
- **奖助学金管理** — 奖学金/助学金发布、学生在线申请、多级审核流程
- **勤工助学** — 岗位发布、学生应聘、录用管理
- **课外活动 / 第二课堂** — 活动发布、在线报名、成绩记录
- **课程与成绩管理** — 课程信息、成绩录入与查询
- **纪律管理** — 违纪记录、晚归记录
- **家校沟通** — 私信消息、班级通知、家长反馈
- **安全教育** — 安全报告提交与管理
- **数据仪表盘** — ECharts 可视化图表展示学生数据概览

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端框架** | Spring Boot 3.2.x, Java 17 |
| **ORM** | MyBatis-Plus 3.5.x |
| **数据库** | MySQL 8.0 |
| **安全认证** | Spring Security + JJWT (JSON Web Token) |
| **API 文档** | SpringDoc OpenAPI (Swagger UI) |
| **文件存储** | Alibaba Cloud OSS |
| **工具库** | Hutool 5.8.x, Apache POI, Lombok |
| **前端框架** | Vue 3 + TypeScript |
| **构建工具** | Vite 5.x |
| **UI 组件库** | Element Plus |
| **状态管理** | Pinia |
| **数据可视化** | ECharts + Vue-ECharts |
| **HTTP 客户端** | Axios |

## 项目结构

```
SmartLearnSystem/
├── backend/                          # 后端 Spring Boot 项目
│   └── src/main/java/com/smartlearn/
│       ├── config/                   # 配置类（OSS, Swagger, WebMvc）
│       ├── controller/               # REST 控制器
│       ├── dto/                      # 数据传输对象
│       ├── entity/                   # 数据库实体类
│       ├── mapper/                   # MyBatis-Plus Mapper 接口
│       ├── security/                 # Spring Security 配置
│       ├── service/                  # 业务服务层
│       └── vo/                       # 视图对象
├── frontend/                         # 前端 Vue 3 项目
│   └── src/
│       ├── api/                      # API 请求封装
│       ├── views/                    # 页面组件
│       │   ├── Dashboard/            # 数据仪表盘
│       │   └── Login/               # 登录/注册
│       └── utils/                    # 工具函数
├── 数据库设计文档.md                  # 数据库表结构文档
└── README.md
```

## 快速开始

### 环境要求

- **JDK** 17+
- **Maven** 3.6+
- **MySQL** 8.0+
- **Node.js** 18+
- **pnpm/npm** 最新版（推荐 pnpm）

### 后端启动

1. 创建数据库：
```sql
CREATE DATABASE smart_learn CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置数据库连接，复制配置模板并填入实际值：
```bash
cp backend/src/main/resources/application-dev.yml.example \
   backend/src/main/resources/application-dev.yml
```

编辑 `application-dev.yml` 填入数据库密码、JWT 密钥、OSS 凭证等信息。

> ⚠️ **注意**：`application-dev.yml` 包含敏感信息，已加入 `.gitignore`，不会被提交到仓库。

3. 启动后端：
```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8081`，Swagger 文档地址：`http://localhost:8081/swagger-ui.html`

### 前端启动

```bash
cd frontend
npm install   # 或 pnpm install
npm run dev   # 或 pnpm dev
```

前端开发服务器默认运行在 `http://localhost:5173`

## 数据库设计

项目包含完整的数据库设计文档，涵盖以下模块：

- 用户认证模块（`sys_user`）
- 学生信息模块（档案、家庭成员、紧急联系人）
- 奖助学金模块（奖学金/助学金申请与审核）
- 勤工助学模块（岗位发布与应聘）
- 课外活动模块（活动报名与第二课堂成绩）
- 课程成绩模块
- 纪律管理模块
- 消息通知模块
- 家校沟通模块

详见 [数据库设计文档.md](./数据库设计文档.md)

## 配置说明

| 配置文件 | 说明 |
|---------|------|
| `application.yml` | 主配置，指定端口、数据源、MyBatis-Plus 等公共配置 |
| `application-dev.yml.example` | 开发环境敏感配置模板，需复制为 `application-dev.yml` 并填入真实值 |
| `application-dev.yml` | 实际开发环境配置（**已忽略，不提交**） |

关键配置项（在 `application-dev.yml` 中）：
- `spring.datasource.username` / `password` — 数据库凭证
- `jwt.secret` — JWT 签名密钥（至少 32 字符）
- `aliyun.oss.access-key-id` / `access-key-secret` — 阿里云 OSS 凭证（如需文件上传功能）


## License

本项目仅用于学习和展示目的。
