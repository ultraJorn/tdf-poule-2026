<template>
  <div class="tp-hero">
    <div class="tp-hero-top">
      <div>
        <div class="tp-eyebrow">
          {{ store.poule.name }} &middot; code
          <span class="mono" style="cursor:pointer;text-decoration:underline;" @click="copyCode">{{ store.code }}</span>
        </div>
        <div class="tp-km-wrap" style="margin-top:10px;">
          <div class="tp-km">{{ store.poule.currentStage }}</div>
          <div>
            <div class="tp-km-label">{{ stageStatusLabel }}</div>
            <div class="tp-km-stage">{{ currentStageLabel }}</div>
          </div>
        </div>
      </div>
      <div style="display:flex; flex-direction:column; align-items:flex-end; gap:8px;">
        <LangToggle />
        <div style="display:flex; gap:8px;">
          <button v-if="store.isAdmin" class="tp-btn secondary small" @click="quickBackup">
            {{ store.backupCopied ? t("btn_copied") : t("backup_word") }}
          </button>
          <button class="tp-btn secondary small" @click="leave">{{ t("switch_poule") }}</button>
        </div>
      </div>
    </div>
    <div class="tp-route">
      <div v-for="s in store.poule.stages" :key="s.n" class="tp-route-seg" :class="{ done: s.locked }" :title="`${t('stage_word')} ${s.n}`"></div>
    </div>
  </div>
  <div class="tp-body">
    <div v-if="error" class="tp-error">{{ error }}</div>
    <div class="tp-tabs">
      <button v-for="tb in tabs" :key="tb.key" class="tp-tab" :class="{ active: tab === tb.key }" @click="setTab(tb.key)">
        {{ tb.label }}
      </button>
    </div>
    <MyTeamTab v-if="tab === 'team'" />
    <LeaderboardTab v-else-if="tab === 'leaderboard'" />
    <StagesTab v-else-if="tab === 'stages'" />
    <ResultsTab v-else-if="tab === 'results'" />
    <AdminTab v-else-if="tab === 'admin'" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { usePouleStore } from "../stores/poule";
import { useI18n } from "../i18n";
import LangToggle from "../components/LangToggle.vue";
import MyTeamTab from "../components/dashboard/MyTeamTab.vue";
import LeaderboardTab from "../components/dashboard/LeaderboardTab.vue";
import StagesTab from "../components/dashboard/StagesTab.vue";
import ResultsTab from "../components/dashboard/ResultsTab.vue";
import AdminTab from "../components/dashboard/AdminTab.vue";

const store = usePouleStore();
const router = useRouter();
const { t } = useI18n();

const tab = ref("team");
const error = ref(null);

const tabs = computed(() => [
  { key: "team", label: t("tab_team") },
  { key: "leaderboard", label: t("tab_leaderboard") },
  { key: "stages", label: t("tab_stages") },
  { key: "results", label: t("tab_results") },
  { key: "admin", label: t("tab_admin") }
]);

const stageStatusLabel = computed(() => {
  const n = store.poule.currentStage;
  if (n === 0) return t("pre_race");
  if (n >= 21) return t("race_complete");
  return t("stages_raced");
});

const currentStageLabel = computed(() => {
  const n = store.poule.currentStage;
  const stage = n === 0 ? store.poule.stages[0] : store.poule.stages[n - 1];
  return n === 0 ? t("stage_n_colon", { n: 1 }) + stage.label : (stage ? stage.label : "");
});

async function setTab(key) {
  tab.value = key;
  if (key === "leaderboard") {
    try { await store.loadLeaderboard(); } catch (e) { error.value = e.message; }
  }
}

function copyCode() {
  navigator.clipboard?.writeText(store.code).catch(() => {});
}

async function quickBackup() {
  try {
    await store.makeBackup();
    await navigator.clipboard.writeText(store.backupCode);
    store.backupCopied = true;
    setTimeout(() => { store.backupCopied = false; }, 2500);
  } catch (e) {
    error.value = e.message;
  }
}

function leave() {
  store.leavePoule();
  router.push("/");
}

onMounted(() => {
  store.loadSchedule().catch(() => {}); // non-critical: just powers the free-swap banner + real stage dates
});
</script>
