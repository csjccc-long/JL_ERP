<template>
  <div ref="hostRef" class="dithering-surface" :style="hostStyle">
    <canvas ref="canvasRef" class="dithering-surface__canvas"></canvas>
  </div>
</template>

<script setup name="DitheringSurface">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"

const props = defineProps({
  shape: { type: String, default: "warp" },
  type: { type: String, default: "random" },
  size: { type: Number, default: 0.5 },
  colorBack: { type: String, default: "#ef671c" },
  colorFront: { type: String, default: "#ffc691" },
  scale: { type: Number, default: 1 },
  rotation: { type: Number, default: 0 },
  offsetX: { type: Number, default: 0 },
  offsetY: { type: Number, default: 0 },
  speed: { type: Number, default: 0.4 },
  paused: { type: Boolean, default: false }
})

const SHAPES = { simplex: 1, warp: 2, dots: 3, wave: 4, ripple: 5, swirl: 6, sphere: 7 }
const TYPES = { random: 1, "2x2": 2, "4x4": 3, "8x8": 4 }

const hostRef = ref(null)
const canvasRef = ref(null)
const failed = ref(false)

const hostStyle = computed(() => failed.value
  ? { background: "radial-gradient(circle at 62% 30%, " + props.colorFront + " 0%, " + props.colorBack + " 72%)" }
  : {})

const VERTEX = `#version 300 es
precision mediump float;
layout(location = 0) in vec4 a_position;
void main() {
  gl_Position = a_position;
}`

const SIMPLEX = `
vec3 permute(vec3 x) { return mod(((x * 34.0) + 1.0) * x, 289.0); }
float snoise(vec2 v) {
  const vec4 C = vec4(0.211324865405187, 0.366025403784439,
    -0.577350269189626, 0.024390243902439);
  vec2 i = floor(v + dot(v, C.yy));
  vec2 x0 = v - i + dot(i, C.xx);
  vec2 i1;
  i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
  vec4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod(i, 289.0);
  vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0))
    + i.x + vec3(0.0, i1.x, 1.0));
  vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy),
      dot(x12.zw, x12.zw)), 0.0);
  m = m * m;
  m = m * m;
  vec3 x = 2.0 * fract(p * C.www) - 1.0;
  vec3 h = abs(x) - 0.5;
  vec3 ox = floor(x + 0.5);
  vec3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
  vec3 g;
  g.x = a0.x * x0.x + h.x * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}`

const HELPERS = `
#define TWO_PI 6.28318530718
#define PI 3.14159265358979323846
float hash11(float p) {
  p = fract(p * 0.3183099) + 0.1;
  p *= p + 19.19;
  return fract(p * p);
}
float hash21(vec2 p) {
  p = fract(p * vec2(0.3183099, 0.3678794)) + 0.1;
  p += dot(p, p + 19.19);
  return fract(p.x * p.y);
}`

const FRAG_HEADER = `#version 300 es
precision mediump float;

uniform float u_time;
uniform vec2 u_resolution;
uniform float u_pixelRatio;
uniform float u_originX;
uniform float u_originY;
uniform float u_worldWidth;
uniform float u_worldHeight;
uniform float u_fit;
uniform float u_scale;
uniform float u_rotation;
uniform float u_offsetX;
uniform float u_offsetY;
uniform float u_pxSize;
uniform vec4 u_colorBack;
uniform vec4 u_colorFront;
uniform float u_shape;
uniform float u_type;

out vec4 fragColor;`

