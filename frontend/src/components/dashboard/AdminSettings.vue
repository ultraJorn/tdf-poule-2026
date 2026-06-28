<template>
  <div class="tp-card">
    <div v-if="error" class="tp-error">{{ error }}</div>
    <div class="tp-field"><label>{{ t("label_pouleName") }}</label><input class="tp-input" v-model="name"></div>
    <div class="tp-row">
      <div class="tp-field"><label>{{ t("label_teamsize_short") }}</label><input class="tp-input" type="number" v-model.number="teamSize" min="3" max="15"></div>
      <div class="tp-field"><label>{{ t("label_budget") }}</label><input class="tp-input" type="number" v-model.number="budgetCap" min="20" max="300"></div>
      <div class="tp-field"><label>{{ t("label_swaps_short") }}</label><input class="tp-input" type="number" v-model.number="totalSwaps" min="0" max="21"></div>
    </div>
    <p class="tp-note" style="margin-bottom:12px;">{{ t("settings_note") }}</p>
    <button class="tp-btn" :disabled="busy" @click="save">{{ t("btn_save_settings") }}</button>
  </div>

  <div class="tp-card">
    <h4 style="margin-top:0;">{{ t("backup_title") }}</h4>
    <p class="tp-note" style="margin-bottom:10px;">{{ t("backup_note") }}</p>
    <textarea class="tp-textarea mono" rows="4" readonly :value="store.backupCode || ''" style="font-size:11px;" @click="$event.target.select()"></textarea>
    <div style="margin-top:10px;">
      <button class="tp-btn secondary" @click="makeBackup">{{ store.backupCopied ? t("btn_copied") : t("btn_copy_backup") }}</button>
    </div>
  </div>

  <div class="tp-card" style="border-color:var(--red);">
    <h4 style="margin-top:0; color:var(--red);">{{ t("danger_zone_title") }}</h4>
    <template v-if="confirmDelete">
      <p style="margin-bottom:12px;">{{ t("delete_confirm_text", { name: store.poule.name }) }}</p>
      <div class="tp-row">
        <button class="tp-btn danger" :disabled="busy" @click="doDelete">{{ t("btn_delete_confirm") }}</button>
        <button class="tp-btn secondary" @click="confirmDelete = false">{{ t("btn_cancel") }}</button>
      </div>
    </template>
    <template v-else>
      <p class="tp-note" style="margin-bottom:12px;">{{ t("delete_poule_note") }}</p>
      <button class="tp-btn danger" @click="confirmDelete = true">{{ t("btn_delete_poule") }}</button>
    </template>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";

const store = usePouleStore();
const router = useRouter();
const { t } = useI18n();

const name = ref(store.poule.name);
const teamSize = ref(store.poule.teamSize);
const budgetCap = ref(store.poule.budgetCap);
const totalSwaps = ref(store.poule.totalSwaps);
const busy = ref(false);
const error = ref(null);
const confirmDelete = ref(false);

async function save() {
  error.value = null;
  busy.value = true;
  try {
    await store.saveSettings({ name: name.value, teamSize: teamSize.value, budgetCap: budgetCap.value, totalSwaps: totalSwaps.value });
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}

async function makeBackup() {
  error.value = null;
  try {
    await store.makeBackup();
    await navigator.clipboard.writeText(store.backupCode);
    store.backupCopied = true;
    setTimeout(() => { store.backupCopied = false; }, 2500);
  } catch (e) {
    error.value = e.message;
  }
}

async function doDelete() {
  error.value = null;
  busy.value = true;
  try {
    await store.deletePoule();
    router.push("/");
  } catch (e) {
    error.value = e.message;
    busy.value = false;
  }
}
</script>
