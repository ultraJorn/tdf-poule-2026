<template>
  <div v-if="error" class="tp-error">{{ error }}</div>
  <div v-if="!store.team"><p class="tp-note">No team found.</p></div>
  <template v-else>

    <!-- ── FREE SWAP BANNER ─────────────────────────────────── -->
    <div v-if="freeSwapActive" class="tp-card" style="border-color:var(--green); background:rgba(79,185,122,0.08);">
      <strong style="color:var(--green);">{{ t("free_swap_active_title") }}</strong>
      <p class="tp-note" style="margin-top:4px;">{{ t("free_swap_active_note", { until: freeSwapUntilLabel }) }}</p>
      <p v-if="countdownLabel" class="display" style="margin:8px 0 0; font-size:22px; color:var(--green);">{{ t("race_starts_in", { countdown: countdownLabel }) }}</p>
      <p v-else-if="freeSwapUntilLabel" class="tp-note" style="margin-top:8px;">{{ t("race_should_be_underway") }}</p>
    </div>

    <!-- ── STATS BAR ────────────────────────────────────────── -->
    <div class="tp-card">
      <div style="display:flex; justify-content:space-between; flex-wrap:wrap; gap:10px;">
        <div><div class="tp-note">{{ t("total_points") }}</div><div class="display" style="font-size:30px;">{{ store.team.total.total }}</div></div>
        <div><div class="tp-note">{{ t("budget_in_use") }}</div><div class="display" style="font-size:30px;">{{ usedBudget }}<span style="font-size:16px;color:var(--text-muted);">/{{ store.poule.budgetCap }}</span></div></div>
        <div>
          <div class="tp-note">{{ t("swaps_left") }}</div>
          <div class="display" style="font-size:30px;">
            {{ swapsLeft }}<span style="font-size:16px;color:var(--text-muted);">/{{ store.poule.totalSwaps }}</span>
            <span v-if="freeSwapActive" class="tp-pill green" style="font-size:10px; vertical-align:middle; margin-left:6px;">{{ t("free_right_now") }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         SWAP PICKER — shown instead of roster while swapping
    ══════════════════════════════════════════════════════ -->
    <template v-if="swapOutId">

      <!-- outgoing rider banner -->
      <div class="tp-card" style="border-color:var(--yellow); background:rgba(245,211,0,0.06);">
        <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:10px;">
          <div>
            <div class="tp-note" style="margin-bottom:3px;">{{ t("swap_out_title", { name: "" }) }}</div>
            <div class="tp-rider-name" style="font-size:17px;">{{ natToFlag(store.ridersById[swapOutId]?.nat) }} {{ store.ridersById[swapOutId]?.name }}</div>
            <div class="tp-rider-meta">
              {{ store.ridersById[swapOutId]?.team }} &middot;
              {{ tagLabel(store.ridersById[swapOutId]?.tag) }} &middot;
              {{ store.ridersById[swapOutId]?.price }}
            </div>
          </div>
          <button class="tp-btn secondary small" @click="cancelSwap">{{ t("btn_cancel") }}</button>
        </div>
      </div>

      <!-- budget room bar -->
      <div class="tp-budget-bar">
        <div style="display:flex; justify-content:space-between; font-size:13px;">
          <span v-html="t('swap_room_note', { room: `<strong>${room}</strong>` })"></span>
        </div>
        <div class="tp-budget-meter">
          <div class="tp-budget-fill" :style="{ width: Math.min(100, ((store.poule.budgetCap - room) / store.poule.budgetCap) * 100) + '%' }"></div>
        </div>
      </div>

      <!-- search + tag filters -->
      <div class="tp-filters">
        <input class="tp-input" style="max-width:220px;" :placeholder="t('search_ph')" v-model="swapFilter">
        <button v-for="tg in tags" :key="tg" class="tp-chip" :class="{ active: swapTag === tg }" @click="swapTag = tg">
          {{ tagLabel(tg) }}
        </button>
      </div>

      <!-- rider grid -->
      <div class="tp-rider-grid">
        <div v-for="r in swapCandidatesFiltered" :key="r.id"
             class="tp-rider-card"
             :class="{ selected: swapInId === r.id, disabled: r.price > room }"
             @click="r.price <= room && toggleSwapIn(r.id)">
          <div style="flex:1; min-width:0;">
            <div class="tp-rider-name">{{ natToFlag(r.nat) }} {{ r.name }}</div>
            <div class="tp-rider-meta">{{ r.team }} &middot; {{ tagLabel(r.tag) }}</div>
          </div>
          <div class="tp-rider-price">{{ r.price }}</div>
        </div>
        <p v-if="!swapCandidatesFiltered.length" class="tp-note">{{ t("no_riders_match") }}</p>
      </div>

      <!-- sticky confirm -->
      <div v-if="swapInId" style="position:sticky; bottom:0; background:var(--bg); padding:12px 0 4px;">
        <button class="tp-btn" style="width:100%;" @click="confirmSwap">
          {{ t("btn_confirm_swap") }}: {{ store.ridersById[swapInId]?.name }}
        </button>
      </div>

    </template>

    <!-- ══════════════════════════════════════════════════════
         NORMAL VIEW: current roster + stage breakdown
    ══════════════════════════════════════════════════════ -->
    <template v-else>

      <div class="tp-card">
        <h3 style="margin-top:0;">{{ t("current_roster") }}</h3>
        <div class="tp-rider-grid">
          <div v-for="id in roster" :key="id" class="tp-rider-card" style="cursor:default;">
            <div style="flex:1; min-width:0;">
              <div class="tp-rider-name">
                {{ natToFlag(store.ridersById[id]?.nat) }} {{ store.ridersById[id]?.name }}
                <span v-if="store.ridersById[id]?.active === false" class="tp-pill muted">DNF</span>
              </div>
              <div class="tp-rider-meta">{{ store.ridersById[id]?.team }} &middot; {{ tagLabel(store.ridersById[id]?.tag) }}</div>
            </div>
            <div class="tp-rider-price">{{ store.ridersById[id]?.price }}</div>
            <button class="tp-btn secondary small" style="margin-left:8px;"
                    :disabled="swapsLeft <= 0 && !freeSwapActive"
                    @click="startSwap(id)">
              {{ t("btn_swap") }}
            </button>
          </div>
        </div>
        <p class="tp-note">{{ t("swap_note") }}</p>
      </div>

      <div class="tp-card">
        <h3 style="margin-top:0;">{{ t("stage_by_stage") }}</h3>
        <div v-if="store.team.total.breakdown.length" class="tp-scroll">
          <table class="tp-table">
            <tr><th>{{ t("th_hash") }}</th><th>{{ t("th_stage") }}</th><th>{{ t("th_points") }}</th></tr>
            <tr v-for="b in store.team.total.breakdown" :key="b.stage">
              <td>{{ t("stage_word") }} {{ b.stage }}</td>
              <td>{{ store.poule.stages[b.stage - 1].label }}</td>
              <td class="mono">{{ b.points }}</td>
            </tr>
          </table>
        </div>
        <p v-else class="tp-note">{{ t("no_stages_scored") }}</p>
      </div>

    </template>
  </template>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";

const store = usePouleStore();
const { t, tagLabel, natToFlag } = useI18n();

const swapOutId   = ref(null);
const swapInId    = ref("");
const swapFilter  = ref("");
const swapTag     = ref("All");
const error       = ref(null);

const tags = ["All", "GC", "Sprint", "Classics", "Climber", "Domestique"];

// ── roster helpers ──────────────────────────────────────────
const roster = computed(() => rosterAtStage(store.poule.currentStage + 1));
const usedBudget = computed(() => roster.value.reduce((s, id) => s + (store.ridersById[id]?.price || 0), 0));
const swapsLeft  = computed(() => store.poule.totalSwaps - (store.team.swapsUsed || 0));

function rosterAtStage(stageNum) {
  let r = store.team.riderIds.slice();
  const swaps = (store.team.swapLog || []).slice().sort((a, b) => a.effectiveFromStage - b.effectiveFromStage);
  for (const sw of swaps) {
    if (sw.effectiveFromStage > stageNum) continue;
    const idx = r.indexOf(sw.outId);
    if (idx !== -1) r[idx] = sw.inId;
    else if (!r.includes(sw.inId)) r.push(sw.inId);
  }
  return r;
}

// ── free-swap banner + countdown ───────────────────────────
const freeSwapActive = computed(() => store.poule.currentStage === 0);
const freeSwapUntilLabel = computed(() => {
  const iso = store.schedule?.stage1Start;
  if (!iso) return "";
  return new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
});

const nowMs = ref(Date.now());
let tickHandle = null;
onMounted(() => { tickHandle = setInterval(() => { nowMs.value = Date.now(); }, 1000); });
onUnmounted(() => { if (tickHandle) clearInterval(tickHandle); });

const countdownLabel = computed(() => {
  const iso = store.schedule?.stage1Start;
  if (!iso) return null;
  const diffMs = new Date(iso).getTime() - nowMs.value;
  if (diffMs <= 0) return null;
  const totalSeconds = Math.floor(diffMs / 1000);
  const days    = Math.floor(totalSeconds / 86400);
  const hours   = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  if (days > 0)  return `${days}d ${hours}h ${minutes}m`;
  if (hours > 0) return `${hours}h ${minutes}m ${seconds}s`;
  return `${minutes}m ${seconds}s`;
});

// ── swap picker ─────────────────────────────────────────────
// Budget room: how much we can spend on the incoming rider
const room = computed(() => {
  const usedExcl = roster.value
    .filter((id) => id !== swapOutId.value)
    .reduce((s, id) => s + (store.ridersById[id]?.price || 0), 0);
  return store.poule.budgetCap - usedExcl;
});

// All active riders not currently on the roster, sorted by price desc
const swapCandidatesAll = computed(() =>
  store.riders
    .filter((r) => r.active !== false)
    .filter((r) => !roster.value.includes(r.id))
    .sort((a, b) => b.price - a.price || a.name.localeCompare(b.name))
);

// Filtered by current search text + tag chip
const swapCandidatesFiltered = computed(() => {
  const ft = swapFilter.value.toLowerCase();
  return swapCandidatesAll.value
    .filter((r) => swapTag.value === "All" || r.tag === swapTag.value)
    .filter((r) => !ft || r.name.toLowerCase().includes(ft) || r.team.toLowerCase().includes(ft));
});

function startSwap(id) {
  swapOutId.value  = id;
  swapInId.value   = "";
  swapFilter.value = "";
  swapTag.value    = "All";
}

function toggleSwapIn(id) {
  swapInId.value = swapInId.value === id ? "" : id;
}

function cancelSwap() {
  swapOutId.value  = null;
  swapInId.value   = "";
  swapFilter.value = "";
  swapTag.value    = "All";
}

async function confirmSwap() {
  if (!swapInId.value) return;
  error.value = null;
  try {
    await store.swap(swapOutId.value, swapInId.value);
    cancelSwap();
  } catch (e) {
    error.value = e.message;
  }
}
</script>
