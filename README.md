# 星辰商城 · 电商管理系统

一个前后端分离的 **B2C 电商管理系统**：买家可以在商城前台浏览商品、加购下单、模拟支付、查看物流、申请售后、发表评价；管理员在后台管理商品、分类、多规格 SKU、订单发货、售后审核、营销内容、会员与系统权限。

> 技术栈：**Spring Boot 3.3 + MyBatis-Plus + Spring Security/JWT + MySQL 8 + Redis** ｜ **Vue 3 + Vite + Element Plus + Pinia + ECharts**

### 交付验证状态（本机实测）

| 项目 | 结果 |
| --- | --- |
| 数据库 | `mall_db` 24 张表建成，演示数据导入成功（10 分类 / 24 商品 / 129 SKU / 6 买家 / 40 订单 / 104 条物流轨迹…） |
| 后端编译 | `mvn compile` **BUILD SUCCESS**（230 个 Java 源文件） |
| 后端启动 | `mvn spring-boot:run` 启动成功，3.6s 内就绪，Tomcat 8080 |
| 接口联调 | `tools/verify_api.ps1` 端到端脚本 **108 项检查全部通过、0 失败**（覆盖鉴权、RBAC、商品多规格、下单/支付/取消与库存回滚、发货与物流、售后申请与审核、Excel 导出、看板、系统管理、会员统计、Token 失效） |
| 接口文档 | Knife4j `/doc.html` 可访问，OpenAPI 解析出 **121 个接口路径** |
| 前端构建 | `npm run build` **成功**（Vite 5，2323 个模块，39 个页面 chunk 全部产出） |
| 前端运行 | `npm run dev` 启动成功，**39/39 个页面组件 + 6 个布局/公共组件全部通过 Vite 编译**，`/api` 代理连通后端，图片静态资源与完整下单链路经代理验证可用 |
| Docker | `docker-compose.yml` 已提供，但**本机未安装 Docker，未实测** |

> 复现验证：先启动后端，再执行
> `pwsh -File tools/verify_api.ps1`（若执行策略受限，可用
> `powershell -ExecutionPolicy Bypass -File tools/verify_api.ps1`）。

---

## 一、功能清单

### 1. 买家商城前台（`/`）

| 模块 | 功能 |
| --- | --- |
| 首页 | 轮播图、平台公告、分类快捷入口、领券专区、推荐/热销/新品楼层 |
| 商品 | 商品列表（分类筛选、关键词搜索、价格区间、销量/价格/新品排序、分页）、商品详情（多图轮播、多规格联动选择、库存与销量、图文详情） |
| 购物车 | 加购、单选/全选、数量增减（校验库存）、批量删除、失效商品分组、实时计价、去结算 |
| 下单 | 结算页预览（收货地址选择、优惠券抵扣、运费与包邮规则、备注）、提交订单、**模拟支付**、取消订单（回滚库存与优惠券） |
| 订单 | 我的订单（状态分页）、订单详情、确认收货、删除订单、物流轨迹时间线 |
| 售后 | 申请售后（仅退款/退货退款/换货）、售后列表与详情、撤销申请 |
| 评价 | 待评价订单项、发表评价（星级/内容/匿名）、我的评价（含商家回复） |
| 会员中心 | 个人资料与改密、收货地址管理、商品收藏、浏览记录、我的优惠券（领券/使用状态） |
| 账户 | 注册、登录（JWT）、退出登录 |

### 2. 管理后台（`/admin`）

| 模块 | 功能 |
| --- | --- |
| 数据看板 | 今日/累计销售额与订单、新增用户、待发货、待处理售后、库存预警；销售趋势折线图、分类销售占比饼图、热销商品 TOP10、订单状态分布 |
| 商品管理 | 商品列表（搜索/筛选/上下架/推荐）、新增与编辑（多规格 SKU 笛卡尔积生成、批量改价改库存、多图上传）、分类树管理、库存预警（阈值可配）、商品评价（回复/隐藏/删除） |
| 订单管理 | 订单列表（多条件查询）、订单详情、发货（自动生成物流轨迹）、追加物流轨迹、商家备注、订单改价、关闭订单、导出 Excel |
| 售后管理 | 售后工单列表与详情、审核（同意/拒绝/退款完成，自动回补库存）、状态流转 |
| 营销管理 | 优惠券（满减/折扣、适用范围、发行量、领取记录）、轮播图、公告（草稿/发布） |
| 会员管理 | 会员列表（下单数、消费总额）、会员详情（地址数/收藏数/评价数/最近订单）、启用禁用（禁用即强制下线） |
| 系统管理 | 管理员管理、角色权限（RBAC 权限树分配）、系统配置（包邮门槛、运费、库存预警阈值等）、操作日志（AOP 自动记录、清理） |
| 权限体系 | 超级管理员 / 运营客服 / 仓储发货三种内置角色，前端动态菜单 + 按钮级权限（`v-perm`），后端 `@PreAuthorize` 二次校验 |

