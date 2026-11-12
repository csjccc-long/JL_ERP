<template>
  <div class="app-container online-page">
    <el-row :gutter="12" class="mb8">
      <el-col :xs="24" :lg="16">
        <el-card class="globe-card" shadow="never">
          <template #header>
            <div class="card-head">
              <div class="card-head__main">
                <span class="card-title">在线用户分布</span>
                <span v-if="selectedRegion" class="card-subtitle">{{ selectedRegion.name }}</span>
              </div>
              <div class="card-head__extra">
                <span v-if="!selectedRegion" class="card-extra">
                  {{ onlineCount }} 个会话 · {{ regionStats.length }} 个地区
                </span>
                <el-button v-else link type="primary" icon="Back" @click="clearRegion">返回地球</el-button>
              </div>
            </div>
          </template>

          <div class="globe-body">
            <div class="globe-canvas" :class="{ 'is-map': selectedRegion }">
              <transition name="globe-swap" mode="out-in">
                <cobe-globe
                  v-if="!selectedRegion"
                  :key="'globe-' + globeKey"
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
                  @marker-click="handleMarkerClick"
                  @marker-hover="handleMarkerHover"
                />
                <location-map
                  v-else
                  :key="'map-' + selectedRegion.key"
                  :location="selectedRegion.name"
                  :coordinates="selectedCoordinates"
                  :count="selectedRegion.count"
                  :meta="selectedMeta"
                />
              </transition>
              <p v-if="!onlineCount && !selectedRegion" class="globe-empty">当前没有在线会话</p>
            </div>

            <div class="globe-side">
              <div v-if="!selectedRegion" class="globe-stats">
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

              <div v-else class="globe-stats">
                <div class="globe-stat">
                  <span class="globe-stat__label">该地区会话</span>
                  <span class="globe-stat__value">{{ selectedRegion.count }}</span>
                </div>
                <div class="globe-stat">
                  <span class="globe-stat__label">独立用户</span>
                  <span class="globe-stat__value">{{ selectedUserCount }}</span>
                </div>
                <div class="globe-stat">
                  <span class="globe-stat__label">占在线总数</span>
                  <span class="globe-stat__value">{{ selectedRegion.share }}%</span>
                </div>
                <div class="globe-stat">
                  <span class="globe-stat__label">地区类型</span>
                  <span class="globe-stat__value globe-stat__value--text">{{ selectedKindLabel }}登录</span>
                </div>
              </div>

              <p class="globe-hint">
                <template v-if="!selectedRegion">
                  拖动地球可旋转查看；点击标点或右侧地区切换到该地址街区，悬停标点可查看地区名称
                </template>
                <template v-else>
                  已锁定 {{ selectedRegion.name }}，右侧为同一地址的在线用户；点击「返回地球」查看全部地区
                </template>
              </p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card class="region-card" shadow="never">
          <template #header>
            <div class="card-head">
              <span class="card-title">{{ selectedRegion ? "当前地址用户" : "地区分布" }}</span>
              <span class="card-extra">
                {{ selectedRegion ? selectedRegion.name + " · " + selectedUsers.length + " 个会话" : "按登录地点聚合" }}
              </span>
            </div>
          </template>

          <transition name="panel-swap" mode="out-in">
            <div v-if="selectedRegion" :key="'users-' + selectedRegion.key" class="region-users">
              <ul v-if="selectedUsers.length" class="region-user-list">
                <li
                  v-for="(user, index) in selectedUsers"
                  :key="user.tokenId"
                  class="region-user"
                  :style="{ animationDelay: index * 0.05 + 's' }"
                >
                  <span class="region-user__avatar">{{ (user.userName || "?").slice(0, 1).toUpperCase() }}</span>
                  <div class="region-user__main">
                    <div class="region-user__top">
                      <span class="region-user__name">{{ user.userName }}</span>
                      <span v-if="user.deptName" class="region-user__dept">{{ user.deptName }}</span>
                    </div>
                    <div class="region-user__meta">
                      <span class="region-user__ip">{{ user.ipaddr || "-" }}</span>
                      <span class="region-user__sep">·</span>
                      <span class="region-user__browser">{{ user.browser || "未知浏览器" }}</span>
                    </div>
                  </div>
                  <div class="region-user__side">
                    <span class="region-user__time">{{ parseTime(user.loginTime, "{m}-{d} {h}:{i}") }}</span>
                    <el-button
                      link
                      type="danger"
                      v-hasPermi="['monitor:online:forceLogout']"
                      @click="handleForceLogout(user)"
                    >
                      强退
                    </el-button>
                  </div>
                </li>
              </ul>
              <el-empty v-else description="该地址暂无在线会话" :image-size="64" />
              <p class="region-foot">同一地址 {{ selectedRegion.count }} 个会话 · 数据来自 Redis 在线会话</p>
            </div>

            <div v-else key="regions" class="region-overview">
              <ul v-if="regionStats.length" class="region-list">
                <li
                  v-for="region in regionStats"
                  :key="region.key"
                  class="region-item"
                  :class="{ 'is-active': hoverKey === region.key }"
                  role="button"
                  tabindex="0"
                  @mouseenter="hoverKey = region.key"
                  @mouseleave="hoverKey = ''"
                  @click="selectRegion(region)"
                  @keydown.enter.prevent="selectRegion(region)"
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
            </div>
          </transition>
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
import LocationMap from "@/components/LocationMap/index.vue";
import { resolveLoginLocation } from "@/utils/geo-city";

