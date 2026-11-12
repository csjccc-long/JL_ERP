<template>
  <div class="location-map" @mouseleave="resetTilt">
    <div
      ref="stageRef"
      class="location-map__stage"
      @mousemove="handleMouseMove"
      @mouseenter="hovered = true"
      @mouseleave="hovered = false"
    >
      <div class="location-map__panel" :style="panelStyle">
        <!-- 地址街区：模拟地图底图，进入时逐条绘制 -->
        <div class="location-map__map" aria-hidden="true">
          <svg class="location-map__roads" viewBox="0 0 100 100" preserveAspectRatio="none">
            <line
              v-for="road in roads"
              :key="road.id"
              class="location-map__road"
              :x1="road.x1"
              :y1="road.y1"
              :x2="road.x2"
              :y2="road.y2"
              :stroke-width="road.weight"
              :style="{ opacity: road.opacity, animationDelay: road.delay + 's' }"
              vector-effect="non-scaling-stroke"
            />
          </svg>

          <span
            v-for="(building, index) in buildings"
            :key="index"
            class="location-map__building"
            :style="{
              top: building.top,
              left: building.left,
              width: building.width,
              height: building.height,
              '--tone': building.opacity,
              animationDelay: building.delay + 's'
            }"
          />

          <span class="location-map__pin">
            <span class="location-map__ping" />
            <svg width="34" height="34" viewBox="0 0 24 24" fill="none">
              <path
                d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"
                fill="#34D399"
              />
              <circle cx="12" cy="9" r="2.5" fill="var(--el-bg-color)" />
            </svg>
          </span>

          <span class="location-map__veil" />
        </div>

        <div class="location-map__content">
          <div class="location-map__top">
            <svg
              class="location-map__icon"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polygon points="3 6 9 3 15 6 21 3 21 18 15 21 9 18 3 21" />
              <line x1="9" x2="9" y1="3" y2="18" />
              <line x1="15" x2="15" y1="6" y2="21" />
            </svg>

            <span class="location-map__badge">
              <i class="location-map__dot" />
              <span>LIVE</span>
              <span v-if="count" class="location-map__count">· {{ count }}</span>
            </span>
          </div>

          <div class="location-map__bottom">
            <h3 class="location-map__title">{{ location }}</h3>
            <p class="location-map__coords">{{ coordinates }}</p>
            <p v-if="meta" class="location-map__meta">{{ meta }}</p>
            <span class="location-map__line" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="LocationMap">
import { computed, ref } from "vue";

const props = defineProps({
  /** 地址名称，如「内网 · 深圳」 */
  location: { type: String, default: "San Francisco, CA" },
  /** 经纬度文本，如「37.7749° N, 122.4194° W」 */
  coordinates: { type: String, default: "" },
  /** 同一地址的在线会话数，展示在 LIVE 徽标后 */
  count: { type: Number, default: 0 },
  /** 附加信息，如登录 IP */
  meta: { type: String, default: "" }
});

const stageRef = ref(null);
const hovered = ref(false);
const rotateX = ref(0);
const rotateY = ref(0);

const panelStyle = computed(() => ({
  transform: "rotateX(" + rotateX.value + "deg) rotateY(" + rotateY.value + "deg)"
}));

