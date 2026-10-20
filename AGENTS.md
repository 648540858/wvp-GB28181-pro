# AGENTS.md

## 概览

- WVP-Pro：GB28181 + JT808/JT1078 视频平台。后端为 Spring Boot 3.4 / Java 21 单 Maven 模块；前端在 `web/`（Vue 2 + Element UI）。
- 入口：`src/main/java/com/genersoft/iot/vmp/VManageBootstrap.java`。
- 后端代码根目录：`src/main/java/com/genersoft/iot/vmp/`。

## 关键路径

- `gb28181/`：SIP 信令、设备/通道、点播、级联。核心类：`service/impl/PlayServiceImpl`、`InviteStreamServiceImpl`、`DialogTerminationService`；会话在 `session/SipInviteSessionManager`，信令在 `transmit/`。
- `jt1078/`：JT808/JT1078 协议与业务；`media/`：ZLMediaKit 接入；`streamProxy/`、`streamPush/`：拉流/推流代理。
- `web/`、`vmanager/`：REST 接口；`service/`：跨模块服务；`conf/`：配置、安全、调度（`DynamicTask`）；`storager/`：Redis 缓存。
- `dao/*Provider`：MyBatis 动态 SQL，必须用 `#{}`，禁止拼字符串。
- `数据库/<version>/`：按版本维护 SQL，无 Flyway/Liquibase。

## 架构与状态

- 运行态数据分三层：MySQL（配置/历史）、Redis（设备、InviteInfo、SIP 事务）、JVM 内存（等待回调、任务表）。修改点播/会话逻辑时三处都要考虑。
- `SipInviteSessionManager` 将 SIP 事务持久化到 Redis，WVP 重启后仍可能存在。
- 设备 BYE/事务清理优先复用 `DialogTerminationService`，不要新增直接 `cmder.streamByeCmd` 的旁路。
- `DynamicTask` 的 key 全局有效；完成/取消的 future 必须移除，否则无法重新注册。

## 构建与运行

- 使用 JDK 21；快速编译：`mvn -q -DskipTests compile`。
- 后端：`mvn spring-boot:run -Dspring-boot.run.profiles=dev`。`application.yml` 默认激活的 `274-dev` 被 gitignore，新 clone 需改用 `dev` 或本地私有 profile。
- 前端：`cd web && npm install`；`npm run dev`（9528，代理到 18080）；`npm run build:prod` 输出到 `src/main/resources/static`（gitignore）。`mvn package` 不会构建前端。
- 全栈：`cd docker && docker compose up -d`。

## 测试

- JUnit 5 + Mockito 单测，无需外部服务。
- `pom.xml` 硬编码 `<skipTests>true</skipTests>`，`-DskipTests=false` 无效；用 IDE 或临时改为 `false`，跑完恢复。
- 较新 JDK 下 Mockito 可能失败，务必使用 JDK 21。

## 编程规范

- 详细规范见 [`doc/编程规范.md`](doc/编程规范.md)；新代码遵守，存量代码逐步靠拢。
- Controller 保持薄，业务放 Service；普通返回由 `GlobalResponseAdvice` 包装为 `WVPResult`，业务错误抛 `ControllerException`。
- 重点约束：DAO 外部参数用 `#{}`、敏感信息不入日志/接口/配置、并发状态清理必须明确归属并使用条件删除、修 bug 补回归测试。

## 约定

- 注释、日志、提交信息以中文为主。
- 适时重构，避免小修小补导致代码臃肿；优先复用现有服务和抽象。
- 不要提交 `target/`、`logs/`、`data/`、`web/node_modules/`、`src/main/resources/static/` 及私有 `application-*.yml`。
- 审核 PR 先用简要文字说明：建议合并 / 可合并但需处理 / 建议发回作者修改。
- 注重隐私和安全，避免在日志、提交信息、配置文件以及接口中泄露敏感信息。
