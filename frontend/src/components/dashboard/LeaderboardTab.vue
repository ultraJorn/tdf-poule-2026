<template>
  <div class="tp-card">
    <p v-if="store.leaderboard === null" class="tp-note">{{ t("loading_leaderboard") }}</p>
    <template v-else>
      <div v-for="(row, i) in store.leaderboard" :key="row.username">
        <div class="tp-lb-row" :style="raceStarted ? 'cursor:pointer;' : ''" @click="raceStarted && toggleTeam(row.username)">
          <div class="tp-bibtag" :class="medalClass(i)">{{ i + 1 }}</div>
          <div class="tp-lb-bar-wrap">
            <div class="tp-lb-name">
              {{ row.username }}
              <span v-if="row.username.toLowerCase() === store.username.toLowerCase()" class="tp-you">{{ t("you_badge") }}</span>
            </div>
            <div class="tp-lb-bar-bg"><div class="tp-lb-bar-fill" :style="{ width: (row.total / max) * 100 + '%' }"></div></div>
          </div>
          <div class="tp-lb-score">{{ row.total }}</div>
          <div v-if="raceStarted" class="tp-pill muted" style="font-size:10px; margin-left:8px; cursor:pointer; white-space:nowrap;">
            {{ expandedUsername === row.username ? t("hide_team") : t("view_team") }}
          </div>
        </div>
        <div v-if="expandedUsername === row.username" style="padding:10px 0 14px 32px;">
          <p v-if="loadingTeam" class="tp-note">{{ t("lb_loading_team") }}</p>
          <p v-else-if="teamError" class="tp-note" style="color:var(--red);">{{ teamError }}</p>
          <p v-else-if="!expandedTeam" class="tp-note">{{ t("lb_no_team") }}</p>
          <template v-else>
            <p class="tp-note" style="margin-bottom:8px; font-weight:600;">{{ t("lb_team_heading", { username: row.username }) }}</p>
            <div class="tp-rider-grid">
              <div v-for="id in currentRoster(expandedTeam)" :key="id" class="tp-rider-card" style="cursor:default;">
                <div>
                  <div class="tp-rider-name">
                    {{ store.ridersById[id]?.name }}
                    <span v-if="store.ridersById[id]?.active === false" class="tp-pill muted">DNF</span>
                  </div>
                  <div class="tp-rider-meta">{{ store.ridersById[id]?.team }} &middot; {{ tagLabel(store.ridersById[id]?.tag) }}</div>
                </div>
                <div class="tp-rider-price">{{ store.ridersById[id]?.price }}</div>
              </div>
            </div>
          </template>
        </div>
      </div>
      <p v-if="!store.leaderboard.length" class="tp-note">{{ t("no_teams_yet") }}</p>
      <p v-if="!raceStarted && store.leaderboard.length" class="tp-note" style="margin-top:14px; text-align:center;">{{ t("teams_hidden_pre_race") }}</p>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { usePouleStore } from "../../stores/poule";
import { useI18n } from "../../i18n";
import { api } from "../../api";

const store = usePouleStore();
const { t, tagLabel } = useI18n();

const max = computed(() => Math.max(1, ...(store.leaderboard || []).map((r) => r.total)));
const raceStarted = computed(() => store.poule.currentStage > 0);

const expandedUsername = ref(null);
const expandedTeam = ref(null);
const loadingTeam = ref(false);
const teamError = ref(null);

function medalClass(i) { return i === 0 ? "gold" : i === 1 ? "silver" : i === 2 ? "bronze" : ""; }

async function toggleTeam(username) {
  if (expandedUsername.value === username) {
    expandedUsername.value = null;
    expandedTeam.value = null;
    return;
  }
  expandedUsername.value = username;
  expandedTeam.value = null;
  teamError.value = null;
  loadingTeam.value = true;
  try {
    expandedTeam.value = await api.getTeam(store.code, username);
  } catch (e) {
    teamError.value = e.message;
  } finally {
    loadingTeam.value = false;
  }
}

function currentRoster(team) {
  let r = team.riderIds.slice();
  const swaps = (team.swapLog || []).slice().sort((a, b) => a.effectiveFromStage - b.effectiveFromStage);
  for (const sw of swaps) {
    if (sw.effectiveFromStage > store.poule.currentStage + 1) continue;
    const idx = r.indexOf(sw.outId);
    if (idx !== -1) r[idx] = sw.inId;
    else if (!r.includes(sw.inId)) r.push(sw.inId);
  }
  return r;
}

onMounted(() => {
  if (store.leaderboard === null) store.loadLeaderboard();
});
</script>
