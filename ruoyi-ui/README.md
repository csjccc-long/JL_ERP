# 金流智能 ERP · 前端（ruoyi-ui）

本目录是金流智能 ERP 的前端工程，基于 Vue 3 + Vite + Element Plus，配套后端见仓库根目录（Spring Boot 4 / Java 17）。项目整体说明请看 [../README.md](../README.md)。

## 技术栈

| 能力 | 选型 |
| --- | --- |
| 框架 | Vue 3.5（`<script setup>` + Composition API） |
| 构建 | Vite 6 |
| UI | Element Plus 2.13 |
| 状态 / 路由 | Pinia 3 / Vue Router 4 |
| 图表 | ECharts 5.6 + 自绘 Canvas / WebGL 组件 |
| 其它 | axios、Quill 2、xlsx、docx-preview、cobe |

## 常用命令

```bash
npm install
npm run dev         # 开发服务器，默认 80 端口，可用 VITE_PORT=8081 覆盖
npm run build:prod  # 生产构建，产物在 dist/
npm run build:stage # 预发布构建
```

开发环境通过 `.env.development` 的 `VITE_APP_BASE_API=/dev-api` 走 Vite 代理（`vite.config.js` 中 `VITE_PROXY_TARGET`，默认指向 `http://localhost:8080`）。

## 目录

```text
src
├─ api         接口封装（erp/**、knowledge/**、assistant/**、system/**）
├─ assets      图标、logo、主题样式（styles/rag-theme.scss、logo-loading.scss）
├─ components  通用与自绘组件（DonutChart、RidgeChart、TicketCard、CobeGlobe、UploadProgress …）
├─ layout      框架布局（侧边栏、顶栏、通知、标签页）
├─ router      路由与守卫
├─ store       Pinia 模块（用户、权限、设置、锁屏）
├─ utils       request / 字典 / 权限 / 上传进度队列 / 城市坐标
└─ views       业务页面（erp/**、knowledge/**、assistant/**、system/**）
```

## 约定

- `vite/plugins/auto-import.js` 用 `unplugin-auto-import` 自动注入 `vue` / `vue-router` / `pinia` / `useDict` / `selectDictLabel`，页面里无需再 import `ref`、`computed`、`onMounted`；**组件必须显式 import**（未启用 `unplugin-vue-components`）。
- 页面文件用 `<script setup name="Xxx">` 指定组件名，供 `keep-alive` 与标签页使用。
- 所有上传走统一进度：`utils/request.js` 拦截 `FormData` 请求并接管 `onUploadProgress`，由右下角 `UploadProgress` 浮层呈现；需要自行展示的（如知识文档弹窗）传 `uploadProgress: false` 关闭全局浮层。
- 主题色以 `assets/styles/rag-theme.scss` 中的 CSS 变量为准（`--el-color-primary: #00beb4` 等），自绘组件一律取变量或函数入参，不写死色值。

## 可视化组件一览

| 组件 | 说明 |
| --- | --- |
| `DonutChart` / `Legend` | SVG 环形占比图，逐段描边动画、悬停发光、圆心联动 |
| `RidgeChart` / `ChartModeSwitch` | 趋势数据的第二种表现：等距「山脉」视图，可与 ECharts 2D 折线切换 |
| `TicketCard` / `DitheringSurface` | 票券式订单卡 + WebGL2 抖动纹理，含撕开 / 盖章 / 时钟 / 像素填充四种状态动效 |
| `CobeGlobe` | 点阵地球（cobe），按登录地聚合标记与弧线 |
| `ParticlesBg` | 锁屏背景粒子（WebGL2 实例化） |
| `ImageGenLoader` / `UploadProgress` | 点阵波浪进度动画与全局上传浮层 |
| `JobStatus` / `BellToggle` / `FullscreenToggle` | 整行勾选、铃铛声波、全屏切换动效 |
