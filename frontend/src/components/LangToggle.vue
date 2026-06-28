<template>
  <div :style="overlay ? overlayStyle : plainStyle">
    <button class="tp-chip" :class="{ active: lang === 'en' }" @click="store.setLang('en')" style="padding:3px 9px;">EN</button>
    <button class="tp-chip" :class="{ active: lang === 'nl' }" @click="store.setLang('nl')" style="padding:3px 9px;">NL</button>
  </div>
</template>

<script setup>
import { usePouleStore } from "../stores/poule";
import { useI18n } from "../i18n";

defineProps({ overlay: { type: Boolean, default: false } });

const store = usePouleStore();
const { lang } = useI18n();

const overlayStyle = "position:absolute; top:14px; right:18px; display:flex; gap:4px; z-index:2;";
const plainStyle = "display:flex; gap:4px;";
</script>