---

## 二、目录结构

```
project2-ds/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/mall/
│   │   ├── MallApplication.java
│   │   ├── common/                   # 统一响应、异常、枚举、操作日志切面、Excel 工具
│   │   ├── config/                   # Security / Redis / MyBatis-Plus / 静态资源 / Knife4j / Jackson
│   │   ├── security/                 # JWT、Token 服务、认证过滤器、安全上下文工具
│   │   ├── entity/                   # 24 个实体（与数据库表一一对应）
│   │   ├── mapper/                   # 24 个 Mapper + 统计 Mapper
│   │   ├── dto/ vo/                  # 请求参数与响应对象
│   │   ├── service/ service/impl/    # 业务层
│   │   └── controller/               # admin（管理端）/ shop（买家端）控制器
│   ├── src/main/resources/application.yml
│   ├── uploads/                      # 图片本地磁盘存储（商品图/轮播图/头像）
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                         # Vue3 + Vite 前端（单工程，路由区分前后台）
│   ├── src/
│   │   ├── api/                      # axios 封装 + 全部接口方法
│   │   ├── store/                    # Pinia：买家、管理员、购物车
│   │   ├── router/                   # 路由表 + 登录/权限守卫
│   │   ├── layouts/                  # ShopLayout / AdminLayout
│   │   ├── components/               # 分页、图片上传、商品卡片、递归菜单
│   │   ├── views/shop/               # 买家端页面
│   │   └── views/admin/              # 管理端页面
│   ├── vite.config.js  nginx.conf  Dockerfile  package.json
├── sql/
│   ├── 01-schema.sql                 # 建库建表（24 张表）
│   └── 02-data.sql                   # 演示数据（可重复导入）
├── docs/                             # 需求说明 / 数据库设计 / 系统架构 / 接口文档
├── tools/gen_seed.py                 # 演示数据生成器（可重新生成 02-data.sql 与占位图）
├── docker-compose.yml                # MySQL + Redis + 后端 + 前端 一键部署
└── README.md
```

---

## 三、快速开始（本地开发）

### 1. 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 后端编译运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0 | 数据库 `mall_db` |
| Redis | 5+ | 登录态与缓存（本地需启动） |

### 2. 初始化数据库

```bash
# 会创建 mall_db、24 张表并写入演示数据（脚本可重复执行）
mysql -h127.0.0.1 -P3306 -uroot -p123456 --default-character-set=utf8mb4 < sql/01-schema.sql
mysql -h127.0.0.1 -P3306 -uroot -p123456 --default-character-set=utf8mb4 < sql/02-data.sql
```

> 如果你的 MySQL 账号密码不同，请同步修改 `backend/src/main/resources/application.yml`
> （或用环境变量 `MYSQL_HOST/MYSQL_PORT/MYSQL_DATABASE/MYSQL_USERNAME/MYSQL_PASSWORD` 覆盖）。

### 3. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/mall-backend.jar
# 或开发模式：mvn spring-boot:run
```

启动成功后：

- 后端接口：<http://localhost:8080/api>
- 在线接口文档（Knife4j）：<http://localhost:8080/doc.html>

> ⚠️ 图片存储目录 `./uploads` 是相对**启动目录**的，请在 `backend/` 目录下启动；
> 若在其他目录启动，请加参数 `--mall.upload.path=D:/path/to/backend/uploads`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 商城前台：<http://localhost:5173>
- 管理后台：<http://localhost:5173/admin/login>

前端 dev server 已配置代理：`/api` → `http://127.0.0.1:8080`。

### 5. 生产构建

```bash
cd frontend
npm run build          # 产物在 frontend/dist，用 Nginx 托管（参考 frontend/nginx.conf）
```

---

## 四、演示账号

| 端 | 账号 | 密码 | 角色 / 说明 |
| --- | --- | --- | --- |
| 管理后台 | `admin` | `123456` | 超级管理员（全部权限） |
| 管理后台 | `operator` | `123456` | 运营/客服（商品、订单、营销、会员，无系统管理） |
| 管理后台 | `warehouse` | `123456` | 仓储/发货（订单发货、物流、库存预警） |
| 管理后台 | `kefu` | `123456` | 客服（同运营角色） |
| 商城前台 | `zhangsan` | `123456` | 买家（另有 `lisi`、`wangwu`、`zhaoliu`、`sunqi`、`zhouba`） |

