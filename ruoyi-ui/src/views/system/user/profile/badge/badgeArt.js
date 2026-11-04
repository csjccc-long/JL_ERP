/**
 * 工卡卡面的"混合晕染"底色。
 *
 * 与文件管理里玻璃文件夹的封面是同一套算法：把一张域扭曲两次的噪声当作液面，
 * 用它的坡度算光，再沿等值线拉出细丝，最后按调色板着色。这里只保留渲染一次
 * 所需的部分，尺寸按卡面大小来，画完就不再重算。
 */

export const BADGE_PALETTES = [
  {
    key: "aurora",
    label: "极光",
    light: true,
    accent: "#a63c38",
    ink: "#1b1518",
    style: "streak",
    colors: ["#f4efe9", "#e9c2b8", "#c2433f", "#2a1e2a", "#22395e", "#6f97c2", "#e5edf4"],
    scale: 0.7,
    warp: 1.4,
    angle: -0.62,
    gloss: 0.18,
    veins: 0
  },
  {
    key: "ember",
    label: "余烬",
    light: false,
    accent: "#f4633a",
    ink: "#faf1e9",
    style: "marble",
    colors: ["#07090c", "#16252c", "#3d5f68", "#1a1416", "#a3201a", "#ef4a2b", "#ff8a5c"],
    scale: 0.62,
    warp: 3.2,
    angle: -0.5,
    gloss: 0.55,
    veins: 0.6
  },
  {
    key: "dusk",
    label: "暮色",
    light: false,
    accent: "#eab08a",
    ink: "#f8f1ea",
    style: "marble",
    colors: ["#120c18", "#3e2850", "#8f6c9c", "#2b1a24", "#b3502f", "#eab08a", "#fbe7d2"],
    scale: 0.75,
    warp: 3.6,
    angle: 0.8,
    gloss: 0.5,
    veins: 0.5
  },
  {
    key: "glacier",
    label: "冰川",
    light: true,
    accent: "#5f7f9c",
    ink: "#151a21",
    style: "streak",
    colors: ["#f7f8fa", "#d9e2ea", "#9fb4c7", "#e8edf2", "#bccbd8", "#ffffff"],
    scale: 0.7,
    warp: 1.6,
    angle: 0.35,
    gloss: 0.25,
    veins: 0
  },
  {
    key: "verdant",
    label: "苔绿",
    light: false,
    accent: "#e9cf87",
    ink: "#f5f3e6",
    style: "marble",
    colors: ["#070b08", "#173024", "#4f7a5a", "#141812", "#a7843a", "#e9cf87", "#fbf3d6"],
    scale: 0.7,
    warp: 3.4,
    angle: 2.1,
    gloss: 0.5,
    veins: 0.5
  },
  {
    key: "abyss",
    label: "深海",
    light: false,
    accent: "#79c8e8",
    ink: "#eaf6fb",
    style: "marble",
    colors: ["#03060c", "#0a1a30", "#1c4a72", "#08101f", "#2f7fb0", "#79c8e8", "#d6effb"],
    scale: 0.68,
    warp: 3.5,
    angle: 1.4,
    gloss: 0.6,
    veins: 0.55
  },
  {
    key: "iris",
    label: "紫雾",
    light: false,
    accent: "#c3a1f2",
    ink: "#f4eeff",
    style: "marble",
    colors: ["#080510", "#1e1330", "#452c66", "#120c1e", "#6a4d9e", "#c3a1f2", "#ecdcff"],
    scale: 0.72,
    warp: 3.1,
    angle: -0.9,
    gloss: 0.45,
    veins: 0.5
  },
  {
    key: "sand",
    label: "沙丘",
    light: true,
    accent: "#a8794f",
    ink: "#2a2118",
    style: "streak",
    colors: ["#faf4e8", "#eddcbe", "#cfa87c", "#8a6844", "#b98a5c", "#f3e8d6", "#ffffff"],
    scale: 0.66,
    warp: 1.8,
    angle: 0.55,
    gloss: 0.3,
    veins: 0
  },
  {
    key: "neon",
    label: "霓虹",
    light: false,
    accent: "#4ce0b3",
    ink: "#e9fff8",
    style: "marble",
    colors: ["#03080a", "#082a2b", "#0f6f5f", "#0b1418", "#17b892", "#4ce0b3", "#d2fff2"],
    scale: 0.7,
    warp: 3.7,
    angle: 2.4,
    gloss: 0.58,
    veins: 0.6
  },
  {
    key: "clay",
    label: "陶土",
    light: false,
    accent: "#d97757",
    ink: "#fbeee6",
    style: "marble",
    colors: ["#0f0908", "#33190f", "#8a462b", "#160f0c", "#bf6236", "#d97757", "#f6d9c9"],
    scale: 0.64,
    warp: 3.3,
    angle: -0.35,
    gloss: 0.5,
    veins: 0.45
  }
];

