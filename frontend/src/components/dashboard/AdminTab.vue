<template>
  <div v-if="!store.isAdmin" class="tp-card">
    <h3 style="margin-top:0;">{{ t("admin_access_title") }}</h3>
    <p class="tp-note" style="margin-bottom:12px;">{{ t("admin_access_note") }}</p>
    <div v-if="error" class="tp-error">{{ error }}</div>
    <div style="display:flex; gap:10px;">
      <input class="tp-input" v-model="password" type="password" :placeholder="t('ph_passphrase')" style="flex:1;" @keydown.enter="unlock">
      <button class="tp-btn" @click="unlock">{{ t("btn_unlock") }}</button>
    </div>
  </div>
  <template v-else>
    <div class="tp-tabs">
      <button v-for="s in sections" :key="s.key" class="tp-tab" :class="{ active: section === s.key }" @click="section = s.key">
        {{ s.label }}
      </button>
    </div>
    <AdminStageEntry v-if="section === 'stage'" />
    <AdminRiders v-else-if="section === 'riders'" />
    <AdminParticipants v-else-if="section === 'participants'" />
    <AdminSettings v-else-if="section === 'settings'" />
  </template>
</template>

<script setup>
import { ref, computed } from "vue";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";
import AdminStageEntry from "./AdminStageEntry.vue";
import AdminRiders from "./AdminRiders.vue";
import AdminParticipants from "./AdminParticipants.vue";
import AdminSettings from "./AdminSettings.vue";

const store = usePouleStore();
const { t } = useI18n();

const password = ref("");
const error = ref(null);
const section = ref("stage");

const sections = computed(() => [
  { key: "stage", label: t("admin_sec_stage") },
  { key: "riders", label: t("admin_sec_riders") },
  { key: "participants", label: t("admin_sec_participants") },
  { key: "settings", label: t("admin_sec_settings") }
]);

async function unlock() {
  error.value = null;
  try {
    await store.unlockAdmin(password.value);
  } catch (e) {
    error.value = e.message;
  }
}
</script>
