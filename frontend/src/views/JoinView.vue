<template>
  <div class="tp-hero" style="position:relative;">
    <LangToggle overlay />
    <RouterLink to="/" class="tp-link-btn">{{ t("back") }}</RouterLink>
    <h1 class="tp-title display" style="margin-top:10px;">{{ t("join_title") }}</h1>
    <p class="tp-sub">{{ t("join_subtitle") }}</p>
  </div>
  <div class="tp-center">
    <div v-if="error" class="tp-error">{{ error }}</div>
    <div class="tp-card">
      <div class="tp-field">
        <label>{{ t("label_code") }}</label>
        <input class="tp-input mono" v-model="code" placeholder="ABCDE" maxlength="8"
               style="text-transform:uppercase;letter-spacing:0.1em;" @keydown.enter="submit">
      </div>
      <div class="tp-field">
        <label>{{ t("label_username") }}</label>
        <input class="tp-input" v-model="username" :placeholder="t('ph_username')" maxlength="30" @keydown.enter="submit">
      </div>
      <button class="tp-btn" style="width:100%;" :disabled="busy" @click="submit">
        {{ busy ? t("btn_joining") : t("btn_continue") }}
      </button>
    </div>
    <p class="tp-note" style="margin-top:14px;">{{ t("forgot_code_note", { title: t("my_poules_title") }) }}</p>
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

const code = ref("");
const username = ref("");
const busy = ref(false);
const error = ref(null);

async function submit() {
  if (!code.value.trim() || !username.value.trim()) {
    error.value = t("err_join_fields");
    return;
  }
  error.value = null;
  busy.value = true;
  try {
    await store.enterPoule(code.value.trim().toUpperCase(), username.value.trim());
    router.push(store.team ? `/poule/${store.code}` : `/poule/${store.code}/team`);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