const FRAG_BODY = `
float getSimplexNoise(vec2 uv, float t) {
  float noise = .5 * snoise(uv - vec2(0., .3 * t));
  noise += .5 * snoise(2. * uv + vec2(0., .32 * t));
  return noise;
}

const int bayer2x2[4] = int[4](0, 2, 3, 1);
const int bayer4x4[16] = int[16](
  0, 8, 2, 10,
  12, 4, 14, 6,
  3, 11, 1, 9,
  15, 7, 13, 5
);
const int bayer8x8[64] = int[64](
  0, 32, 8, 40, 2, 34, 10, 42,
  48, 16, 56, 24, 50, 18, 58, 26,
  12, 44, 4, 36, 14, 46, 6, 38,
  60, 28, 52, 20, 62, 30, 54, 22,
  3, 35, 11, 43, 1, 33, 9, 41,
  51, 19, 59, 27, 49, 17, 57, 25,
  15, 47, 7, 39, 13, 45, 5, 37,
  63, 31, 55, 23, 61, 29, 53, 21
);

float getBayerValue(vec2 uv, int size) {
  ivec2 pos = ivec2(fract(uv / float(size)) * float(size));
  int index = pos.y * size + pos.x;
  if (size == 2) {
    return float(bayer2x2[index]) / 4.0;
  } else if (size == 4) {
    return float(bayer4x4[index]) / 16.0;
  } else if (size == 8) {
    return float(bayer8x8[index]) / 64.0;
  }
  return 0.0;
}

void main() {
  float t = .5 * u_time;

  float pxSize = u_pxSize * u_pixelRatio;
  vec2 pxSizeUV = gl_FragCoord.xy - .5 * u_resolution;
  pxSizeUV /= pxSize;
  vec2 canvasPixelizedUV = (floor(pxSizeUV) + .5) * pxSize;
  vec2 normalizedUV = canvasPixelizedUV / u_resolution;

  vec2 ditheringNoiseUV = canvasPixelizedUV;
  vec2 shapeUV = normalizedUV;

  vec2 boxOrigin = vec2(.5 - u_originX, u_originY - .5);
  vec2 givenBoxSize = vec2(u_worldWidth, u_worldHeight);
  givenBoxSize = max(givenBoxSize, vec2(1.)) * u_pixelRatio;
  float r = u_rotation * PI / 180.;
  mat2 graphicRotation = mat2(cos(r), sin(r), -sin(r), cos(r));
  vec2 graphicOffset = vec2(-u_offsetX, u_offsetY);

  float patternBoxRatio = givenBoxSize.x / givenBoxSize.y;
  vec2 boxSize = vec2(
    (u_worldWidth == 0.) ? u_resolution.x : givenBoxSize.x,
    (u_worldHeight == 0.) ? u_resolution.y : givenBoxSize.y
  );

  if (u_shape > 3.5) {
    vec2 objectBoxSize = vec2(0.);
    objectBoxSize.x = min(boxSize.x, boxSize.y);
    if (u_fit == 1.) {
      objectBoxSize.x = min(u_resolution.x, u_resolution.y);
    } else if (u_fit == 2.) {
      objectBoxSize.x = max(u_resolution.x, u_resolution.y);
    }
    objectBoxSize.y = objectBoxSize.x;
    vec2 objectWorldScale = u_resolution.xy / objectBoxSize;

    shapeUV *= objectWorldScale;
    shapeUV += boxOrigin * (objectWorldScale - 1.);
    shapeUV += vec2(-u_offsetX, u_offsetY);
    shapeUV /= u_scale;
    shapeUV = graphicRotation * shapeUV;
  } else {
    vec2 patternBoxSize = vec2(0.);
    patternBoxSize.x = patternBoxRatio * min(boxSize.x / patternBoxRatio, boxSize.y);
    float patternWorldNoFitBoxWidth = patternBoxSize.x;
    if (u_fit == 1.) {
      patternBoxSize.x = patternBoxRatio * min(u_resolution.x / patternBoxRatio, u_resolution.y);
    } else if (u_fit == 2.) {
      patternBoxSize.x = patternBoxRatio * max(u_resolution.x / patternBoxRatio, u_resolution.y);
    }
    patternBoxSize.y = patternBoxSize.x / patternBoxRatio;
    vec2 patternWorldScale = u_resolution.xy / patternBoxSize;

    shapeUV += vec2(-u_offsetX, u_offsetY) / patternWorldScale;
    shapeUV += boxOrigin;
    shapeUV -= boxOrigin / patternWorldScale;
    shapeUV *= u_resolution.xy;
    shapeUV /= u_pixelRatio;
    if (u_fit > 0.) {
      shapeUV *= (patternWorldNoFitBoxWidth / patternBoxSize.x);
    }
    shapeUV /= u_scale;
    shapeUV = graphicRotation * shapeUV;
    shapeUV += boxOrigin / patternWorldScale;
    shapeUV -= boxOrigin;
    shapeUV += .5;
  }

  float shape = 0.;
  if (u_shape < 1.5) {
    shapeUV *= .001;
    shape = 0.5 + 0.5 * getSimplexNoise(shapeUV, t);
    shape = smoothstep(0.3, 0.9, shape);
  } else if (u_shape < 2.5) {
    shapeUV *= .003;
    for (float i = 1.0; i < 6.0; i++) {
      shapeUV.x += 0.6 / i * cos(i * 2.5 * shapeUV.y + t);
      shapeUV.y += 0.6 / i * cos(i * 1.5 * shapeUV.x + t);
    }
    shape = .15 / max(0.001, abs(sin(t - shapeUV.y - shapeUV.x)));
    shape = smoothstep(0.02, 1., shape);
  } else if (u_shape < 3.5) {
    shapeUV *= .05;
    float stripeIdx = floor(2. * shapeUV.x / TWO_PI);
    float rand = hash11(stripeIdx * 10.);
    rand = sign(rand - .5) * pow(.1 + abs(rand), .4);
    shape = sin(shapeUV.x) * cos(shapeUV.y - 5. * rand * t);
    shape = pow(abs(shape), 6.);
  } else if (u_shape < 4.5) {
    shapeUV *= 4.;
    float wave = cos(.5 * shapeUV.x - 2. * t) * sin(1.5 * shapeUV.x + t) * (.75 + .25 * cos(3. * t));
    shape = 1. - smoothstep(-1., 1., shapeUV.y + wave);
  } else if (u_shape < 5.5) {
    float dist = length(shapeUV);
    float waves = sin(pow(dist, 1.7) * 7. - 3. * t) * .5 + .5;
    shape = waves;
  } else if (u_shape < 6.5) {
    float l = length(shapeUV);
    float angle = 6. * atan(shapeUV.y, shapeUV.x) + 4. * t;
    float twist = 1.2;
    float offset = 1. / pow(max(l, 1e-6), twist) + angle / TWO_PI;
    float mid = smoothstep(0., 1., pow(l, twist));
    shape = mix(0., fract(offset), mid);
  } else {
    shapeUV *= 2.;
    float d = 1. - pow(length(shapeUV), 2.);
    vec3 pos = vec3(shapeUV, sqrt(max(0., d)));
    vec3 lightPos = normalize(vec3(cos(1.5 * t), .8, sin(1.25 * t)));
    shape = .5 + .5 * dot(lightPos, pos);
    shape *= step(0., d);
  }

  int type = int(floor(u_type));
  float dithering = 0.0;
  if (type == 1) {
    dithering = step(hash21(ditheringNoiseUV), shape);
  } else if (type == 2) {
    dithering = getBayerValue(pxSizeUV, 2);
  } else if (type == 3) {
    dithering = getBayerValue(pxSizeUV, 4);
  } else {
    dithering = getBayerValue(pxSizeUV, 8);
  }

  dithering -= .5;
  float res = step(.5, shape + dithering);

  vec3 fgColor = u_colorFront.rgb * u_colorFront.a;
  float fgOpacity = u_colorFront.a;
  vec3 bgColor = u_colorBack.rgb * u_colorBack.a;
  float bgOpacity = u_colorBack.a;

  vec3 color = fgColor * res;
  float opacity = fgOpacity * res;
  color += bgColor * (1. - opacity);
  opacity += bgOpacity * (1. - opacity);

  fragColor = vec4(color, opacity);
}`