演示数据规模：**10 个分类 / 24 个商品 / 129 个 SKU / 6 个买家 / 40 笔订单（覆盖 6 种状态）/ 77 条订单明细 / 104 条物流轨迹 / 3 个售后单 / 19 条商品评价 / 5 张优惠券 / 5 张轮播图 / 5 条公告 / 65 条权限 / 9 项系统配置**。

---

## 五、Docker 一键部署

```bash
docker compose up -d --build
```

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| 前台商城 + 后台 | <http://localhost:8081> | Nginx 托管前端并反代 `/api` |
| 后端接口文档 | <http://localhost:8080/doc.html> | |
| MySQL | `localhost:3307` | root / 123456，数据卷 `mall-mysql-data` |
| Redis | `localhost:6380` | |

容器首次启动会自动执行 `sql/` 下的建表与演示数据脚本。
> 说明：交付环境未安装 Docker，`docker-compose.yml` 未在本机实测，首次启动请留意 `docker compose logs -f backend`。

---

## 六、接口文档

- **在线文档（推荐）**：启动后端后访问 <http://localhost:8080/doc.html>，分「买家商城端」「管理后台」两组。
- 鉴权方式：先调用登录接口拿到 `token`，点击页面右上角 **Authorize** 填入 token（无需 `Bearer ` 前缀）。
- 静态说明：`docs/03-接口文档.md`（含全部接口清单与示例）。
- 统一响应结构：

```json
{ "code": 200, "message": "操作成功", "data": { }, "timestamp": 1760000000000 }
```

分页响应 `data` 结构：`{ "records": [], "total": 40, "page": 1, "size": 10, "pages": 4 }`

**认证约定**

| 端 | 登录接口 | 请求头 |
| --- | --- | --- |
| 买家端 | `POST /api/auth/login` | `Authorization: Bearer <token>` |
| 管理端 | `POST /api/admin/auth/login` | `Authorization: Bearer <token>` |

Token 为 JWT，同时在 Redis 中保存登录态，因此**退出登录、禁用账号、变更角色权限都会立即使旧 Token 失效**。

---

## 七、关键设计说明

1. **鉴权与权限**：JWT 无状态签名 + Redis 登录态（`mall:login:{type}:{id}`）。过滤器解析 Token 后写入 Spring Security 上下文；`/api/admin/**` 需要 `TYPE_ADMIN`，买家业务接口需要 `TYPE_BUYER`，管理端方法上再用 `@PreAuthorize("hasAuthority('product:add')")` 做按钮级校验。
2. **多规格 SKU**：SPU（`product`）+ 规格定义（`product_spec`）+ SKU（`product_sku`），SKU 用 `spec_data` 存 JSON、`spec_info` 存展示文本；商品价格与库存由 SKU 汇总。
3. **库存一致性**：下单即扣减库存，使用 `UPDATE product_sku SET stock = stock - ? WHERE id = ? AND stock >= ?` 的原子更新，影响行数为 0 即抛业务异常并回滚；取消订单/关闭订单/售后退款完成会回补库存与销量。
4. **金额计算**：前端传入的金额一律不信任，服务端根据 SKU 现价重算商品总额、运费（满 99 包邮，否则 10 元）、优惠券抵扣与实付金额。
5. **优惠券**：`coupon` 定义 + `user_coupon` 领取记录，下单时核销（状态改为已使用并记录订单号），取消订单自动回滚。
6. **订单状态机**：`0 待付款 → 1 待发货 → 2 待收货 → 3 已完成`，另有 `4 已取消`、`5 售后中`；每次流转都会写入对应时间戳（支付/发货/收货/完成/关闭）。
7. **操作日志**：`@OperLog(module=..., value=...)` 注解 + AOP 切面自动记录管理员写操作（含 IP、耗时、异常信息），后台可查询与清理。
8. **图片存储**：本地磁盘 `backend/uploads/{type}/yyyyMM/`，通过 `/api/files/**` 静态资源映射访问；演示数据使用内置 SVG 占位图（无需外网）。
9. **接口文档**：Knife4j（OpenAPI 3），`/doc.html`。
10. **Excel 导入导出**：Apache POI 实现商品导出/导入（含模板下载）与订单导出。

---

## 八、常见问题

| 问题 | 处理 |
| --- | --- |
| 启动报 `Unknown database 'mall_db'` | 先执行 `sql/01-schema.sql` 与 `sql/02-data.sql` |
| 启动报 Redis 连接失败 | 启动本机 Redis，或修改 `application.yml` 中 `spring.data.redis.*` |
| 商品图片 404 | 确认后端启动目录为 `backend/`（`uploads` 相对路径），或显式指定 `--mall.upload.path` |
| 端口冲突 | 后端改 `server.port`，前端改 `vite.config.js` 的 `server.port` 与代理 target |
| 登录后仍提示未登录 | 检查浏览器是否禁用 localStorage，或 Token 是否已过期（默认 7 天） |
| 前端请求 404 | 确认后端已启动且 dev server 代理配置指向 8080 |

