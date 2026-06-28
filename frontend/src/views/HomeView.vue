<template>
  <div class="tp-hero" style="position:relative;">
    <LangToggle overlay />
    <div class="tp-eyebrow">Tour de France &middot; 2026</div>
    <h1 class="tp-title display">{{ t("home_title") }}</h1>
    <p class="tp-sub">{{ t("home_subtitle") }}</p>
  </div>
  <div class="tp-center">
    <div v-if="error" class="tp-error">{{ error }}</div>

    <div v-if="store.myPoules.length" class="tp-card">
      <h3 style="margin:0 0 4px;">{{ t("my_poules_title") }}</h3>
      <p class="tp-note" style="margin-bottom:12px;">{{ t("my_poules_note") }}</p>
      <div v-for="p in store.myPoules" :key="p.code + p.username" class="tp-rider-card" style="cursor:default;">
        <div>
          <div class="tp-rider-name">{{ p.name }}</div>
          <div class="tp-rider-meta mono">{{ p.code }}</div>
        </div>
        <button class="tp-btn secondary small" style="margin-left:auto;" @click="continueAs(p)">
          {{ t("btn_continue_as", { username: p.username }) }}
        </button>
        <button class="tp-btn secondary small" :title="t('btn_forget')" @click="store.forgetPoule(p.code, p.username)">&times;</button>
      </div>
    </div>

    <div class="tp-card">
      <h3 style="margin:0 0 6px;">{{ t("home_create_title") }}</h3>
      <p class="tp-note" style="margin-bottom:14px;">{{ t("home_create_note") }}</p>
      <RouterLink to="/create" class="tp-btn" style="width:100%; text-align:center;">{{ t("home_create_btn") }}</RouterLink>
    </div>
    <div class="tp-card">
      <h3 style="margin:0 0 6px;">{{ t("home_join_title") }}</h3>
      <p class="tp-note" style="margin-bottom:14px;">{{ t("home_join_note") }}</p>
      <RouterLink to="/join" class="tp-btn secondary" style="width:100%; text-align:center;">{{ t("home_join_btn") }}</RouterLink>
    </div>

    <div class="tp-card">
      <h3 style="margin:0 0 6px;">{{ t("restore_title") }}</h3>
      <p class="tp-note" style="margin-bottom:12px;">{{ t("restore_note") }}</p>
      <textarea class="tp-textarea mono" v-model="restoreCode" rows="3" style="font-size:11px;"></textarea>
      <input class="tp-input" v-model="restoreName" :placeholder="t('restore_name_ph')" maxlength="30" style="margin-top:8px;">
      <input class="tp-input" v-model="restorePassword" type="password" :placeholder="t('restore_password_ph')" maxlength="40" style="margin-top:8px;">
      <button class="tp-btn secondary" style="width:100%; margin-top:10px;" :disabled="busy" @click="doRestore">
        {{ t("btn_restore") }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { usePouleStore } from "../stores/poule";
import { useI18n } from "../i18n";
import LangToggle from "../components/LangToggle.vue";

const store = usePouleStore();
const router = useRouter();
const { t } = useI18n();

const error = ref(null);
const busy = ref(false);
const restoreCode = ref("");
const restoreName = ref("");
const restorePassword = ref("");

async function continueAs(p) {
  error.value = null;
  try {
    await store.enterPoule(p.code, p.username);
    router.push(store.team ? `/poule/${store.code}` : `/poule/${store.code}/team`);
  } catch (e) {
    error.value = e.message;
  }
}

async function doRestore() {
  if (!restoreCode.value.trim() || !restorePassword.value.trim()) {
    error.value = "Paste a backup code and set a new organizer passphrase.";
    return;
  }
  error.value = null;
  busy.value = true;
  try {
    await store.restoreFromCode(restoreCode.value, restoreName.value, restorePassword.value);
    router.push(store.team ? `/poule/${store.code}` : `/poule/${store.code}/team`);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
