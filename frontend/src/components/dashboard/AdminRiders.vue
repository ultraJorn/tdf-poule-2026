<template>
  <div class="tp-card">
    <div v-if="error" class="tp-error">{{ error }}</div>
    <div class="tp-row" style="margin-bottom:12px;">
      <input class="tp-input" v-model="filterText" :placeholder="t('filter_riders_ph')">
      <button class="tp-btn secondary" @click="addRow">{{ t("btn_add_rider") }}</button>
      <button class="tp-btn" :disabled="busy" @click="saveAll">{{ t("btn_save_changes") }}</button>
    </div>
    <div class="tp-scroll">
      <table class="tp-table">
        <tr><th>{{ t("th_name") }}</th><th>{{ t("th_team") }}</th><th>{{ t("th_price") }}</th><th>{{ t("th_tag") }}</th><th>{{ t("th_active") }}</th><th></th></tr>
        <tr v-for="r in filteredRiders" :key="r.id">
          <td><input class="tp-input" style="padding:5px 8px;" v-model="r.name"></td>
          <td><input class="tp-input" style="padding:5px 8px;" v-model="r.team"></td>
          <td><input class="tp-input mono" style="padding:5px 8px; width:64px;" type="number" v-model.number="r.price"></td>
          <td>
            <select class="tp-select" style="padding:5px 8px;" v-model="r.tag">
              <option v-for="tg in tags" :key="tg" :value="tg">{{ tagLabel(tg) }}</option>
            </select>
          </td>
          <td><input type="checkbox" v-model="r.active"></td>
          <td><button class="tp-btn danger small" @click="removeRow(r)">{{ t("btn_remove") }}</button></td>
        </tr>
      </table>
    </div>
  </div>
  <div class="tp-card">
    <h4 style="margin-top:0;">{{ t("bulk_import_title") }}</h4>
    <p class="tp-note" style="margin-bottom:10px;" v-html="bulkImportNote"></p>
    <textarea class="tp-textarea" v-model="bulkText" rows="5" placeholder="Tadej Pogačar, UAE Team Emirates - XRG, 20, GC"></textarea>
    <div style="margin-top:10px;"><button class="tp-btn secondary" :disabled="busy" @click="bulkImport">{{ t("btn_replace_riders") }}</button></div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from "vue";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";

const store = usePouleStore();
const { t, tagLabel } = useI18n();

const tags = ["GC", "Sprint", "Classics", "Climber", "Domestique"];
const filterText = ref("");
const error = ref(null);
const busy = ref(false);
const bulkText = ref("");
const bulkImportNote = computed(() => t("bulk_import_note", { fmt: "<span class='mono'>Name, Team, Price, Tag</span>" }));

// Local reactive copies so edits don't hit the API on every keystroke; "Save changes" pushes
// any rows whose fields actually differ from the store, and newly added rows get created.
const localRiders = reactive(store.riders.map((r) => ({ ...r, _isNew: false })));

const filteredRiders = computed(() => {
  const ft = filterText.value.toLowerCase();
  if (!ft) return localRiders;
  return localRiders.filter((r) => r.name.toLowerCase().includes(ft) || r.team.toLowerCase().includes(ft));
});

function addRow() {
  localRiders.push({ id: null, name: "New rider", team: "Unknown", price: 6, tag: "Domestique", active: true, _isNew: true });
}

async function removeRow(r) {
  if (r._isNew) {
    const idx = localRiders.indexOf(r);
    if (idx !== -1) localRiders.splice(idx, 1);
    return;
  }
  error.value = null;
  try {
    await store.deleteRider(r.id);
    const idx = localRiders.findIndex((x) => x.id === r.id);
    if (idx !== -1) localRiders.splice(idx, 1);
  } catch (e) {
    error.value = e.message;
  }
}

async function saveAll() {
  error.value = null;
  busy.value = true;
  try {
    for (const r of localRiders) {
      const payload = { name: r.name, team: r.team, price: r.price, tag: r.tag, active: r.active };
      if (r._isNew) {
        const created = await store.addRider(payload);
        r.id = created.id;
        r._isNew = false;
      } else {
        await store.updateRider(r.id, payload);
      }
    }
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}

async function bulkImport() {
  if (!bulkText.value.trim()) return;
  const lines = bulkText.value.trim().split("\n").map((l) => l.trim()).filter(Boolean);
  const riders = lines.map((line) => {
    const [name, team, price, tag] = line.split(",").map((s) => (s || "").trim());
    return { name: name || "Unnamed", team: team || "Unknown", price: parseInt(price, 10) || 6, tag: tag || "Domestique", active: true };
  });
  error.value = null;
  busy.value = true;
  try {
    await store.bulkReplaceRiders(riders);
    localRiders.splice(0, localRiders.length, ...store.riders.map((r) => ({ ...r, _isNew: false })));
    bulkText.value = "";
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
