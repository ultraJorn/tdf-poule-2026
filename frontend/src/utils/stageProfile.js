// Schematic road profile, shaped by each stage's *actual* km/elev (not surveyed elevation
// data, but driven by it) plus a per-stage seed -- two stages sharing a tag used to render
// pixel-identical because the old version only looked at stage.tag. Now the climb density
// (m climbed per km) sets the overall amplitude/steepness, and a seeded pseudo-random wave
// set means every stage looks distinct even within the same category.

// Deterministic PRNG (mulberry32) so the same stage always renders the same shape across
// reloads, without needing to store random points anywhere.
function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// The overall trend line for a tag, before per-stage noise is layered on -- this is what
// keeps "Mountain" stages reading as build-to-a-summit, "Flat" stages as flat, etc., while
// `density` (climbing per km) stretches/steepens it per stage.
function envelope(tag, x, density) {
  if (tag === "Flat") return 0.15 + density * 0.012;
  if (tag === "TTT" || tag === "ITT") {
    const ramp = x > 0.6 ? (x - 0.6) / 0.4 : 0;
    return 0.14 + density * 0.006 + ramp * (0.10 + density * 0.012);
  }
  if (tag === "Hilly") {
    return 0.20 + Math.pow(x, 0.6) * (0.10 + density * 0.013);
  }
  // Mountain: climbs toward a summit finish, with a brief valley dip before the final ramp.
  let y = 0.14 + Math.pow(x, 1.3) * (0.30 + density * 0.015);
  if (x > 0.72) {
    const f = (x - 0.72) / 0.28;
    const dip = f < 0.15 ? -0.12 * (1 - f / 0.15) : 0;
    const finalHeight = Math.min(0.95, 0.45 + density * 0.014);
    const ramp = f < 0.15 ? 0 : ((f - 0.15) / 0.85) * finalHeight;
    y = Math.max(y + dip, ramp);
  }
  return y;
}

function profileShape(stage) {
  const tag = stage.tag;
  const density = stage.km ? (stage.elev || 0) / stage.km : 0; // metres climbed per km
  const rng = mulberry32((stage.n || 1) * 97 + Math.round(density * 13) + 1);

  const numWaves = tag === "Flat" ? 2 : tag === "Hilly" ? 3 : tag === "Mountain" ? 4 : 2;
  const waves = Array.from({ length: numWaves }, () => ({
    freq: 3 + rng() * 12,
    phase: rng() * Math.PI * 2,
    amp: (0.015 + rng() * 0.045) * (tag === "Flat" ? 0.7 : 1)
  }));

  const n = tag === "Mountain" ? 60 : tag === "Hilly" ? 50 : 42;
  const pts = [];
  for (let i = 0; i <= n; i++) {
    const x = i / n;
    let y = envelope(tag, x, density);
    for (const w of waves) y += w.amp * Math.sin(x * w.freq + w.phase);
    pts.push({ x, y: Math.min(0.97, Math.max(0.08, y)) });
  }
  return pts;
}

// Real GPX-derived elevation profiles aren't normalized per-stage -- a flat stage and a high
// mountain stage are scaled against the SAME reference relief, so flat stages genuinely render
// flat and mountains render dramatic, rather than every stage being stretched to fill the same
// chart height regardless of how much actual climbing it has. REFERENCE_RELIEF_M is set just
// above the biggest elevation range across the 2026 route's real GPX files (stage 20, ~2092m).
const REFERENCE_RELIEF_M = 2200;
const MIN_DISPLAY_FRACTION = 0.08; // even pancake-flat stages still show a thin visible line
const CHART_LOW = 0.06, CHART_HIGH = 0.94;

function realProfilePoints(stage) {
  const profile = stage.elevationProfile;
  const totalKm = profile[profile.length - 1].km || 1;
  const elevs = profile.map((p) => p.elev);
  const elevMin = Math.min(...elevs), elevMax = Math.max(...elevs);
  const actualRange = Math.max(1, elevMax - elevMin);
  const displayFraction = Math.max(actualRange / REFERENCE_RELIEF_M, MIN_DISPLAY_FRACTION);
  const displayRange = displayFraction * (CHART_HIGH - CHART_LOW);
  return profile.map((p) => ({
    x: p.km / totalKm,
    y: CHART_LOW + ((p.elev - elevMin) / actualRange) * displayRange
  }));
}

export function hasRealProfile(stage) {
  return Array.isArray(stage.elevationProfile) && stage.elevationProfile.length >= 2;
}

export function profileSvgMarkup(stage) {
  const W = 600, H = 150, padL = 8, padR = 8, padB = 22, padT = 10;
  const pts = hasRealProfile(stage) ? realProfilePoints(stage) : profileShape(stage);
  const innerW = W - padL - padR, innerH = H - padT - padB;
  const X = (x) => padL + x * innerW;
  const Y = (y) => padT + (1 - y) * innerH;
  const line = pts.map((p, i) => `${i === 0 ? "M" : "L"}${X(p.x).toFixed(1)},${Y(p.y).toFixed(1)}`).join(" ");
  const area = `${line} L${X(1).toFixed(1)},${Y(0).toFixed(1)} L${X(0).toFixed(1)},${Y(0).toFixed(1)} Z`;
  const fill = stage.tag === "Mountain" ? "var(--red)" : stage.tag === "Hilly" ? "#E8A33A" : stage.tag === "Flat" ? "var(--green)" : "var(--blue)";
  const km = stage.km || 0;
  const ticks = [];
  if (km) {
    for (let d = 0; d <= km; d += Math.max(25, Math.round(km / 6 / 25) * 25)) ticks.push(d);
    if (ticks[ticks.length - 1] !== km) ticks.push(km);
  }
  const tickEls = ticks.map((d) => {
    const x = X(d / km);
    return `<line x1="${x.toFixed(1)}" y1="${(H - padB).toFixed(1)}" x2="${x.toFixed(1)}" y2="${(H - padB + 4).toFixed(1)}" stroke="var(--border)" stroke-width="1"/>
      <text x="${x.toFixed(1)}" y="${(H - padB + 15).toFixed(1)}" fill="var(--text-muted)" font-size="10" font-family="monospace" text-anchor="middle">${d}</text>`;
  }).join("");
  const startY = Y(pts[0].y), finishY = Y(pts[pts.length - 1].y);
  return `<svg viewBox="0 0 ${W} ${H}" width="100%" preserveAspectRatio="xMidYMid meet" xmlns="http://www.w3.org/2000/svg" role="img">
    <defs><linearGradient id="pg-${stage.n}" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="${fill}" stop-opacity="0.45"/>
      <stop offset="100%" stop-color="${fill}" stop-opacity="0.05"/>
    </linearGradient></defs>
    <path d="${area}" fill="url(#pg-${stage.n})"/>
    <path d="${line}" fill="none" stroke="${fill}" stroke-width="2.5" stroke-linejoin="round"/>
    <circle cx="${X(0).toFixed(1)}" cy="${startY.toFixed(1)}" r="3.5" fill="var(--text)"/>
    <circle cx="${X(1).toFixed(1)}" cy="${finishY.toFixed(1)}" r="4" fill="${fill}" stroke="var(--surface)" stroke-width="1.5"/>
    ${tickEls}
  </svg>`;
}