/** 街区道路，坐标是百分数（0–100），与 viewBox 对齐 */
const roads = [
  { id: "main-h-1", x1: 0, y1: 35, x2: 100, y2: 35, weight: 4, opacity: 0.25, delay: 0.2 },
  { id: "main-h-2", x1: 0, y1: 65, x2: 100, y2: 65, weight: 4, opacity: 0.25, delay: 0.28 },
  { id: "main-v-1", x1: 30, y1: 0, x2: 30, y2: 100, weight: 3, opacity: 0.2, delay: 0.36 },
  { id: "main-v-2", x1: 70, y1: 0, x2: 70, y2: 100, weight: 3, opacity: 0.2, delay: 0.42 },
  { id: "sub-h-1", x1: 0, y1: 20, x2: 100, y2: 20, weight: 1.5, opacity: 0.1, delay: 0.5 },
  { id: "sub-h-2", x1: 0, y1: 50, x2: 100, y2: 50, weight: 1.5, opacity: 0.1, delay: 0.56 },
  { id: "sub-h-3", x1: 0, y1: 80, x2: 100, y2: 80, weight: 1.5, opacity: 0.1, delay: 0.62 },
  { id: "sub-v-1", x1: 15, y1: 0, x2: 15, y2: 100, weight: 1.5, opacity: 0.1, delay: 0.68 },
  { id: "sub-v-2", x1: 45, y1: 0, x2: 45, y2: 100, weight: 1.5, opacity: 0.1, delay: 0.72 },
  { id: "sub-v-3", x1: 55, y1: 0, x2: 55, y2: 100, weight: 1.5, opacity: 0.1, delay: 0.76 },
  { id: "sub-v-4", x1: 85, y1: 0, x2: 85, y2: 100, weight: 1.5, opacity: 0.1, delay: 0.8 }
];

/** 街区建筑块 */
const buildings = [
  { top: "40%", left: "10%", width: "15%", height: "20%", opacity: 0.3, delay: 0.42 },
  { top: "15%", left: "35%", width: "12%", height: "15%", opacity: 0.25, delay: 0.48 },
  { top: "70%", left: "75%", width: "18%", height: "18%", opacity: 0.28, delay: 0.54 },
  { top: "20%", left: "76%", width: "10%", height: "25%", opacity: 0.22, delay: 0.6 },
  { top: "55%", left: "5%", width: "8%", height: "12%", opacity: 0.2, delay: 0.66 },
  { top: "8%", left: "58%", width: "14%", height: "10%", opacity: 0.22, delay: 0.72 }
];

/** 鼠标位置 → 轻微 3D 倾斜，范围 ±8° */
function handleMouseMove(event) {
  const stage = stageRef.value;
  if (!stage) return;
  const rect = stage.getBoundingClientRect();
  if (!rect.width || !rect.height) return;
  const offsetX = event.clientX - rect.left - rect.width / 2;
  const offsetY = event.clientY - rect.top - rect.height / 2;
  rotateX.value = Math.max(-8, Math.min(8, (-offsetY / rect.height) * 16));
  rotateY.value = Math.max(-8, Math.min(8, (offsetX / rect.width) * 16));
}

function resetTilt() {
  hovered.value = false;
  rotateX.value = 0;
  rotateY.value = 0;
}
</script>

<style scoped lang="scss">
.location-map {
  width: 100%;
}

.location-map__stage {
  position: relative;
  width: 100%;
  height: 340px;
  perspective: 1000px;
}

.location-map__panel {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light, rgba(22, 22, 24, 0.09));
  border-radius: 16px;
  background: var(--el-bg-color, #ffffff);
  box-shadow: var(--app-shadow-card, 0 1px 2px rgba(16, 24, 40, 0.04));
  transform-style: preserve-3d;
  transition: transform 0.18s ease-out, box-shadow 0.3s ease;
  will-change: transform;
}

.location-map__panel:hover {
  box-shadow: 0 10px 30px rgba(16, 24, 40, 0.09);
}