const FRAG = FRAG_HEADER + SIMPLEX + HELPERS + FRAG_BODY

function parseColor(value) {
  if (Array.isArray(value)) {
    const channel = (index) => Math.min(1, Math.max(0, Number(value[index]) || 0))
    return [channel(0), channel(1), channel(2), value.length > 3 ? channel(3) : 1]
  }
  const text = String(value || "").trim()
  if (text.startsWith("#")) {
    let hex = text.slice(1)
    if (hex.length === 3) hex = hex.split("").map((c) => c + c).join("")
    if (hex.length === 6) hex += "ff"
    const num = parseInt(hex.slice(0, 8), 16)
    if (Number.isNaN(num)) return [0, 0, 0, 1]
    return [
      parseInt(hex.slice(0, 2), 16) / 255,
      parseInt(hex.slice(2, 4), 16) / 255,
      parseInt(hex.slice(4, 6), 16) / 255,
      parseInt(hex.slice(6, 8), 16) / 255
    ]
  }
  const match = text.match(/rgba?\(([^)]+)\)/i)
  if (match) {
    const parts = match[1].split(",").map((item) => parseFloat(item))
    return [
      (parts[0] || 0) / 255,
      (parts[1] || 0) / 255,
      (parts[2] || 0) / 255,
      parts.length > 3 ? parts[3] : 1
    ]
  }
  return [0, 0, 0, 1]
}

