<template>
  <div class="tp-hero" style="position:relative;">
    <LangToggle overlay />
    <div class="tp-eyebrow">{{ store.poule.name }} &middot; {{ store.code }}</div>
    <h1 class="tp-title display" style="font-size:22px;">{{ t("pick_team_title", { name: store.username }) }}</h1>
    <p class="tp-sub">
      {{ t("pick_team_subtitle", { size: store.poule.teamSize, cap: store.poule.budgetCap }) }}
      <template v-if="store.poule.currentStage > 0">{{ t("pick_team_late", { n: store.poule.currentStage }) }}</template>
    </p>
  </div>
  <div class="tp-body">
    <div v-if="error" class="tp-error">{{ error }}</div>

    <div class="tp-budget-bar">
      <div style="display:flex;justify-content:space-between;font-size:13px;">
        <span><strong>{{ selected.length }} / {{ store.poule.teamSize }}</strong> {{ t("riders_word") }}</span>
        <span class="mono"><strong>{{ used }}</strong> / {{ store.poule.budgetCap }} {{ t("budget_word") }}</span>
      </div>
      <div class="tp-budget-meter"><div class="tp-budget-fill" :class="{ over }" :style="{ width: Math.min(100, (used / store.poule.budgetCap) * 100) + '%' }"></div></div>
    </div>

    <div class="tp-filters">
      <input class="tp-input" style="max-width:220px;" :placeholder="t('search_ph')" v-model="filterText">
      <button v-for="tg in tags" :key="tg" class="tp-chip" :class="{ active: filterTag === tg }" @click="filterTag = tg">
        {{ tagLabel(tg) }}
      </button>
    </div>

    <div class="tp-rider-grid">
      <div v-for="r in filteredRiders" :key="r.id"
           class="tp-rider-card"
           :class="{ selected: selected.includes(r.id), disabled: isDisabled(r) }"
           @click="toggle(r)">
        <input type="checkbox" class="tp-checkbox" :checked="selected.includes(r.id)" :disabled="isDisabled(r)" tabindex="-1">
        <div>
          <div class="tp-rider-name">{{ r.name }}</div>
          <div class="tp-rider-meta">{{ r.team }} &middot; {{ tagLabel(r.tag) }}</div>
        </div>
        <div class="tp-rider-price">{{ r.price }}</div>
      </div>
      <p v-if="!filteredRiders.length" class="tp-note">{{ t("no_riders_match") }}</p>
    </div>

    <div style="margin-top:20px; position:sticky; bottom:0; background:var(--bg); padding-top:10px;">
      <button class="tp-btn" style="width:100%;" :disabled="selected.length !== store.poule.teamSize || over || busy" @click="confirm">
        {{ busy ? t("btn_saving") : t("btn_confirm_team", { n: selected.length, size: store.poule.teamSize }) }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { usePouleStore } from "../stores/poule";
import { useI18n } from "../i18n";
import LangToggle from "../components/LangToggle.vue";

const store = usePouleStore();
const router = useRouter();
const { t, tagLabel } = useI18n();

onMounted(() => {
  if (store.team) router.replace(`/poule/${store.code}`);
});

const tags = ["All", "GC", "Sprint", "Classics", "Climber", "Domestique"];
const selected = ref([]);
const filterText = ref("");
const filterTag = ref("All");
const busy = ref(false);
const error = ref(null);

const used = computed(() => selected.value.reduce((s, id) => s + (store.ridersById[id]?.price || 0), 0));
const over = computed(() => used.value > store.poule.budgetCap);

const filteredRiders = computed(() => {
  const ft = filterText.value.toLowerCase();
  return store.riders
    .filter((r) => r.active !== false)
    .filter((r) => filterTag.value === "All" || r.tag === filterTag.value)
    .filter((r) => !ft || r.name.toLowerCase().includes(ft) || r.team.toLowerCase().includes(ft))
    .sort((a, b) => b.price - a.price || a.name.localeCompare(b.name));
});

function isDisabled(r) {
  if (selected.value.includes(r.id)) return false;
  const wouldExceed = used.value + r.price > store.poule.budgetCap;
  const full = selected.value.length >= store.poule.teamSize;
  return wouldExceed || full;
}

function toggle(r) {
  const idx = selected.value.indexOf(r.id);
  if (idx !== -1) { selected.value.splice(idx, 1); return; }
  if (isDisabled(r)) return;
  selected.value.push(r.id);
}

async function confirm() {
  if (selected.value.length !== store.poule.teamSize) return;
  error.value = null;
  busy.value = true;
  try {
    await store.confirmTeam(selected.value);
    router.push(`/poule/${store.code}`);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