export const DEFAULT_PALETTE = "aurora";

export const paletteOf = key => BADGE_PALETTES.find(p => p.key === key) || BADGE_PALETTES[0];

/** `#rgb` 或 `#rrggbb` 转三元组；读不出来时给中灰。 */
function hexToRgb(hex) {
  let h = String(hex).trim().replace(/^#/, "");
  if (h.length === 3) h = h[0] + h[0] + h[1] + h[1] + h[2] + h[2];
  const n = /^[0-9a-f]{6}$/i.test(h) ? parseInt(h, 16) : 0x808080;
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

/** 可复现的 mulberry32。 */
function rng(seed) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** 梯度噪声，多倍频叠加，每层之间转一下坐标。 */
function makeFbm(seed) {
  const r = rng(seed);
  const perm = new Uint8Array(512);
  const grad = new Float32Array(512);
  const p = [];
  for (let i = 0; i < 256; i++) p.push(i);
  for (let i = 255; i > 0; i--) {
    const j = Math.floor(r() * (i + 1));
    const t = p[i];
    p[i] = p[j];
    p[j] = t;
  }
  for (let i = 0; i < 512; i++) perm[i] = p[i & 255];
  for (let i = 0; i < 256; i++) {
    const a = r() * Math.PI * 2;
    grad[i * 2] = Math.cos(a);
    grad[i * 2 + 1] = Math.sin(a);
  }
  const noise = (x, y) => {
    const xi = Math.floor(x);
    const yi = Math.floor(y);
    const xf = x - xi;
    const yf = y - yi;
    const X = xi & 255;
    const Y = yi & 255;
    const h00 = perm[X + perm[Y]] * 2;
    const h10 = perm[X + 1 + perm[Y]] * 2;
    const h01 = perm[X + perm[Y + 1]] * 2;
    const h11 = perm[X + 1 + perm[Y + 1]] * 2;
    const d00 = grad[h00] * xf + grad[h00 + 1] * yf;
    const d10 = grad[h10] * (xf - 1) + grad[h10 + 1] * yf;
    const d01 = grad[h01] * xf + grad[h01 + 1] * (yf - 1);
    const d11 = grad[h11] * (xf - 1) + grad[h11 + 1] * (yf - 1);
    const u = xf * xf * xf * (xf * (xf * 6 - 15) + 10);
    const v = yf * yf * yf * (yf * (yf * 6 - 15) + 10);
    return d00 + u * (d10 - d00) + v * (d01 - d00 + u * (d11 - d01 - d10 + d00));
  };
  return (x, y, octaves) => {
    let sum = 0;
    let amp = 0.5;
    for (let i = 0; i < octaves; i++) {
      sum += amp * noise(x, y);
      const nx = 1.6 * x + 1.2 * y + 7.1;
      y = -1.2 * x + 1.6 * y + 3.3;
      x = nx;
      amp *= 0.5;
    }
    return sum;
  };
}

/**
 * 把一块卡面画进 canvas：同一套调色板与种子永远画出同一张图。
 * 分辨率按半分辨率取场、再三次上采样，所以画得很快。
 */
export function paintWash(canvas, palette, seed = 7) {
  if (!canvas) return;
  const w = Math.max(1, Math.round(canvas.width));
  const h = Math.max(1, Math.round(canvas.height));
  const colors = palette.colors.length >= 2 ? palette.colors : ["#111111", "#eeeeee"];
  const stops = colors.map((c, i) => ({ t: i / (colors.length - 1), c: hexToRgb(c) }));
  const scale = palette.scale ?? 0.7;
  const warp = palette.warp ?? 3;
  const gloss = palette.gloss ?? 0.4;
  const veins = palette.veins ?? 0;
  const streak = palette.style === "streak";
  const fbm = makeFbm(seed * 977 + 13);
  const r = rng(seed + 1);
  const ox = r() * 50;
  const oy = r() * 50;
  const angle = (palette.angle ?? 0) + (r() - 0.5) * 0.5;
  const ca = Math.cos(angle);
  const sa = Math.sin(angle);
  const m = Math.min(w, h);
  const gw = Math.ceil(w / 2) + 3;
  const gh = Math.ceil(h / 2) + 3;
  const G = new Float32Array(gw * gh);
  const GQ = new Float32Array(gw * gh);
  const F = new Float32Array(w * h);
  const Q = new Float32Array(w * h);
  const pixels = new Uint8ClampedArray(w * h * 4);

  for (let gy = 0; gy < gh; gy++) {
    for (let gx = 0; gx < gw; gx++) {
      const u = ((gx - 1) * 2 - w / 2) / m;
      const v = ((gy - 1) * 2 - h / 2) / m;
      let px = (u * ca - v * sa) * scale;
      let py = (u * sa + v * ca) * scale;
      if (streak) {
        px *= 0.14;
        py *= 2.2;
      }
      px += ox;
      py += oy;
      const qx = fbm(px, py, 3);
      const qy = fbm(px + 5.2, py + 1.3, 3);
      const rx = fbm(px + warp * qx + 1.7, py + warp * qy + 9.2, 3);
      const ry = fbm(px + warp * qx + 8.3, py + warp * qy + 2.8, 3);
      G[gy * gw + gx] = fbm(px + warp * rx, py + warp * ry, 4);
      GQ[gy * gw + gx] = Math.sqrt(qx * qx + qy * qy);
    }
  }

  const cr = (a, b, c, d) => (-a + 9 * b + 9 * c - d) / 16;
  const T = new Float32Array(w * gh);
  for (let gy = 0; gy < gh; gy++) {
    const rr = gy * gw;
    for (let x = 0; x < w; x++) {
      const c = (x >> 1) + 1;
      T[gy * w + x] = x & 1 ? cr(G[rr + c - 1], G[rr + c], G[rr + c + 1], G[rr + c + 2]) : G[rr + c];
    }
  }
  for (let y = 0; y < h; y++) {
    const c = (y >> 1) + 1;
    for (let x = 0; x < w; x++) {
      F[y * w + x] = y & 1 ? cr(T[(c - 1) * w + x], T[c * w + x], T[(c + 1) * w + x], T[(c + 2) * w + x]) : T[c * w + x];
      const gx = (x >> 1) + 1;
      const q0 = GQ[c * gw + gx];
      const qx = x & 1 ? (q0 + GQ[c * gw + gx + 1]) / 2 : q0;
      const q1 = y & 1 ? (x & 1 ? (GQ[(c + 1) * gw + gx] + GQ[(c + 1) * gw + gx + 1]) / 2 : GQ[(c + 1) * gw + gx]) : qx;
      Q[y * w + x] = (qx + q1) / 2;
    }
  }

  const sample = [];
  const stride = Math.max(1, Math.floor(F.length / 4096));
  for (let i = 0; i < F.length; i += stride) sample.push(F[i]);
  sample.sort((a, b) => a - b);
  const lo = sample[Math.floor(sample.length * 0.02)];
  const hi = sample[Math.floor(sample.length * 0.98)];
  const k = 1 / (hi - lo || 1);
  const lx = -0.55;
  const ly = -0.65;
  const lz = 0.52;
  const slope = k * m * 0.012;
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const i = y * w + x;
      let t = (F[i] - lo) * k;
      t = t < 0 ? 0 : t > 1 ? 1 : t;
      let j = 1;
      while (j < stops.length - 1 && stops[j].t < t) j++;
      const a = stops[j - 1];
      const b = stops[j];
      let f = (t - a.t) / (b.t - a.t || 1);
      f = f < 0 ? 0 : f > 1 ? 1 : f;
      f = f * f * (3 - 2 * f);
      const gx = (F[x < w - 1 ? i + 1 : i] - F[x > 0 ? i - 1 : i]) * slope;
      const gy = (F[y < h - 1 ? i + w : i] - F[y > 0 ? i - w : i]) * slope;
      const nd = (-gx * lx - gy * ly + lz) / Math.sqrt(gx * gx + gy * gy + 1);
      const spec = Math.pow(nd > 0 ? nd : 0, 24) * gloss * 255;
      const silk = 1 + 0.035 * Math.sin(t * 150 + Q[i] * 24);
      const vein = veins * Math.max(0, 1 - Math.abs(Q[i] - 0.42) * 14) * 0.6;
      const lit = (0.82 + 0.28 * nd) * silk * (1 - vein);
      pixels[i * 4] = (a.c[0] + (b.c[0] - a.c[0]) * f) * lit + spec;
      pixels[i * 4 + 1] = (a.c[1] + (b.c[1] - a.c[1]) * f) * lit + spec;
      pixels[i * 4 + 2] = (a.c[2] + (b.c[2] - a.c[2]) * f) * lit + spec;
      pixels[i * 4 + 3] = 255;
    }
  }

  const ctx = canvas.getContext("2d");
  if (!ctx) return;
  ctx.putImageData(new ImageData(pixels, w, h), 0, 0);
}