/* 街区底图 */
.location-map__map {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(120% 90% at 50% 50%, var(--el-fill-color-lighter, #f8f8f9) 0%, var(--el-fill-color-light, #f4f5f6) 100%);
  color: var(--el-text-color-primary, #161618);
}

.location-map__roads {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.location-map__road {
  stroke: currentColor;
  stroke-linecap: round;
  stroke-dasharray: 140;
  stroke-dashoffset: 140;
  animation: lm-draw 0.9s cubic-bezier(0.22, 0.61, 0.36, 1) forwards;
}

.location-map__building {
  position: absolute;
  border-radius: 2px;
  background: currentColor;
  transform: scale(0.82);
  opacity: 0;
  animation: lm-pop 0.45s cubic-bezier(0.22, 0.61, 0.36, 1) forwards;
}

/* 中心位置图钉 */
.location-map__pin {
  position: absolute;
  top: 50%;
  left: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  translate: -50% -100%;
  filter: drop-shadow(0 0 10px rgba(52, 211, 153, 0.5));
  animation: lm-pin 0.6s cubic-bezier(0.22, 0.61, 0.36, 1) 0.3s both;
}

.location-map__ping {
  position: absolute;
  top: -4px;
  left: 50%;
  width: 34px;
  height: 34px;
  translate: -50% 0;
  border-radius: 50%;
  border: 1px solid rgba(52, 211, 153, 0.55);
  animation: lm-ping 2.4s ease-out 1s infinite;
}

.location-map__veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to top,
    var(--el-bg-color, #ffffff) 0%,
    color-mix(in srgb, var(--el-bg-color, #ffffff) 72%, transparent) 34%,
    transparent 66%
  );
  pointer-events: none;
}

.location-map__content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 100%;
  padding: 18px 20px;
}

.location-map__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.location-map__icon {
  color: #34d399;
  filter: drop-shadow(0 0 4px rgba(52, 211, 153, 0.3));
  transition: filter 0.3s ease, transform 0.3s ease;
}

.location-map__panel:hover .location-map__icon {
  filter: drop-shadow(0 0 8px rgba(52, 211, 153, 0.65));
  transform: translateY(-1px);
}

.location-map__badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 9px;
  border-radius: 999px;
  border: 1px solid var(--el-border-color-lighter, rgba(22, 22, 24, 0.07));
  background: var(--el-bg-color, #ffffff);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--el-text-color-secondary, #75787a);
  transition: transform 0.25s ease;
}

.location-map__panel:hover .location-map__badge {
  transform: scale(1.05);
}

.location-map__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 0 3px rgba(52, 211, 153, 0.16);
  animation: lm-blink 2.4s ease-in-out infinite;
}

.location-map__count {
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-primary, #161618);
}

.location-map__bottom {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.location-map__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: -0.01em;
  color: var(--el-text-color-primary, #161618);
  transition: transform 0.3s cubic-bezier(0.22, 0.61, 0.36, 1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-map__panel:hover .location-map__title {
  transform: translateX(4px);
}

.location-map__coords {
  margin: 0;
  font-family: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.02em;
  color: var(--el-text-color-secondary, #75787a);
}

.location-map__meta {
  margin: 0;
  font-size: 11px;
  line-height: 1.5;
  color: var(--el-text-color-secondary, #75787a);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-map__line {
  display: block;
  width: 100%;
  height: 1px;
  margin-top: 6px;
  transform: scaleX(0.35);
  transform-origin: left center;
  background: linear-gradient(90deg, rgba(52, 211, 153, 0.55), rgba(52, 211, 153, 0.28), transparent);
  transition: transform 0.45s cubic-bezier(0.22, 0.61, 0.36, 1);
}

.location-map__panel:hover .location-map__line {
  transform: scaleX(1);
}

@keyframes lm-draw {
  to {
    stroke-dashoffset: 0;
  }
}

@keyframes lm-pop {
  to {
    transform: scale(1);
    opacity: var(--tone, 0.24);
  }
}

@keyframes lm-pin {
  from {
    opacity: 0;
    transform: translateY(-18px) scale(0.6);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes lm-ping {
  0% {
    opacity: 0.7;
    transform: scale(0.7);
  }
  70%,
  100% {
    opacity: 0;
    transform: scale(1.9);
  }
}

@keyframes lm-blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.45;
  }
}

@media (max-width: 1199px) {
  .location-map__stage {
    height: 280px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .location-map__road,
  .location-map__building,
  .location-map__pin,
  .location-map__ping,
  .location-map__dot {
    animation: none;
  }

  .location-map__road {
    stroke-dashoffset: 0;
  }

  .location-map__building {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