const { proxy } = getCurrentInstance();

const onlineList = ref([]);
const loading = ref(true);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const hoverKey = ref("");
const globeKey = ref(0);
/** 当前锁定的登录地点 key，为空时展示全球视图 */
const selectedKey = ref("");

const queryParams = ref({
  ipaddr: undefined,
  userName: undefined
});

const REGION_COLORS = ["#00beb4", "#5b8ff9", "#f7863b", "#9270ca", "#3ba05c", "#e8b30b"];
const KIND_LABELS = { intranet: "内网", city: "城市", unknown: "未知" };

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

/**
 * 地球上的点位：每个地区一个，数量越多点越大。
 * 标签会互相压盖，所以只常显会话最多的地区；其余地区在悬停时显示。
 */
const globeMarkers = computed(() =>
  regionStats.value.map((region, index) => {
    const active = hoverKey.value === region.key;
    // 只有单个地区时常显标签；多个地区挤在一起时标签会互相压盖，改为悬停显示
    const alwaysLabel = regionStats.value.length === 1;
    return {
      id: "loc-" + region.key,
      regionKey: region.key,
      location: [region.lat, region.lng],
      label: active || alwaysLabel ? region.name + " · " + region.count : "",
      size: (0.018 + Math.min(0.026, Math.sqrt(region.count) * 0.008)) * (active ? 1.45 : 1)
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

const selectedRegion = computed(
  () => regionStats.value.find((region) => region.key === selectedKey.value) || null
);

const selectedUsers = computed(() => {
  if (!selectedRegion.value) return [];
  return onlineList.value.filter(
    (row) => resolveLoginLocation(row.loginLocation).key === selectedRegion.value.key
  );
});

const selectedUserCount = computed(() => new Set(selectedUsers.value.map((row) => row.userName)).size);
const selectedKindLabel = computed(() =>
  selectedRegion.value ? KIND_LABELS[selectedRegion.value.kind] || "未知" : ""
);

function formatCoordinates(lat, lng) {
  const ns = lat >= 0 ? "N" : "S";
  const ew = lng >= 0 ? "E" : "W";
  return Math.abs(lat).toFixed(4) + "° " + ns + ", " + Math.abs(lng).toFixed(4) + "° " + ew;
}

const selectedCoordinates = computed(() =>
  selectedRegion.value ? formatCoordinates(selectedRegion.value.lat, selectedRegion.value.lng) : ""
);

/** 地图卡片上的一行补充信息：该地址的登录 IP */
const selectedMeta = computed(() => {
  if (!selectedRegion.value) return "";
  const ips = [...new Set(selectedUsers.value.map((row) => row.ipaddr).filter(Boolean))];
  if (!ips.length) return selectedUsers.value.length + " 个会话";
  const shown = ips.slice(0, 2).join(" / ");
  return "IP " + shown + (ips.length > 2 ? " 等 " + ips.length + " 个" : "");
});

/** 点击地球上的标点 → 切换到该地址 */
function handleMarkerClick(marker) {
  if (marker?.regionKey) {
    selectedKey.value = marker.regionKey;
    hoverKey.value = "";
  }
}

/** 鼠标悬停在地球标点上：联动高亮右侧地区行，并显示该地区标签 */
function handleMarkerHover(marker) {
  hoverKey.value = marker?.regionKey || "";
}

function selectRegion(region) {
  selectedKey.value = region.key;
  hoverKey.value = "";
}

function clearRegion() {
  selectedKey.value = "";
  hoverKey.value = "";
}

// 在线会话刷新后，如果锁定的地区已经没有会话，自动回到全球视图
watch(regionStats, (list) => {
  if (selectedKey.value && !list.some((region) => region.key === selectedKey.value)) {
    selectedKey.value = "";
  }
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

  &__main {
    display: flex;
    align-items: baseline;
    gap: 8px;
    min-width: 0;
  }

  &__extra {
    display: flex;
    align-items: center;
    flex: none;
  }

  .card-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--app-text, #161618);
  }

  .card-subtitle {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 12px;
    color: var(--el-color-primary, #00beb4);
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
  overflow: hidden;
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

  &.is-map {
    flex-basis: 430px;
    max-width: 520px;
  }
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

    &--text {
      font-size: 16px;
      line-height: 1.4;
    }
  }
}

.globe-hint {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-secondary, #75787a);
}

/* 地球 ↔ 地址街区 的切换动效 */
.globe-swap-enter-active {
  animation: view-in 0.5s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

.globe-swap-leave-active {
  animation: view-out 0.26s ease both;
}

@keyframes view-in {
  from {
    opacity: 0;
    transform: scale(0.88);
    filter: blur(6px);
  }
  to {
    opacity: 1;
    transform: scale(1);
    filter: blur(0);
  }
}

@keyframes view-out {
  from {
    opacity: 1;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(1.06);
    filter: blur(8px);
  }
}

.region-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.region-overview {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 150px;
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
  transition: background-color 0.2s ease, transform 0.2s ease;
  cursor: pointer;

  &.is-active {
    background: var(--el-fill-color-light, #f4f5f6);
    transform: translateX(2px);
  }

  &:focus-visible {
    outline: 2px solid var(--el-color-primary-light-5, #80dfd9);
    outline-offset: 1px;
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

/* 同一地址的在线用户列表 */
.region-users {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 150px;
}

.region-user-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 340px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.region-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 8px 8px 6px;
  border-radius: 10px;
  animation: user-in 0.42s cubic-bezier(0.22, 0.61, 0.36, 1) both;
  transition: background-color 0.2s ease;

  &:hover {
    background: var(--el-fill-color-light, #f4f5f6);
  }

  &__avatar {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: none;
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: var(--el-color-primary-light-9, #e6f9f7);
    color: var(--el-color-primary-dark-1, #00aba2);
    font-size: 13px;
    font-weight: 600;
  }

  &__main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  &__top {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
    font-size: 13px;
    color: var(--app-text, #161618);
  }

  &__name {
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__dept {
    flex: none;
    max-width: 96px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding: 0 6px;
    border-radius: 4px;
    background: var(--el-fill-color-light, #f4f5f6);
    font-size: 11px;
    line-height: 18px;
    color: var(--app-text-secondary, #75787a);
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 4px;
    min-width: 0;
    font-size: 11px;
    color: var(--app-text-secondary, #75787a);
  }

  &__ip {
    font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  }

  &__sep {
    opacity: 0.6;
  }

  &__browser {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__side {
    flex: none;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 2px;

    :deep(.el-button.is-link) {
      height: 16px;
      padding: 0;
      font-size: 11px;
      color: var(--el-color-danger);
    }
  }

  &__time {
    font-size: 11px;
    font-variant-numeric: tabular-nums;
    color: var(--app-text-secondary, #75787a);
  }
}

@keyframes user-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.panel-swap-enter-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.panel-swap-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.panel-swap-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.panel-swap-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.table-card {
  border-radius: 12px;

  .table-query {
    margin-bottom: 4px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .globe-swap-enter-active,
  .globe-swap-leave-active,
  .region-user {
    animation: none;
  }

  .panel-swap-enter-active,
  .panel-swap-leave-active {
    transition: none;
  }
}

@media (max-width: 1199px) {
  .globe-canvas {
    flex-basis: 280px;

    &.is-map {
      flex-basis: 100%;
      max-width: none;
    }
  }

  .globe-card,
  .region-card {
    height: auto;
  }

  .region-card :deep(.el-card__body) {
    height: auto;
    min-height: 150px;
  }

  .region-user-list {
    max-height: 320px;
  }
}
</style>
