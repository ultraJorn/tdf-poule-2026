<template>
  <div class="tp-card">
    <p v-if="store.leaderboard === null" class="tp-note">{{ t("loading_leaderboard") }}</p>
    <template v-else>
      <div v-for="(row, i) in store.leaderboard" :key="row.username" class="tp-lb-row">
        <div class="tp-bibtag" :class="medalClass(i)">{{ i + 1 }}</div>
        <div class="tp-lb-bar-wrap">
          <div class="tp-lb-name">
            {{ row.username }}
            <span v-if="row.username.toLowerCase() === store.username.toLowerCase()" class="tp-you">{{ t("you_badge") }}</span>
          </div>
          <div class="tp-lb-bar-bg"><div class="tp-lb-bar-fill" :style="{ width: (row.total / max) * 100 + '%' }"></div></div>
        </div>
        <div class="tp-lb-score">{{ row.total }}</div>
      </div>
      <p v-if="!store.leaderboard.length" class="tp-note">{{ t("no_teams_yet") }}</p>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted } from "vue";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";

const store = usePouleStore();
const { t } = useI18n();

const max = computed(() => Math.max(1, ...(store.leaderboard || []).map((r) => r.total)));

function medalClass(i) { return i === 0 ? "gold" : i === 1 ? "silver" : i === 2 ? "bronze" : ""; }

onMounted(() => {
  if (store.leaderboard === null) store.loadLeaderboard();
});
</script>
