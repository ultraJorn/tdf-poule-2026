<template>
  <div class="tp-paper">
    <template v-if="points.length >= 2">
      <div class="tp-paper-stats">
        <div v-for="c in statCards" :key="c.k" class="tp-paper-stat">
          <div class="k">{{ c.k }}</div>
          <div class="v">{{ c.v }}<small>{{ c.u }}</small></div>
        </div>
      </div>

      <div class="tp-paper-chart-card">
        <svg ref="chartRef" viewBox="0 0 1000 380" preserveAspectRatio="none" role="img" aria-label="Stage elevation profile"></svg>
        <div ref="readRef" class="tp-paper-read"></div>
      </div>

      <div class="tp-paper-climbs">
        <h4>Categorised climbs</h4>
        <div v-if="climbs.length" class="tp-paper-climb-list">
          <div v-for="(c, i) in climbs" :key="i" class="tp-paper-climb-row">
            <span class="tp-paper-chip" :style="{ background: catColor(c.cat) }">{{ c.cat }}</span>
            <span>Climb to {{ Math.round(c.topEle) }} m</span>
            <span class="meta">{{ c.len.toFixed(1) }} km &middot; {{ c.grad.toFixed(1) }}%</span>
            <span class="num">@ {{ c.endKm.toFixed(1) }} km</span>
          </div>
        </div>
        <div v-else class="tp-paper-empty">No categorised climbs detected on this stage.</div>
      </div>
    </template>
    <p v-else class="tp-paper-empty">No route data available for this stage yet.</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from "vue";
import { metrics as computeMetrics, detectClimbs, CAT_COLOR } from "../../utils/climbDetection";

const props = defineProps({ stage: { type: Object, required: true } });

const W = 1000, H = 380, PAD = { l: 46, r: 14, t: 18, b: 30 };
const chartRef = ref(null);
const readRef = ref(null);
let scale = null;

const points = computed(() =>
  (props.stage.elevationProfile || []).map((p) => ({ x: p.km, y: p.elev }))
);

const climbs = computed(() => (points.value.length >= 2 ? detectClimbs(points.value) : []));

const statCards = computed(() => {
  if (points.value.length < 2) return [];
  const m = computeMetrics(points.value);
  return [
    { k: "Distance", v: (props.stage.km ?? m.distance).toFixed(1), u: "km" },
    { k: "Elevation gain", v: Math.round(props.stage.elev ?? m.ascent).toLocaleString(), u: "m" },
    { k: "Highest point", v: Math.round(m.max).toLocaleString(), u: "m" },
    { k: "Lowest point", v: Math.round(m.min).toLocaleString(), u: "m" }
  ];
});

function catColor(cat) {
  // CAT_COLOR holds CSS var() references that only resolve inside .tp-paper's own scope --
  // fine here since this chip is rendered inside the same .tp-paper subtree.
  return CAT_COLOR[cat];
}

function make(tag, attrs) {
  const el = document.createElementNS("http://www.w3.org/2000/svg", tag);
  for (const k in attrs) el.setAttribute(k, attrs[k]);
  return el;
}

function niceStep(raw) {
  const pow = Math.pow(10, Math.floor(Math.log10(raw)));
  const n = raw / pow;
  const step = n < 1.5 ? 1 : n < 3 ? 2 : n < 7 ? 5 : 10;
  return step * pow;
}

function niceTicks(min, max, count) {
  const step = niceStep((max - min) / count);
  const start = Math.ceil(min / step) * step;
  const out = [];
  for (let v = start; v <= max; v += step) out.push(v);
  return out;
}

function eleAt(km) {
  const pts = scale.pts;
  if (km <= pts[0].x) return pts[0].y;
  if (km >= pts[pts.length - 1].x) return pts[pts.length - 1].y;
  let lo = 0, hi = pts.length - 1;
  while (hi - lo > 1) {
    const mid = (lo + hi) >> 1;
    if (pts[mid].x < km) lo = mid; else hi = mid;
  }
  const a = pts[lo], b = pts[hi];
  const t = (km - a.x) / (b.x - a.x);
  return a.y + (b.y - a.y) * t;
}

function onMove(evt) {
  if (!scale) return;
  const r = chartRef.value.getBoundingClientRect();
  const clientX = evt.touches ? evt.touches[0].clientX : evt.clientX;
  const px = ((clientX - r.left) / r.width) * W;
  let km = ((px - PAD.l) / (W - PAD.l - PAD.r)) * scale.maxX;
  km = Math.max(0, Math.min(scale.maxX, km));
  const ele = eleAt(km);
  scale.hair.setAttribute("opacity", 1);
  scale.hairLine.setAttribute("x1", scale.sx(km));
  scale.hairLine.setAttribute("x2", scale.sx(km));
  scale.hairDot.setAttribute("cx", scale.sx(km));
  scale.hairDot.setAttribute("cy", scale.sy(ele));
  readRef.value.classList.add("on");
  readRef.value.innerHTML = `<b>${km.toFixed(1)}</b> km &nbsp; <b>${Math.round(ele)}</b> m`;
}

