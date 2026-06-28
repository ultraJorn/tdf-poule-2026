<template>
  <p v-if="!ids.length" class="tp-note">{{ empty }}</p>
  <div v-else>
    <div v-for="(id, i) in ids" :key="id" class="tp-lb-row">
      <div class="tp-bibtag" :class="medalClass(i)">{{ i + 1 }}</div>
      <div class="tp-lb-bar-wrap">
        <div class="tp-lb-name">{{ store.ridersById[id]?.name || id }}</div>
        <div class="tp-rider-meta">{{ store.ridersById[id]?.team }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { usePouleStore } from "../../stores/poule";

defineProps({ ids: { type: Array, default: () => [] }, empty: { type: String, default: "" } });
const store = usePouleStore();

function medalClass(i) { return i === 0 ? "gold" : i === 1 ? "silver" : i === 2 ? "bronze" : ""; }
</script>