let gl = null
let program = null
let uniforms = {}
let rafId = 0
let observer = null
let intersection = null
let lastTime = 0
let frameTime = 0
let renderScale = 1
let resolutionChanged = true
let disposed = false
let visible = true
let attached = false

function compile(type, source) {
  const shader = gl.createShader(type)
  gl.shaderSource(shader, source)
  gl.compileShader(shader)
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    console.warn("[DitheringSurface] shader error:", gl.getShaderInfoLog(shader))
    gl.deleteShader(shader)
    return null
  }
  return shader
}

function buildProgram() {
  const vs = compile(gl.VERTEX_SHADER, VERTEX)
  const fs = compile(gl.FRAGMENT_SHADER, FRAG)
  if (!vs || !fs) return null
  const prog = gl.createProgram()
  gl.attachShader(prog, vs)
  gl.attachShader(prog, fs)
  gl.linkProgram(prog)
  gl.deleteShader(vs)
  gl.deleteShader(fs)
  if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
    console.warn("[DitheringSurface] link error:", gl.getProgramInfoLog(prog))
    gl.deleteProgram(prog)
    return null
  }
  return prog
}

function collectUniforms() {
  const names = [
    "u_time", "u_resolution", "u_pixelRatio", "u_originX", "u_originY",
    "u_worldWidth", "u_worldHeight", "u_fit", "u_scale", "u_rotation",
    "u_offsetX", "u_offsetY", "u_pxSize", "u_colorBack", "u_colorFront",
    "u_shape", "u_type"
  ]
  const map = {}
  names.forEach((name) => { map[name] = gl.getUniformLocation(program, name) })
  return map
}

function applyUniforms() {
  if (!gl || !program) return
  gl.useProgram(program)
  gl.uniform1f(uniforms.u_originX, 0.5)
  gl.uniform1f(uniforms.u_originY, 0.5)
  gl.uniform1f(uniforms.u_worldWidth, 0)
  gl.uniform1f(uniforms.u_worldHeight, 0)
  gl.uniform1f(uniforms.u_fit, 0)
  gl.uniform1f(uniforms.u_scale, props.scale)
  gl.uniform1f(uniforms.u_rotation, props.rotation)
  gl.uniform1f(uniforms.u_offsetX, props.offsetX)
  gl.uniform1f(uniforms.u_offsetY, props.offsetY)
  gl.uniform1f(uniforms.u_pxSize, props.size)
  gl.uniform1f(uniforms.u_shape, SHAPES[props.shape] || SHAPES.warp)
  gl.uniform1f(uniforms.u_type, TYPES[props.type] || TYPES.random)
  gl.uniform4fv(uniforms.u_colorBack, parseColor(props.colorBack))
  gl.uniform4fv(uniforms.u_colorFront, parseColor(props.colorFront))
  resolutionChanged = true
}

function resize() {
  const host = hostRef.value
  const canvas = canvasRef.value
  if (!host || !canvas || !gl) return
  const width = Math.max(1, Math.round(host.clientWidth))
  const height = Math.max(1, Math.round(host.clientHeight))
  const dpr = Math.min(2, window.devicePixelRatio || 1)
  const maxPixels = 520 * 340
  let pixelWidth = width * dpr
  let pixelHeight = height * dpr
  const scale = Math.min(1, Math.sqrt(maxPixels / (pixelWidth * pixelHeight)))
  const nextWidth = Math.max(1, Math.round(pixelWidth * scale))
  const nextHeight = Math.max(1, Math.round(pixelHeight * scale))
  if (canvas.width === nextWidth && canvas.height === nextHeight) return
  renderScale = nextWidth / width
  canvas.width = nextWidth
  canvas.height = nextHeight
  resolutionChanged = true
  gl.viewport(0, 0, nextWidth, nextHeight)
}

