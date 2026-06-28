// One-time helper: pulls the route map image out of the original single-file app's
// inline MAP_DATA_URI and writes it to frontend/public/route-map.jpg as a real static
// asset, instead of re-embedding tens of KB of base64 text in source files.
//
// Usage (from frontend/):  node scripts/extract-route-map.cjs
const fs = require("fs");
const path = require("path");

const htmlPath = path.resolve(__dirname, "../../tour-de-france-poule-2026.html");
const outPath = path.resolve(__dirname, "../public/route-map.jpg");

const html = fs.readFileSync(htmlPath, "utf8");
const match = html.match(/MAP_DATA_URI\s*=\s*"data:image\/jpeg;base64,([^"]+)"/);
if (!match) {
  console.error("Could not find MAP_DATA_URI in " + htmlPath);
  process.exit(1);
}

fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, Buffer.from(match[1], "base64"));
console.log("Wrote " + outPath);
