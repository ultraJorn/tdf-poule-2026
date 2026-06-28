<template>
  <!-- App.vue already wraps every route in #tdf-poule, so this view starts straight at
       .tp-hero rather than nesting a second #tdf-poule id. -->
  <div class="tp-hero">
    <div class="tp-eyebrow">Site admin</div>
    <h1 class="tp-title display">All poules on this deployment</h1>
    <p class="tp-sub">
      Gated by a single shared secret (the backend's <span class="mono">SITE_ADMIN_SECRET</span>),
      not any individual poule's organizer passphrase. Deleting here is permanent and doesn't
      ask for that poule's own password.
    </p>
  </div>
  <div class="tp-body">
      <div v-if="error" class="tp-error">{{ error }}</div>

      <div v-if="!unlocked" class="tp-card">
        <div class="tp-field">
          <label>Site admin secret</label>
          <input class="tp-input" v-model="secret" type="password" placeholder="SITE_ADMIN_SECRET value" @keydown.enter="unlock">
        </div>
        <button class="tp-btn" :disabled="busy" @click="unlock">{{ busy ? "Checking…" : "Unlock" }}</button>
      </div>

      <template v-else>
        <div class="tp-card" style="display:flex; justify-content:space-between; align-items:center;">
          <div class="tp-note" style="margin:0;">{{ poules.length }} poule{{ poules.length === 1 ? "" : "s" }} total</div>
          <div style="display:flex; gap:8px;">
            <button class="tp-btn secondary small" :disabled="busy" @click="refresh">Refresh</button>
            <button class="tp-btn secondary small" @click="lock">Lock</button>
          </div>
        </div>

        <div class="tp-card">
          <div class="tp-scroll">
            <table class="tp-table">
              <tr>
                <th>Code</th><th>Name</th><th>Created</th><th>Stage</th><th>Riders</th><th>Teams</th><th></th>
              </tr>
              <tr v-for="p in poules" :key="p.code">
                <td class="mono">{{ p.code }}</td>
                <td>{{ p.name }}</td>
                <td>{{ formatDate(p.createdAt) }}</td>
                <td>{{ p.currentStage }}/21</td>
                <td>{{ p.riderCount }}</td>
                <td>{{ p.teamCount }}</td>
                <td>
                  <template v-if="confirmCode === p.code">
                    <button class="tp-btn danger small" :disabled="busy" @click="doDelete(p.code)">Confirm delete</button>
                    <button class="tp-btn secondary small" @click="confirmCode = null">Cancel</button>
                  </template>
                  <button v-else class="tp-btn danger small" @click="confirmCode = p.code">Delete</button>
                </td>
              </tr>
            </table>
          </div>
          <p v-if="!poules.length" class="tp-note">No poules exist on this deployment.</p>
        </div>
      </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { api } from "../api";

const SECRET_KEY = "tdf-site-admin-secret";

const secret = ref(sessionStorage.getItem(SECRET_KEY) || "");
const unlocked = ref(false);
const poules = ref([]);
const busy = ref(false);
const error = ref(null);
const confirmCode = ref(null);

function formatDate(iso) {
  try { return new Date(iso).toLocaleString(); } catch { return iso; }
}

async function unlock() {
  if (!secret.value.trim()) return;
  error.value = null;
  busy.value = true;
  try {
    poules.value = await api.listAllPoules(secret.value);
    unlocked.value = true;
    sessionStorage.setItem(SECRET_KEY, secret.value);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}

async function refresh() {
  error.value = null;
  busy.value = true;
  try {
    poules.value = await api.listAllPoules(secret.value);
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}

function lock() {
  unlocked.value = false;
  poules.value = [];
  sessionStorage.removeItem(SECRET_KEY);
}

async function doDelete(code) {
  error.value = null;
  busy.value = true;
  try {
    await api.deletePouleAsSiteAdmin(code, secret.value);
    poules.value = poules.value.filter((p) => p.code !== code);
    confirmCode.value = null;
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}

onMounted(() => {
  if (secret.value) unlock();
});
</script>
