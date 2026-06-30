<template>
  <div class="tp-card">
    <h3 style="margin-top:0;">{{ t("participants_title") }}</h3>
    <p class="tp-note" style="margin-bottom:14px;">{{ t("participants_note") }}</p>
    <div v-if="error" class="tp-error">{{ error }}</div>
    <p v-if="loading" class="tp-note">{{ t("lb_loading_team") }}</p>
    <p v-else-if="!rows.length" class="tp-note">{{ t("no_teams_yet") }}</p>
    <template v-else>
      <div v-for="row in rows" :key="row.username"
           style="display:flex; align-items:center; justify-content:space-between; padding:8px 0; border-bottom:1px solid var(--border);">
        <div>
          <span class="tp-rider-name">{{ row.username }}</span>
          <span v-if="row.username.toLowerCase() === store.username.toLowerCase()" class="tp-you" style="margin-left:6px;">{{ t("you_badge") }}</span>
          <span class="tp-note" style="margin-left:8px;">{{ row.total }} pts</span>
        </div>
        <button class="tp-btn secondary small"
                :disabled="row.username.toLowerCase() === store.username.toLowerCase()"
                @click="confirmRemove(row.username)">
          {{ t("btn_remove_participant") }}
        </button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";

const store = usePouleStore();
const { t } = useI18n();

const rows = ref([]);
const loading = ref(true);
const error = ref(null);

onMounted(async () => {
  try {
    await store.loadLeaderboard();
    rows.value = store.leaderboard || [];
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});

async function confirmRemove(username) {
  if (!confirm(t("confirm_remove_participant", { username }))) return;
  error.value = null;
  try {
    await store.deleteParticipant(username);
    rows.value = rows.value.filter((r) => r.username !== username);
  } catch (e) {
    error.value = e.message;
  }
}
</script>
