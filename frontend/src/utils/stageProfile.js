// Ported verbatim from the original app's profileShape()/profileSVG(): a schematic road
// profile shaped by stage type + total climbing, not surveyed elevation data.

function profileShape(stage) {
  const tag = stage.tag;
  const pts = [];
  const push = (x, y) => pts.push({ x, y });
  if (tag === "Flat") {
    for (let i = 0; i <= 40; i++) { const x = i / 40; const y = 0.18 + 0.06 * Math.sin(x * 9) + 0.04 * Math.sin(x * 23); push(x, y); }
  } else if (tag === "TTT" || tag === "ITT") {
    for (let i = 0; i <= 40; i++) { const x = i / 40; let y = 0.15 + 0.05 * Math.sin(x * 7); if (x > 0.6) y += 0.25 * Math.max(0, Math.sin((x - 0.6) * Math.PI / 0.4)); push(x, y); }
  } else if (tag === "Hilly") {
    for (let i = 0; i <= 48; i++) { const x = i / 48; const base = 0.22 + 0.30 * Math.pow(x, 0.7); const roll = 0.16 * Math.abs(Math.sin(x * 7)); let y = base * 0.5 + roll + 0.10 * Math.sin(x * 15); push(x, Math.min(0.8, y)); }
  } else {
    for (let i = 0; i <= 52; i++) {
      const x = i / 52;
      let y = 0.15 + 0.62 * Math.pow(x, 1.4);
      y += 0.10 * Math.sin(x * 8);
      if (x > 0.72) {
        const f = (x - 0.72) / 0.28;
        const ramp = f < 0.18 ? (0.4 - f / 0.18 * 0.12) : (0.28 + (f - 0.18) / 0.82 * 0.68);
        y = Math.max(y * 0.6, ramp);
      }
      push(x, Math.min(0.97, Math.max(0.12, y)));
    }
  }
  return pts;
}

export function profileSvgMarkup(stage) {
  const W = 600, H = 150, padL = 8, padR = 8, padB = 22, padT = 10;
  const pts = profileShape(stage);
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
