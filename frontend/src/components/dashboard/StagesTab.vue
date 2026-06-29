<template>
  <div v-if="detailStage" >
    <button class="tp-link-btn" style="margin-bottom:12px;" @click="detailStage = null">{{ t("back_to_stages") }}</button>
    <div class="tp-card">
      <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:10px; flex-wrap:wrap;">
        <div>
          <h2 class="display" style="margin:0; font-size:20px;">{{ t("stage_word") }} {{ detailStage.n }}</h2>
          <div style="margin-top:4px;">{{ detailStage.label }} <span class="tp-pill muted">{{ stageTagLabel(detailStage.tag) }}</span></div>
          <div v-if="formatStageDate(detailStage.n)" class="tp-note mono" style="margin-top:4px;">{{ formatStageDate(detailStage.n) }}</div>
        </div>
        <span v-if="detailStage.locked" class="tp-pill green">{{ t("scored_pill") }}</span>
        <span v-else class="tp-pill muted">{{ t("pending_pill") }}</span>
      </div>
      <div style="margin-top:16px;">
        <StageProfileChart v-if="hasRealProfile(detailStage)" :stage="detailStage" />
        <template v-else>
          <div v-if="detailStage.km" style="display:flex; gap:24px; margin-bottom:10px;">
            <div><div class="tp-note">{{ t("th_distance") }}</div><div class="display" style="font-size:22px;">{{ detailStage.km }}<span style="font-size:13px; color:var(--text-muted);"> km</span></div></div>
            <div><div class="tp-note">{{ t("th_climbing") }}</div><div class="display" style="font-size:22px;">{{ detailStage.elev }}<span style="font-size:13px; color:var(--text-muted);"> m</span></div></div>
          </div>
          <div style="background:var(--surface2); border:1px solid var(--border); border-radius:10px; padding:12px;">
            <div v-html="profileSvgMarkup(detailStage)"></div>
            <div class="tp-note" style="text-align:center; margin-top:4px;">{{ t("profile_caption") }}</div>
          </div>
        </template>
      </div>
      <p v-if="note(detailStage)" style="margin-top:16px; font-size:14px; line-height:1.5;">{{ note(detailStage) }}</p>
      <div v-if="detailStage.favorites?.length" style="margin-top:14px;">
        <div class="tp-note" style="margin-bottom:6px;">{{ t("favorites_word") }}</div>
        <span v-for="f in detailStage.favorites" :key="f" class="tp-pill" style="background:var(--surface3); color:var(--text); margin:0 6px 6px 0; display:inline-block;">{{ f }}</span>
      </div>
    </div>
  </div>
  <div v-else>
    <div class="tp-card" style="padding:0; overflow:hidden;">
      <img src="/route-map.jpg" alt="Tour de France 2026 route map" style="width:100%; display:block;"
           @error="onMapMissing">
    </div>
    <p class="tp-note" style="margin:10px 0;">{{ t("stages_source_note") }}</p>
    <div v-for="s in store.poule.stages" :key="s.n" class="tp-card tp-stage-row" style="padding:13px 16px; cursor:pointer;" @click="detailStage = s">
      <div style="display:flex; justify-content:space-between; align-items:center; gap:10px; flex-wrap:wrap;">
        <div><strong>{{ t("stage_word") }} {{ s.n }}</strong> &middot; {{ s.label }} <span class="tp-pill muted">{{ stageTagLabel(s.tag) }}</span></div>
        <div style="display:flex; align-items:center; gap:8px;">
          <span v-if="s.locked" class="tp-pill green">{{ t("scored_pill") }}</span>
          <span v-else class="tp-pill muted">{{ t("pending_pill") }}</span>
          <span style="color:var(--text-muted); font-size:18px;">&rsaquo;</span>
        </div>
      </div>
      <div v-if="s.km" class="tp-note mono" style="margin-top:6px;">{{ s.km }} km &middot; {{ s.elev }} m {{ t("th_climbing").toLowerCase() }}</div>
      <div v-if="formatStageDate(s.n)" class="tp-note mono" style="margin-top:2px;">{{ formatStageDate(s.n) }}</div>
      <div style="margin-top:8px; pointer-events:none;" v-html="profileSvgMarkup(s)"></div>
      <p v-if="note(s)" class="tp-note" style="margin-top:6px;">{{ note(s) }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";
import { profileSvgMarkup, hasRealProfile } from "../../utils/stageProfile";
import StageProfileChart from "./StageProfileChart.vue";

const store = usePouleStore();
const { t, lang, stageTagLabel } = useI18n();

const detailStage = ref(null);

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

// Manual string formatting (no Date object) so there's zero risk of the browser's local
// timezone shifting a midnight-UTC date back a day -- these are fixed real-world calendar
// values from the schedule API, not something that needs re-interpreting per visitor.
function formatStageDate(stageNumber) {
  const s = store.scheduleByStage[stageNumber];
  if (!s) return "";
  const [y, m, d] = s.date.split("-").map(Number);
  return `${s.day}, ${d} ${MONTHS[m - 1]} ${y} · ${s.startTimeCest} CEST`;
}

function note(stage) {
  if (!stage.note) return "";
  return (stage.note[lang.value] || stage.note.en || "");
}

function onMapMissing(e) {
  // The static map asset is generated locally via scripts/extract-route-map.cjs; hide the
  // broken-image box gracefully if it hasn't been run yet rather than showing a placeholder icon.
  e.target.closest(".tp-card").style.display = "none";
}
</script>
