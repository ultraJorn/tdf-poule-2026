<template>
  <div class="tp-card">
    <div class="tp-field">
      <label>{{ t("label_stage") }}</label>
      <select class="tp-select" v-model.number="stageNum">
        <option v-for="s in store.poule.stages" :key="s.n" :value="s.n">
          {{ t("stage_word") }} {{ s.n }} &mdash; {{ s.label }} {{ s.locked ? "(" + t("scored_pill").toLowerCase() + ")" : "" }}
        </option>
      </select>
    </div>

    <h4 style="margin:14px 0 8px;">{{ t("finish_order_title") }}</h4>
    <div class="tp-place-grid">
      <div v-for="i in 15" :key="'p' + i" class="tp-place-cell">
        <label>{{ ordinal(i) }}</label>
        <select class="tp-select" v-model="places[i - 1]">
          <option value="">&mdash;</option>
          <optgroup v-for="(group, teamName) in groupedRiders" :key="teamName" :label="teamName">
            <option v-for="r in group" :key="r.id" :value="r.id">{{ r.name }} ({{ r.price }})</option>
          </optgroup>
        </select>
      </div>
    </div>

    <h4 style="margin:18px 0 8px;">{{ t("jersey_holders_title") }}</h4>
    <div class="tp-jersey-grid">
      <div v-for="j in jerseyKeys" :key="j" class="tp-jersey-cell">
        <label><span class="tp-dot" :class="j"></span>{{ jerseyLabel(j) }}</label>
        <select class="tp-select" v-model="jerseys[j]">
          <option value="">&mdash;</option>
          <optgroup v-for="(group, teamName) in groupedRiders" :key="teamName" :label="teamName">
            <option v-for="r in group" :key="r.id" :value="r.id">{{ r.name }} ({{ r.price }})</option>
          </optgroup>
        </select>
      </div>
    </div>

    <h4 style="margin:18px 0 8px;">{{ t("gc_entry_title") }}</h4>
    <p class="tp-note" style="margin:-4px 0 8px;">{{ t("gc_entry_note") }}</p>
    <div class="tp-place-grid">
      <div v-for="i in 10" :key="'g' + i" class="tp-place-cell">
        <label>{{ ordinal(i) }}</label>
        <select class="tp-select" v-model="gc[i - 1]">
          <option value="">&mdash;</option>
          <optgroup v-for="(group, teamName) in groupedRiders" :key="teamName" :label="teamName">
            <option v-for="r in group" :key="r.id" :value="r.id">{{ r.name }} ({{ r.price }})</option>
          </optgroup>
        </select>
      </div>
    </div>

    <div v-if="error" class="tp-error" style="margin-top:14px;">{{ error }}</div>
    <div style="margin-top:18px;">
      <button class="tp-btn" :disabled="busy" @click="save">{{ t("btn_save_lock_stage", { n: stageNum }) }}</button>
    </div>
    <p class="tp-note">{{ t("stage_entry_note") }}</p>
  </div>

  <div class="tp-card">
    <h4 style="margin-top:0;">{{ t("csv_import_title") }}</h4>
    <p class="tp-note" style="margin-bottom:10px;">{{ t("csv_import_note") }}</p>
    <pre class="mono" style="background:var(--surface2); border:1px solid var(--border); border-radius:8px; padding:10px 12px; font-size:12px; overflow-x:auto; margin:0 0 10px;">{{ t("csv_import_ph") }}</pre>
    <textarea class="tp-textarea" v-model="csvText" rows="6" placeholder="1,Rider Name"></textarea>
    <div style="margin-top:10px;">
      <button class="tp-btn secondary" :disabled="busy" @click="importCsv">{{ t("btn_import_csv", { n: stageNum }) }}</button>
    </div>
    <p v-if="csvMessage" class="tp-note" :style="{ color: csvHasErrors ? 'var(--red)' : 'var(--green)' }">{{ csvMessage }}</p>
  </div>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";

const store = usePouleStore();
const { t, ordinal, jerseyLabel } = useI18n();

const jerseyKeys = ["yellow", "green", "polka", "white"];

const stageNum = ref(Math.min(21, store.poule.currentStage + 1));
const places = ref(Array.from({ length: 15 }, () => ""));
const gc = ref(Array.from({ length: 10 }, () => ""));
const jerseys = ref({ yellow: "", green: "", polka: "", white: "" });
const error = ref(null);
const busy = ref(false);
const csvText = ref("");
const csvMessage = ref("");
const csvHasErrors = ref(false);

const groupedRiders = computed(() => {
  const out = {};
  store.riders.filter((r) => r.active !== false).forEach((r) => {
    (out[r.team] = out[r.team] || []).push(r);
  });
  Object.values(out).forEach((list) => list.sort((a, b) => a.name.localeCompare(b.name)));
  return Object.fromEntries(Object.entries(out).sort(([a], [b]) => a.localeCompare(b)));
});

function loadStageIntoForm(n) {
  const stage = store.poule.stages[n - 1];
  const top = stage?.top || [];
  const gcEntries = stage?.gc || [];
  places.value = Array.from({ length: 15 }, (_, i) => top[i] || "");
  gc.value = Array.from({ length: 10 }, (_, i) => gcEntries[i] || "");
  jerseys.value = { yellow: "", green: "", polka: "", white: "", ...(stage?.jerseys || {}) };
  csvMessage.value = "";
}

watch(stageNum, (n) => loadStageIntoForm(n), { immediate: true });

async function save() {
  error.value = null;
  busy.value = true;
  try {
    await store.saveStageResult(stageNum.value, places.value, jerseys.value, gc.value);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}

async function importCsv() {
  if (!csvText.value.trim()) {
    csvMessage.value = t("csv_no_rows");
    csvHasErrors.value = true;
    return;
  }
  try {
    const result = await store.importStageCsv(stageNum.value, csvText.value);
    const matched = result.matchedRows;
    if (result.saved) {
      csvMessage.value = t("csv_result_ok", { n: stageNum.value, matched, total: result.totalRows });
      csvHasErrors.value = false;
      loadStageIntoForm(stageNum.value);
    } else {
      csvMessage.value = t("csv_result_errors", { n: stageNum.value, ok: matched, bad: result.unmatchedNames.join(", ") });
      csvHasErrors.value = true;
    }
  } catch (e) {
    csvMessage.value = e.message;
    csvHasErrors.value = true;
  }
}
</script>