function render(now) {
  if (disposed || !gl || !program) return
  const delta = lastTime ? Math.min(64, now - lastTime) : 16
  lastTime = now
  frameTime += delta * (props.paused ? 0 : props.speed)
  gl.useProgram(program)
  gl.uniform1f(uniforms.u_time, frameTime * 0.001)
  if (resolutionChanged) {
    gl.uniform2f(uniforms.u_resolution, gl.canvas.width, gl.canvas.height)
    gl.uniform1f(uniforms.u_pixelRatio, renderScale)
    resolutionChanged = false
  }
  gl.clearColor(0, 0, 0, 0)
  gl.clear(gl.COLOR_BUFFER_BIT)
  gl.drawArrays(gl.TRIANGLES, 0, 6)
  rafId = visible && !props.paused && props.speed !== 0 ? requestAnimationFrame(render) : 0
}

function kick() {
  if (disposed || rafId || !gl || !program) return
  if (!visible || props.paused || props.speed === 0) {
    render(performance.now())
    return
  }
  lastTime = 0
  rafId = requestAnimationFrame(render)
}

function stop() {
  if (rafId) {
    cancelAnimationFrame(rafId)
    rafId = 0
  }
}

function onVisibility() {
  if (document.hidden) {
    stop()
  } else if (visible) {
    kick()
  }
}

function setup() {
  const canvas = canvasRef.value
  const host = hostRef.value
  if (!canvas || !host) return
  gl = canvas.getContext("webgl2", { alpha: true, premultipliedAlpha: false, antialias: false, depth: false, stencil: false })
  if (!gl) {
    failed.value = true
    release()
    return
  }
  program = buildProgram()
  if (!program) {
    failed.value = true
    const lose = gl.getExtension("WEBGL_lose_context")
    if (lose) lose.loseContext()
    gl = null
    release()
    return
  }
  uniforms = collectUniforms()
  const buffer = gl.createBuffer()
  gl.bindBuffer(gl.ARRAY_BUFFER, buffer)
  gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]), gl.STATIC_DRAW)
  const location = gl.getAttribLocation(program, "a_position")
  gl.enableVertexAttribArray(location)
  gl.vertexAttribPointer(location, 2, gl.FLOAT, false, 0, 0)
  applyUniforms()
  resize()
  attached = true
  observer = new ResizeObserver(() => { resize(); kick() })
  observer.observe(host)
  intersection = new IntersectionObserver((entries) => {
    visible = entries.some((entry) => entry.isIntersecting)
    if (visible) kick()
    else stop()
  }, { rootMargin: "120px" })
  intersection.observe(host)
  document.addEventListener("visibilitychange", onVisibility)
  kick()
}

function teardown() {
  disposed = true
  stop()
  if (observer) { observer.disconnect(); observer = null }
  if (intersection) { intersection.disconnect(); intersection = null }
  document.removeEventListener("visibilitychange", onVisibility)
  if (gl && program) {
    gl.deleteProgram(program)
    program = null
  }
  if (gl) {
    const lose = gl.getExtension("WEBGL_lose_context")
    if (lose) lose.loseContext()
    gl = null
  }
}

onMounted(() => {
  if (!attach()) return
  setup()
})

onBeforeUnmount(() => {
  teardown()
  release()
})

watch(() => [props.shape, props.type, props.size, props.colorBack, props.colorFront, props.scale, props.rotation, props.offsetX, props.offsetY], () => {
  if (!program) return
  applyUniforms()
  kick()
})

watch(() => props.paused, (value) => {
  if (value) stop()
  else kick()
})

watch(() => props.speed, (value) => {
  if (value === 0) {
    stop()
    render(performance.now())
  } else {
    kick()
  }
})

// ---- context budget: browsers cap live WebGL contexts, share a small pool ----
const MAX_CONTEXTS = 12
function attach() {
  if (attached) return true
  if (liveContexts >= MAX_CONTEXTS) {
    failed.value = true
    return false
  }
  liveContexts += 1
  attached = true
  return true
}
function release() {
  if (!attached) return
  attached = false
  liveContexts = Math.max(0, liveContexts - 1)
}
</script>

<script>
let liveContexts = 0
</script>

<style scoped>
.dithering-surface {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border-radius: inherit;
  pointer-events: none;
}

.dithering-surface__canvas {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
