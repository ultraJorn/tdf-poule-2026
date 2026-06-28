// Ported from a standalone reference design: lightweight client-side climb detection over an
// already-downsampled {x: km, y: elev} point series. Boundaries/gradients are approximate
// (the profile is 180 points, not the full raw GPX), good enough to flag and categorize the
// stage's notable climbs for display, not survey-grade.

export function metrics(points) {
  let asc = 0, desc = 0, max = -Infinity, min = Infinity;
  for (let i = 0; i < points.length; i++) {
    const y = points[i].y;
    if (y > max) max = y;
    if (y < min) min = y;
    if (i > 0) {
      const d = y - points[i - 1].y;
      if (d > 0) asc += d; else desc += -d;
    }
  }
  return { distance: points[points.length - 1].x, ascent: asc, descent: desc, max, min };
}

function categorise(gain, grad) {
  const score = gain * grad; // crude "difficulty" proxy
  if (gain >= 1000 && grad >= 6) return "HC";
  if (score >= 4500) return "1";
  if (score >= 2200) return "2";
  if (score >= 1000) return "3";
  return "4";
}

/** Sustained rises of >=80m gained, tolerating tiny dips so a climb with a brief flat/dip
 *  section in the middle still counts as one climb rather than splitting into several. */
export function detectClimbs(points) {
  const climbs = [];
  let i = 0;
  while (i < points.length - 1) {
    if (points[i + 1].y > points[i].y) {
      const startI = i;
      let j = i;
      while (j < points.length - 1 && points[j + 1].y >= points[j].y - 3) j++;
      const a = points[startI], b = points[j];
      const gain = b.y - a.y, len = b.x - a.x;
      if (gain >= 80 && len > 0.4) {
        const grad = (gain / (len * 1000)) * 100;
        climbs.push({ startKm: a.x, endKm: b.x, gain, len, grad, topEle: b.y, cat: categorise(gain, grad) });
      }
      i = j;
    } else {
      i++;
    }
  }
  return climbs;
}

export const CAT_COLOR = { HC: "var(--hc)", 1: "var(--cat1)", 2: "var(--cat2)", 3: "var(--cat3)", 4: "var(--cat4)" };
