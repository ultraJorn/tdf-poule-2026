<template>
  <div v-if="error" class="tp-error">{{ error }}</div>
  <div v-if="!store.team"><p class="tp-note">No team found.</p></div>
  <template v-else>
    <div v-if="freeSwapActive" class="tp-card" style="border-color:var(--green); background:rgba(79,185,122,0.08);">
      <strong style="color:var(--green);">{{ t("free_swap_active_title") }}</strong>
      <p class="tp-note" style="margin-top:4px;">{{ t("free_swap_active_note", { until: freeSwapUntilLabel }) }}</p>
    </div>
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

    <div v-if="swapOutId" class="tp-card" style="border-color:var(--yellow);">
      <h3 style="margin-top:0;">{{ t("swap_out_title", { name: store.ridersById[swapOutId]?.name || "" }) }}</h3>
      <p class="tp-note" v-html="t('swap_room_note', { room: `<strong>${room}</strong>` })"></p>
      <div class="tp-field">
        <select class="tp-select" v-model="swapInId">
          <option v-if="!candidates.length" value="">{{ t("no_riders_fit") }}</option>
          <option v-for="c in candidates" :key="c.id" :value="c.id">{{ c.name }} ({{ c.team }}) &mdash; {{ c.price }}</option>
        </select>
      </div>
      <div class="tp-row">
        <button class="tp-btn" :disabled="!candidates.length" @click="confirmSwap">{{ t("btn_confirm_swap") }}</button>
        <button class="tp-btn secondary" @click="swapOutId = null">{{ t("btn_cancel") }}</button>
      </div>
    </div>

    <div class="tp-card">
      <h3 style="margin-top:0;">{{ t("current_roster") }}</h3>
      <div class="tp-rider-grid">
        <div v-for="id in roster" :key="id" class="tp-rider-card" style="cursor:default;">
          <div>
            <div class="tp-rider-name">
              {{ store.ridersById[id]?.name }}
              <span v-if="store.ridersById[id]?.active === false" class="tp-pill muted">DNF</span>
            </div>
            <div class="tp-rider-meta">{{ store.ridersById[id]?.team }} &middot; {{ tagLabel(store.ridersById[id]?.tag) }}</div>
          </div>
          <div class="tp-rider-price">{{ store.ridersById[id]?.price }}</div>
          <button class="tp-btn secondary small" style="margin-left:8px;" :disabled="swapsLeft <= 0 && !freeSwapActive" @click="swapOutId = id">{{ t("btn_swap") }}</button>
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

<script setup>
import { ref, computed } from "vue";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";

const store = usePouleStore();
const { t, tagLabel } = useI18n();

const swapOutId = ref(null);
const swapInId = ref("");
const error = ref(null);

const roster = computed(() => rosterAtStage(store.poule.currentStage + 1));
const usedBudget = computed(() => roster.value.reduce((s, id) => s + (store.ridersById[id]?.price || 0), 0));
const swapsLeft = computed(() => store.poule.totalSwaps - (store.team.swapsUsed || 0));

const freeSwapActive = computed(() => !!store.schedule?.freeSwapWindowActive);
const freeSwapUntilLabel = computed(() => {
  const iso = store.schedule?.stage1Start;
  if (!iso) return "";
  return new Date(iso).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
});

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

const room = computed(() => {
  const usedExcl = roster.value.filter((id) => id !== swapOutId.value).reduce((s, id) => s + (store.ridersById[id]?.price || 0), 0);
  return store.poule.budgetCap - usedExcl;
});

const candidates = computed(() => {
  return store.riders
    .filter((r) => r.active !== false)
    .filter((r) => !roster.value.includes(r.id))
    .filter((r) => r.price <= room.value)
    .sort((a, b) => b.price - a.price || a.name.localeCompare(b.name));
});

async function confirmSwap() {
  if (!swapInId.value) return;
  error.value = null;
  try {
    await store.swap(swapOutId.value, swapInId.value);
    swapOutId.value = null;
    swapInId.value = "";
  } catch (e) {
    error.value = e.message;
  }
}
</script>
