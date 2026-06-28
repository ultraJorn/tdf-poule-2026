<template>
  <div class="tp-hero" style="position:relative;">
    <LangToggle overlay />
    <RouterLink to="/" class="tp-link-btn">{{ t("back") }}</RouterLink>
    <h1 class="tp-title display" style="margin-top:10px;">{{ t("create_title") }}</h1>
    <p class="tp-sub">{{ t("create_subtitle") }}</p>
  </div>
  <div class="tp-center">
    <div v-if="error" class="tp-error">{{ error }}</div>
    <div class="tp-card">
      <div class="tp-field">
        <label>{{ t("label_pouleName") }}</label>
        <input class="tp-input" v-model="pouleName" :placeholder="t('ph_pouleName')" maxlength="60">
      </div>
      <div class="tp-field">
        <label>{{ t("label_username") }}</label>
        <input class="tp-input" v-model="username" :placeholder="t('ph_username')" maxlength="30">
      </div>
      <div class="tp-field">
        <label>{{ t("label_password") }}</label>
        <input class="tp-input" v-model="adminPassword" type="password" :placeholder="t('ph_password')" maxlength="40">
      </div>
      <div class="tp-row">
        <div class="tp-field">
          <label>{{ t("label_teamsize") }}</label>
          <input class="tp-input" type="number" v-model.number="teamSize" min="3" max="15">
        </div>
        <div class="tp-field">
          <label>{{ t("label_budget") }}</label>
          <input class="tp-input" type="number" v-model.number="budgetCap" min="20" max="300">
        </div>
        <div class="tp-field">
          <label>{{ t("label_swaps") }}</label>
          <input class="tp-input" type="number" v-model.number="totalSwaps" min="0" max="21">
        </div>
      </div>
      <p class="tp-note" style="margin-bottom:14px;">{{ t("create_pricing_note") }}</p>
      <button class="tp-btn" style="width:100%;" :disabled="busy" @click="submit">
        {{ busy ? t("btn_creating") : t("btn_create_poule") }}
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

const pouleName = ref("");
const username = ref("");
const adminPassword = ref("");
const teamSize = ref(9);
const budgetCap = ref(100);
const totalSwaps = ref(3);
const busy = ref(false);
const error = ref(null);

async function submit() {
  if (!pouleName.value.trim() || !username.value.trim() || !adminPassword.value) {
    error.value = t("err_create_fields");
    return;
  }
  error.value = null;
  busy.value = true;
  try {
    await store.createPoule({
      pouleName: pouleName.value.trim(),
      username: username.value.trim(),
      adminPassword: adminPassword.value,
      teamSize: teamSize.value,
      budgetCap: budgetCap.value,
      totalSwaps: totalSwaps.value
    });
    router.push(`/poule/${store.code}/team`);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
