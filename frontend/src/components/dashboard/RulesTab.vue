<template>
  <div class="tp-card">
    <h2 style="margin-top:0;">{{ t("rules_title") }}</h2>

    <h3>{{ t("rules_finish_title") }}</h3>
    <p class="tp-note" style="margin-bottom:10px;">{{ t("rules_finish_note") }}</p>
    <div class="tp-scroll">
      <table class="tp-table">
        <tr>
          <th>Place</th>
          <td v-for="p in finishPoints" :key="p.place" style="text-align:center;">{{ ordinal(p.place) }}</td>
        </tr>
        <tr>
          <th>Points</th>
          <td v-for="p in finishPoints" :key="p.place" class="mono" style="text-align:center; color:var(--yellow);">{{ p.pts }}</td>
        </tr>
      </table>
    </div>

    <h3 style="margin-top:22px;">{{ t("rules_jersey_title") }}</h3>
    <p class="tp-note" style="margin-bottom:10px;">{{ t("rules_jersey_note") }}</p>
    <table class="tp-table" style="max-width:340px;">
      <tr v-for="j in jerseyBonuses" :key="j.key">
        <td style="display:flex; align-items:center; gap:8px;">
          <span :style="{ display:'inline-block', width:'12px', height:'12px', borderRadius:'50%', background:j.color }"></span>
          {{ jerseyLabel(j.key) }}
        </td>
        <td class="mono" style="color:var(--yellow);">+{{ j.pts }}</td>
      </tr>
    </table>

    <h3 style="margin-top:22px;">{{ t("rules_gc_title") }}</h3>
    <p class="tp-note" style="margin-bottom:10px;">{{ t("rules_gc_note") }}</p>
    <div class="tp-scroll">
      <table class="tp-table">
        <tr>
          <th>GC rank</th>
          <td v-for="g in gcBonuses" :key="g.rank" style="text-align:center;">{{ ordinal(g.rank) }}</td>
        </tr>
        <tr>
          <th>Bonus</th>
          <td v-for="g in gcBonuses" :key="g.rank" class="mono" style="text-align:center; color:var(--yellow);">{{ g.pts }}</td>
        </tr>
      </table>
    </div>

    <h3 style="margin-top:22px;">{{ t("rules_swaps_title") }}</h3>
    <p class="tp-note">{{ t("rules_swaps_note") }}</p>
    <p class="tp-note" style="margin-top:6px;">{{ t("rules_budget_note") }}</p>

    <h3 style="margin-top:22px;">{{ t("rules_freeswap_title") }}</h3>
    <p class="tp-note">{{ t("rules_freeswap_note") }}</p>
  </div>
</template>

<script setup>
import { useI18n } from "../../i18n";

const { t, ordinal, jerseyLabel } = useI18n();

const finishPoints = [
  { place: 1, pts: 25 }, { place: 2, pts: 20 }, { place: 3, pts: 16 },
  { place: 4, pts: 14 }, { place: 5, pts: 12 }, { place: 6, pts: 10 },
  { place: 7, pts: 8 },  { place: 8, pts: 7 },  { place: 9, pts: 6 },
  { place: 10, pts: 5 }, { place: 11, pts: 4 }, { place: 12, pts: 3 },
  { place: 13, pts: 2 }, { place: 14, pts: 1 }, { place: 15, pts: 1 }
];

const jerseyBonuses = [
  { key: "yellow", pts: 8,  color: "#f5c800" },
  { key: "green",  pts: 5,  color: "#4fb97a" },
  { key: "polka",  pts: 5,  color: "#e05252" },
  { key: "white",  pts: 3,  color: "#e8e8e8" }
];

const gcBonuses = [
  { rank: 1, pts: 250 }, { rank: 2, pts: 200 }, { rank: 3, pts: 160 },
  { rank: 4, pts: 140 }, { rank: 5, pts: 120 }, { rank: 6, pts: 100 },
  { rank: 7, pts: 80 },  { rank: 8, pts: 70 },  { rank: 9, pts: 60 },
  { rank: 10, pts: 50 }
];
</script>