function onLeave() {
  readRef.value?.classList.remove("on");
  scale?.hair.setAttribute("opacity", 0);
}

function renderChart() {
  if (!chartRef.value || points.value.length < 2) return;
  const pts = points.value;
  const m = computeMetrics(pts);
  const maxX = pts[pts.length - 1].x;
  const yPad = Math.max(40, (m.max - m.min) * 0.15);
  const yMin = Math.max(0, m.min - yPad);
  const yMax = m.max + yPad;

  const sx = (x) => PAD.l + (x / maxX) * (W - PAD.l - PAD.r);
  const sy = (y) => H - PAD.b - ((y - yMin) / (yMax - yMin)) * (H - PAD.t - PAD.b);

  const svg = chartRef.value;
  svg.innerHTML = "";

  for (const ty of niceTicks(yMin, yMax, 4)) {
    svg.appendChild(make("line", { x1: PAD.l, y1: sy(ty), x2: W - PAD.r, y2: sy(ty), stroke: "var(--grid)", "stroke-width": 1 }));
    const lbl = make("text", { x: PAD.l - 6, y: sy(ty) + 4, "text-anchor": "end", "font-size": 12, fill: "var(--ink-soft)", "font-family": "Spline Sans Mono, monospace" });
    lbl.textContent = Math.round(ty);
    svg.appendChild(lbl);
  }
  const xStep = niceStep(maxX / 6);
  for (let x = 0; x <= maxX + 0.001; x += xStep) {
    svg.appendChild(make("line", { x1: sx(x), y1: H - PAD.b, x2: sx(x), y2: H - PAD.b + 4, stroke: "var(--ink-soft)", "stroke-width": 1 }));
    const lbl = make("text", { x: sx(x), y: H - PAD.b + 18, "text-anchor": "middle", "font-size": 12, fill: "var(--ink-soft)", "font-family": "Spline Sans Mono, monospace" });
    lbl.textContent = Math.round(x);
    svg.appendChild(lbl);
  }

  const gradId = `tp-paper-fill-${props.stage.n}`;
  const defs = make("defs", {});
  const grad = make("linearGradient", { id: gradId, x1: 0, y1: 0, x2: 0, y2: 1 });
  grad.appendChild(make("stop", { offset: "0%", "stop-color": "var(--jersey)", "stop-opacity": 0.95 }));
  grad.appendChild(make("stop", { offset: "100%", "stop-color": "var(--jersey)", "stop-opacity": 0.35 }));
  defs.appendChild(grad);
  svg.appendChild(defs);

  let d = `M ${sx(pts[0].x)} ${H - PAD.b}`;
  for (const p of pts) d += ` L ${sx(p.x).toFixed(2)} ${sy(p.y).toFixed(2)}`;
  d += ` L ${sx(pts[pts.length - 1].x)} ${H - PAD.b} Z`;
  svg.appendChild(make("path", { d, fill: `url(#${gradId})` }));

  let dl = `M ${sx(pts[0].x)} ${sy(pts[0].y)}`;
  for (const p of pts) dl += ` L ${sx(p.x).toFixed(2)} ${sy(p.y).toFixed(2)}`;
  svg.appendChild(make("path", { d: dl, fill: "none", stroke: "var(--road)", "stroke-width": 2.2, "stroke-linejoin": "round" }));

  for (const c of climbs.value) {
    const color = CAT_COLOR[c.cat];
    svg.appendChild(make("line", { x1: sx(c.endKm), y1: sy(c.topEle), x2: sx(c.endKm), y2: sy(c.topEle) - 22, stroke: color, "stroke-width": 2 }));
    const tag = make("text", { x: sx(c.endKm), y: sy(c.topEle) - 26, "text-anchor": "middle", "font-size": 11, "font-weight": 700, fill: color, "font-family": "Archivo, sans-serif" });
    tag.textContent = c.cat;
    svg.appendChild(tag);
  }

  const hair = make("g", { opacity: 0 });
  const hairLine = make("line", { x1: 0, y1: PAD.t, x2: 0, y2: H - PAD.b, stroke: "var(--ink)", "stroke-width": 1, "stroke-dasharray": "3 3" });
  const hairDot = make("circle", { r: 4, fill: "var(--ink)", stroke: "var(--paper)", "stroke-width": 2 });
  hair.appendChild(hairLine);
  hair.appendChild(hairDot);
  svg.appendChild(hair);

  scale = { sx, sy, maxX, pts, hair, hairLine, hairDot };
}

onMounted(() => {
  renderChart();
  chartRef.value?.addEventListener("mousemove", onMove);
  chartRef.value?.addEventListener("touchmove", onMove, { passive: true });
  chartRef.value?.addEventListener("mouseleave", onLeave);
  chartRef.value?.addEventListener("touchend", onLeave);
});

watch(() => props.stage, () => nextTick(renderChart));
</script>