function rgbToTone(r, g, b) {
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const d = max - min;
  let hue = 0;
  if (d > 0.0001) {
    if (max === r) hue = ((g - b) / d) % 6;
    else if (max === g) hue = (b - r) / d + 2;
    else hue = (r - g) / d + 4;
    hue *= 60;
    if (hue < 0) hue += 360;
  }
  const l = (max + min) / 2;
  const sat = d < 0.0001 ? 0 : d / (1 - Math.abs(2 * l - 1));
  return { lum: 0.2126 * r + 0.7152 * g + 0.0722 * b, hue, sat };
}

/** 卡面整体色调（每 4px 采样平均），logo 取同一个色相才不会和卡面脱节。 */
export function washTone(canvas) {
  const ctx = canvas.getContext("2d");
  if (!ctx) return { lum: 0.2, hue: 0, sat: 0 };
  const w = canvas.width;
  const h = canvas.height;
  let data;
  try {
    data = ctx.getImageData(0, 0, w, h).data;
  } catch (e) {
    return { lum: 0.2, hue: 0, sat: 0 };
  }
  let sr = 0;
  let sg = 0;
  let sb = 0;
  let count = 0;
  for (let y = 0; y < h; y += 4) {
    for (let x = 0; x < w; x += 4) {
      const i = (y * w + x) * 4;
      sr += data[i] / 255;
      sg += data[i + 1] / 255;
      sb += data[i + 2] / 255;
      count++;
    }
  }
  if (!count) return { lum: 0.2, hue: 0, sat: 0 };
  return rgbToTone(sr / count, sg / count, sb / count);
}

/** 卡面平均亮度（每 4px 采样），用来判断这块晕染上该用浅色还是深色字。 */
export function washLuminance(canvas) {
  const ctx = canvas.getContext("2d");
  if (!ctx) return 0.2;
  const w = canvas.width;
  const h = canvas.height;
  let data;
  try {
    data = ctx.getImageData(0, 0, w, h).data;
  } catch (e) {
    return 0.2;
  }
  let sum = 0;
  let count = 0;
  for (let y = 0; y < h; y += 4) {
    for (let x = 0; x < w; x += 4) {
      const i = (y * w + x) * 4;
      const r = data[i] / 255;
      const g = data[i + 1] / 255;
      const b = data[i + 2] / 255;
      sum += 0.2126 * r + 0.7152 * g + 0.0722 * b;
      count++;
    }
  }
  return count ? sum / count : 0.2;
}
