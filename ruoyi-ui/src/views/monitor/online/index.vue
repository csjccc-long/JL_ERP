<template>
  <div class="app-container online-page">
    <el-row :gutter="12" class="mb8">
      <el-col :xs="24" :lg="16">
        <el-card class="globe-card" shadow="never">
          <template #header>
            <div class="card-head">
              <span class="card-title">在线用户分布</span>
              <span class="card-extra">
                {{ onlineCount }} 个会话 · {{ regionStats.length }} 个地区
              </span>
            </div>
          </template>

          <div class="globe-body">
            <div class="globe-canvas">
              <cobe-globe
                :key="globeKey"
                :markers="globeMarkers"
                :arcs="globeArcs"
                :marker-color="[0, 0.745, 0.706]"
                :base-color="[1, 1, 1]"
                :arc-color="[0, 0.745, 0.706]"
                :glow-color="[0.8, 0.9, 0.9]"
                :dark="0"
                :phi="initialPhi"
                :map-brightness="14"
                :marker-elevation="0.02"
                :arc-width="0.4"
                :arc-height="0.2"
                :speed="0.0028"
                :theta="0.22"
                :diffuse="1.2"
                :map-samples="16000"
              />
              <p v-if="!onlineCount" class="globe-empty">当前没有在线会话</p>
            </div>

            <div class="globe-side">
              <div class="globe-stats">
                <div class="globe-stat">
                  <span class="globe-stat__label">在线会话</span>
                  <span class="globe-stat__value">{{ onlineCount }}</span>
                </div>
                <div class="globe-stat">
                  <span class="globe-stat__label">独立用户</span>
                  <span class="globe-stat__value">{{ userCount }}</span>
                </div>
                <div class="globe-stat">
                  <span class="globe-stat__label">覆盖地区</span>
                  <span class="globe-stat__value">{{ regionStats.length }}</span>
                </div>
                <div class="globe-stat">
                  <span class="globe-stat__label">内网会话</span>
                  <span class="globe-stat__value">{{ intranetCount }}</span>
                </div>
              </div>
              <p class="globe-hint">拖动地球可旋转查看；数据来自 Redis 在线会话</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="region-card" shadow="never">
          <template #header>
            <div class="card-head">
              <span class="card-title">地区分布</span>
              <span class="card-extra">按登录地点聚合</span>
            </div>
          </template>

          <ul v-if="regionStats.length" class="region-list">
            <li
              v-for="region in regionStats"
              :key="region.key"
              class="region-item"
              :class="{ 'is-active': hoverKey === region.key }"
              @mouseenter="hoverKey = region.key"
              @mouseleave="hoverKey = ''"
            >
              <div class="region-item__head">
                <span class="region-item__dot" :style="{ background: region.color }" />
                <span class="region-item__name">{{ region.name }}</span>
                <span class="region-item__count">{{ region.count }}</span>
                <span class="region-item__share">{{ region.share }}%</span>
              </div>
              <div class="region-item__bar">
                <i :style="{ width: region.percent + '%', background: region.color }" />
              </div>
            </li>
          </ul>
          <el-empty v-else description="暂无在线会话" :image-size="72" />

          <p class="region-foot">
            内网 {{ intranetCount }} 个会话 · 外部 {{ externalCount }} 个会话
          </p>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-head">
          <span class="card-title">全部在线会话</span>
          <el-button link type="primary" icon="Refresh" @click="getList">刷新</el-button>
        </div>
      </template>

      <el-form :model="queryParams" ref="queryRef" :inline="true" class="table-query">
        <el-form-item label="登录地址" prop="ipaddr">
          <el-input
            v-model="queryParams.ipaddr"
            placeholder="请输入登录地址"
            clearable
            style="width: 180px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="用户名称" prop="userName">
          <el-input
            v-model="queryParams.userName"
            placeholder="请输入用户名称"
            clearable
            style="width: 180px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="loading"
        :data="onlineList.slice((pageNum - 1) * pageSize, pageNum * pageSize)"
        style="width: 100%"
      >
        <el-table-column label="序号" width="56" type="index" align="center">
          <template #default="scope">
            <span>{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="会话编号" align="center" prop="tokenId" :show-overflow-tooltip="true" />
        <el-table-column label="登录名称" align="center" prop="userName" :show-overflow-tooltip="true" />
        <el-table-column label="所属部门" align="center" prop="deptName" :show-overflow-tooltip="true" />
        <el-table-column label="主机" align="center" prop="ipaddr" :show-overflow-tooltip="true" />
        <el-table-column label="登录地点" align="center" prop="loginLocation" :show-overflow-tooltip="true" />
        <el-table-column label="操作系统" align="center" prop="os" :show-overflow-tooltip="true" />
        <el-table-column label="浏览器" align="center" prop="browser" :show-overflow-tooltip="true" />
        <el-table-column label="登录时间" align="center" prop="loginTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.loginTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button
              link
              type="primary"
              icon="Delete"
              @click="handleForceLogout(scope.row)"
              v-hasPermi="['monitor:online:forceLogout']"
            >
              强退
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="pageNum" v-model:limit="pageSize" />
    </el-card>
  </div>
</template>

<script setup name="Online">
import { forceLogout, list as initData } from "@/api/monitor/online";
import CobeGlobe from "@/components/CobeGlobe/index.vue";
import { resolveLoginLocation } from "@/utils/geo-city";

const { proxy } = getCurrentInstance();

const onlineList = ref([]);
const loading = ref(true);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const hoverKey = ref("");
const globeKey = ref(0);

const queryParams = ref({
  ipaddr: undefined,
  userName: undefined
});

const REGION_COLORS = ["#00beb4", "#5b8ff9", "#f7863b", "#9270ca", "#3ba05c", "#e8b30b"];

/** 在线会话总数（RuoYi 在线列表不分页，前端切片） */
const onlineCount = computed(() => onlineList.value.length);
const userCount = computed(() => new Set(onlineList.value.map((row) => row.userName)).size);

/** 按登录地点聚合：内网合并成一个点位，其余按城市 */
const regionStats = computed(() => {
  const grouped = new Map();
  for (const row of onlineList.value) {
    const location = resolveLoginLocation(row.loginLocation);
    const item = grouped.get(location.key) || {
      key: location.key,
      name: location.name,
      city: location.city,
      lat: location.lat,
      lng: location.lng,
      kind: location.kind,
      count: 0
    };
    item.count += 1;
    grouped.set(location.key, item);
  }

  const list = [...grouped.values()].sort((a, b) => b.count - a.count);
  const max = list[0]?.count || 1;
  const sum = onlineCount.value || 1;
  return list.map((item, index) => ({
    ...item,
    percent: Math.max(6, Math.round((item.count / max) * 100)),
    share: Math.round((item.count / sum) * 100),
    color: REGION_COLORS[index % REGION_COLORS.length]
  }));
});

const intranetCount = computed(
  () => regionStats.value.find((region) => region.kind === "intranet")?.count || 0
);
const externalCount = computed(() => onlineCount.value - intranetCount.value);

/** 地球上的点位：每个地区一个，数量越多点越大，hover 地区时高亮 */
const globeMarkers = computed(() =>
  regionStats.value.map((region) => {
    const active = hoverKey.value === region.key;
    return {
      id: "loc-" + region.key,
      location: [region.lat, region.lng],
      label: region.name + " · " + region.count,
      size: (0.018 + Math.min(0.026, Math.sqrt(region.count) * 0.008)) * (active ? 1.45 : 1),
      color: active ? [0.05, 0.85, 0.8] : undefined
    };
  })
);

/** 弧线：从内网（总部）连到各个外部城市 */
const globeArcs = computed(() => {
  const hub = regionStats.value.find((region) => region.kind === "intranet") || regionStats.value[0];
  if (!hub) return [];
  return regionStats.value
    .filter((region) => region.key !== hub.key)
    .slice(0, 8)
    .map((region) => ({
      id: "arc-" + hub.key + "-" + region.key,
      from: [hub.lat, hub.lng],
      to: [region.lat, region.lng]
    }));
});

/** 初始旋转角：让总部（或会话最多的地区）正对镜头，用户一进页面就能看到自己的位置 */
const initialPhi = computed(() => {
  const hub = regionStats.value.find((region) => region.kind === "intranet") || regionStats.value[0];
  const lng = hub ? hub.lng : 114.0579;
  const phi = (lng * Math.PI) / 180 + 0.9;
  return ((phi % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
});

/** 地球按窗口宽度分档重建，避免 canvas 缩放糊掉 */
function syncGlobeKey() {
  const bucket = Math.round(window.innerWidth / 140);
  if (bucket !== globeKey.value) globeKey.value = bucket;
}

/** 查询登录日志列表 */
function getList() {
  loading.value = true;
  initData(queryParams.value)
    .then((response) => {
      onlineList.value = response.rows;
      total.value = response.total;
      loading.value = false;
    })
    .catch(() => {
      loading.value = false;
    });
}

/** 搜索按钮操作 */
function handleQuery() {
  pageNum.value = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 强退按钮操作 */
function handleForceLogout(row) {
  proxy
    .$modal.confirm('是否确认强退名称为"' + row.userName + '"的用户?')
    .then(function () {
      return forceLogout(row.tokenId);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    })
    .catch(() => {});
}

getList();
syncGlobeKey();
window.addEventListener("resize", syncGlobeKey);
onBeforeUnmount(() => window.removeEventListener("resize", syncGlobeKey));
</script>

<style scoped lang="scss">
.online-page {
  :deep(.el-card__header) {
    padding: 14px 18px;
  }

  :deep(.el-card__body) {
    padding: 18px;
  }
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  .card-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--app-text, #161618);
  }

  .card-extra {
    font-size: 12px;
    color: var(--app-text-secondary, #75787a);
  }
}

.globe-card,
.region-card {
  height: 100%;
  border-radius: 12px;
}

.globe-card :deep(.el-card__body) {
  display: flex;
  align-items: stretch;
}

.globe-body {
  display: flex;
  align-items: center;
  gap: 28px;
  width: 100%;
  flex-wrap: wrap;
}

.globe-canvas {
  position: relative;
  flex: 0 1 340px;
  min-width: 220px;
  margin: 0 auto;
}

.globe-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  font-size: 13px;
  color: var(--app-text-secondary, #75787a);
  pointer-events: none;
}

.globe-side {
  flex: 1 1 190px;
  min-width: 180px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
}

.globe-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.globe-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--el-fill-color-light, #f4f5f6);

  &__label {
    font-size: 12px;
    color: var(--app-text-secondary, #75787a);
  }

  &__value {
    font-size: 22px;
    font-weight: 600;
    line-height: 1.1;
    color: var(--app-text, #161618);
    font-variant-numeric: tabular-nums;
  }
}

.globe-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-secondary, #75787a);
}

.region-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.region-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.region-item {
  padding: 8px 10px;
  border-radius: 10px;
  transition: background-color 0.2s ease;
  cursor: default;

  &.is-active {
    background: var(--el-fill-color-light, #f4f5f6);
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: var(--app-text, #161618);
  }

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex: none;
  }

  &__name {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__count {
    font-weight: 600;
    font-variant-numeric: tabular-nums;
  }

  &__share {
    width: 38px;
    text-align: right;
    font-size: 12px;
    color: var(--app-text-secondary, #75787a);
    font-variant-numeric: tabular-nums;
  }

  &__bar {
    margin-top: 8px;
    height: 6px;
    border-radius: 3px;
    background: var(--el-fill-color-light, #f4f5f6);
    overflow: hidden;

    i {
      display: block;
      height: 100%;
      border-radius: 3px;
      transition:
        width 0.5s cubic-bezier(0.22, 0.61, 0.36, 1),
        background-color 0.2s ease;
    }
  }
}

.region-foot {
  margin: auto 0 0;
  padding-top: 12px;
  border-top: 1px dashed var(--el-border-color-lighter, #ebedf0);
  font-size: 12px;
  color: var(--app-text-secondary, #75787a);
}

.table-card {
  border-radius: 12px;

  .table-query {
    margin-bottom: 4px;
  }
}

@media (max-width: 1199px) {
  .globe-canvas {
    flex-basis: 280px;
  }

  .globe-card,
  .region-card {
    height: auto;
  }

  .region-card :deep(.el-card__body) {
    height: auto;
    min-height: 150px;
  }
}
</style>
