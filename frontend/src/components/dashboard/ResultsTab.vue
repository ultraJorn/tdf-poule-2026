<template>
  <div v-if="!scoredStages.length" class="tp-card"><p class="tp-note">{{ t("results_none_scored") }}</p></div>
  <template v-else>
    <div class="tp-card">
      <div class="tp-field" style="margin-bottom:0;">
        <label>{{ t("label_stage") }}</label>
        <select class="tp-select" v-model.number="selected">
          <option v-for="s in scoredStages" :key="s.n" :value="s.n">{{ t("stage_word") }} {{ s.n }} &mdash; {{ s.label }}</option>
        </select>
      </div>
    </div>
    <div class="tp-card">
      <h3 style="margin-top:0;">{{ t("results_stage_result") }}</h3>
      <div class="tp-note" style="margin:-6px 0 10px;">{{ t("stage_word") }} {{ stage.n }} &middot; {{ stage.label }}</div>
      <RankedList :ids="stage.top.slice(0, 10)" :empty="t('results_no_top')" />
    </div>
    <div class="tp-card">
      <h3 style="margin-top:0;">{{ t("results_gc") }}</h3>
      <div class="tp-note" style="margin:-6px 0 10px;">{{ t("stages_raced_after", { n: stage.n }) }}</div>
      <RankedList :ids="stage.gc.slice(0, 10)" :empty="t('results_no_gc')" />
    </div>
  </template>
</template>

<script setup>
import { ref, computed, watch } from "vue";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";
import RankedList from "./RankedList.vue";

const store = usePouleStore();
const { t } = useI18n();

const scoredStages = computed(() => store.poule.stages.filter((s) => s.locked));
const selected = ref(scoredStages.value.length ? scoredStages.value[scoredStages.value.length - 1].n : null);

watch(scoredStages, (list) => {
  if (list.length && !list.some((s) => s.n === selected.value)) {
    selected.value = list[list.length - 1].n;
  }
});

const stage = computed(() => store.poule.stages[selected.value - 1]);
</script>