---

## 九、数据库表清单（24 张）

| 分类 | 表 |
| --- | --- |
| 账户 | `user`、`user_address`、`favorite`、`browse_history` |
| 商品 | `category`、`product`、`product_spec`、`product_sku` |
| 交易 | `cart`、`orders`、`order_item`、`express_track`、`refund`、`coupon`、`user_coupon` |
| 内容 | `product_comment`、`banner`、`notice` |
| 系统 | `sys_user`、`sys_role`、`sys_permission`、`sys_role_permission`、`sys_config`、`sys_oper_log` |

详细字段与 E-R 说明见 `docs/02-数据库设计.md`。

---

## 十、重新生成演示数据（可选）

```bash
python tools/gen_seed.py          # 重新生成 sql/02-data.sql 与 backend/uploads/**/*.svg
```

> 脚本会读取 `tools/pwd_hash.txt` 中的 BCrypt 密码哈希（对应明文 `123456`）；
> 如该文件不存在，请先用 `tools/Hash.java` 生成，或直接将演示账号密码改为其他值。

---

## 十一、已知限制与后续改进建议

本项目为教学/演示级别的完整电商系统，以下为代码审计后确认、**尚未处理**的已知限制（不影响主流程可用性）：

| 类别 | 说明 | 建议 |
| --- | --- | --- |
| 定时任务 | 超时未支付订单的自动取消是**惰性触发**（买家访问订单列表时清理自己的订单），无 `@Scheduled` | 增加定时任务扫描全量超时订单 |
| 系统配置 | `shop_name`、`shop_notice`、`hot_search_keywords`、`order_auto_finish_days`、`after_sale_days` 已入库但代码暂未读取（运费、包邮门槛、库存预警阈值、自动取消时长**已生效**） | 需要时在对应业务中读取 |
| 订单可见性 | 买家删除订单后，管理端订单列表也查不到（MyBatis-Plus 逻辑删除全局生效） | 后台查询改为绕过逻辑删除或引入归档 |
| 售后状态机 | `refund.status = 1（已同意）` 在当前流转路径下不可达（仅退款直接 1→4，退货退款 1→3） | 统一状态机定义 |
| 物流节点 | 发货时自动生成的"运输中"轨迹时间为 `now + 2 小时`（未来时间） | 改为顺延或仅生成揽收节点 |
| 统计口径 | 看板"今日订单数"按下单时间、"今日销售额"按支付时间且仅计已支付，两者不可相除算客单价 | 文档已注明，按需统一 |
| 在线人数 | 使用 Redis `KEYS` 扫描（O(N)），且统计的是"账号数"而非并发连接数 | 改用 `SCAN` 或维护计数 |
| 图片存储 | 本地磁盘存储，多实例部署时各实例不共享，且无引用计数 | 生产环境建议接入 OSS/MinIO |
| 并发库存 | 库存扣减为单条原子 SQL（`stock = stock - ? WHERE stock >= ?`），高并发下未加分布式锁 | 秒杀场景可引入 Redis 预扣 + 消息队列 |
| 命名规范 | `orders` 表名为复数（`order` 为 MySQL 保留字），其余 23 张表为单数 | 如需统一可改为 `mall_order` |
| 日志清理 | `sys_oper_log` 无自动清理，需人工在后台清理 | 增加定时清理策略 |
| 上传接口 | 上传/删除文件接口未加 `@PreAuthorize`，仅依赖 `/api/admin/**` 的角色校验 | 按需补细粒度权限 |

> 本次交付中**已修复**的问题：后台「系统配置」的运费与包邮门槛未生效（现已在计算运费时优先读取 `sys_config`）、库存预警未按商品自身 `warn_stock` 判定、订单改价导致金额等式不成立、停用角色未踢出在线管理员、前后端权限判定规则不一致。

---

## 十二、文档索引

| 文档 | 内容 |
| --- | --- |
| `docs/01-需求说明书.md` | 项目背景、角色权限矩阵、69 个用例（FR）、100 条业务规则（BR）、非功能需求、需求追溯矩阵 |
| `docs/02-数据库设计.md` | 设计规范、5 张 E-R 图、24 张表逐表字段说明、9 个统计口径（含真实 SQL） |
| `docs/03-系统架构设计.md` | 总体架构、技术选型理由、分层设计、14 张 mermaid（时序/状态机/部署）、15 条架构决策（ADR） |
| `docs/04-接口文档.md` | 143 个接口逐条说明（方法/路径/权限/参数/响应示例）、状态码字典、65 条权限清单 |
| 在线接口文档 | 启动后端后访问 <http://localhost:8080/doc.html>（Knife4j） |
