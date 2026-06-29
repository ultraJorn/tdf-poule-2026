import { defineStore } from "pinia";
import { api } from "../api";

const MY_POULES_KEY = "tdf-my-poules";
const LANG_KEY = "tdf-lang";

function loadJson(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function ridersIndex(riders) {
  const m = {};
  (riders || []).forEach((r) => { m[r.id] = r; });
  return m;
}

export const usePouleStore = defineStore("poule", {
  state: () => ({
    lang: loadJson(LANG_KEY, "en"),
    myPoules: loadJson(MY_POULES_KEY, []),

    code: null,
    username: null,
    poule: null, // { code, name, budgetCap, teamSize, totalSwaps, currentStage, stages: [...] }
    riders: [],
    team: null, // { username, riderIds, swapsUsed, swapLog, total: { total, breakdown } }
    isAdmin: false,
    adminPassword: null, // kept in memory only, sent as X-Admin-Password on admin calls

    leaderboard: null,

    backupCode: null,
    backupCopied: false,

    schedule: null, // { stages, stage1Start, freeSwapWindowStart, freeSwapWindowActive }

    busy: false,
    error: null
  }),

  getters: {
    ridersById: (state) => ridersIndex(state.riders),
    scheduleByStage: (state) => {
      const m = {};
      (state.schedule?.stages || []).forEach((s) => { m[s.stage] = s; });
      return m;
    }
  },

  actions: {
    setLang(lang) {
      this.lang = lang;
      localStorage.setItem(LANG_KEY, JSON.stringify(lang));
    },

    rememberPoule(code, name, username) {
      let list = this.myPoules.filter((p) => !(p.code === code && p.username.toLowerCase() === username.toLowerCase()));
      list.unshift({ code, name, username, ts: Date.now() });
      this.myPoules = list.slice(0, 20);
      localStorage.setItem(MY_POULES_KEY, JSON.stringify(this.myPoules));
    },

    forgetPoule(code, username) {
      this.myPoules = this.myPoules.filter((p) => !(p.code === code && p.username.toLowerCase() === username.toLowerCase()));
      localStorage.setItem(MY_POULES_KEY, JSON.stringify(this.myPoules));
    },

    applyBundle(bundle) {
      this.poule = bundle.poule;
      this.riders = bundle.riders;
    },

    async createPoule(payload) {
      this.busy = true;
      this.error = null;
      try {
        const bundle = await api.createPoule(payload);
        this.code = bundle.poule.code;
        this.username = payload.username;
        this.isAdmin = true;
        this.adminPassword = payload.adminPassword;
        this.applyBundle(bundle);
        this.team = null;
        this.rememberPoule(this.code, this.poule.name, this.username);
        return bundle;
      } finally {
        this.busy = false;
      }
    },

    async enterPoule(code, username) {
      this.busy = true;
      this.error = null;
      try {
        const bundle = await api.getPoule(code);
        this.code = bundle.poule.code;
        this.username = username;
        this.isAdmin = false;
        this.adminPassword = null;
        this.applyBundle(bundle);
        try {
          this.team = await api.getTeam(this.code, username);
        } catch {
          this.team = null;
        }
        this.rememberPoule(this.code, this.poule.name, this.username);
        return bundle;
      } finally {
        this.busy = false;
      }
    },

    leavePoule() {
      this.code = null;
      this.username = null;
      this.poule = null;
      this.riders = [];
      this.team = null;
      this.isAdmin = false;
      this.adminPassword = null;
      this.leaderboard = null;
      this.backupCode = null;
    },

    async confirmTeam(riderIds) {
      this.busy = true;
      try {
        this.team = await api.createTeam(this.code, { username: this.username, riderIds });
        return this.team;
      } finally {
        this.busy = false;
      }
    },

    async swap(outId, inId) {
      this.team = await api.swap(this.code, this.username, { outId, inId });
      return this.team;
    },

    async refreshTeam() {
      try {
        this.team = await api.getTeam(this.code, this.username);
      } catch {
        this.team = null;
      }
    },

    async loadLeaderboard() {
      this.leaderboard = null;
      this.leaderboard = await api.leaderboard(this.code);
    },

    async loadSchedule() {
      if (this.schedule) return; // same real-world schedule for every poule, fetch once
      this.schedule = await api.getSchedule();
    },

    async refreshPoule() {
      this.applyBundle(await api.getPoule(this.code));
    },

    async unlockAdmin(password) {
      await api.checkAdminPassword(this.code, password);
      this.isAdmin = true;
      this.adminPassword = password;
    },

    async saveStageResult(stageNumber, finishOrder, jerseys, gcOrder) {
      const bundle = await api.saveStageResult(this.code, stageNumber, this.adminPassword, {
        finishOrder, jerseys, gcOrder
      });
      this.applyBundle(bundle);
      await this.refreshTeam();
    },

    async importStageCsv(stageNumber, csvText) {
      const result = await api.importStageCsv(this.code, stageNumber, this.adminPassword, csvText);
      if (result.saved) {
        await this.refreshPoule();
        await this.refreshTeam();
      }
      return result;
    },

    async addRider(payload) {
      const r = await api.addRider(this.code, this.adminPassword, payload);
      this.riders = [...this.riders, r];
      return r;
    },

    async updateRider(riderId, payload) {
      const r = await api.updateRider(this.code, riderId, this.adminPassword, payload);
      this.riders = this.riders.map((x) => (x.id === riderId ? r : x));
      return r;
    },

    async deleteRider(riderId) {
      await api.deleteRider(this.code, riderId, this.adminPassword);
      this.riders = this.riders.filter((x) => x.id !== riderId);
    },

    async bulkReplaceRiders(riders) {
      this.riders = await api.bulkReplaceRiders(this.code, this.adminPassword, { riders });
    },

    async saveSettings(payload) {
      const bundle = await api.updateSettings(this.code, this.adminPassword, payload);
      this.applyBundle(bundle);
    },

    async makeBackup() {
      const data = await api.exportBackup(this.code, this.adminPassword);
      this.backupCode = btoa(unescape(encodeURIComponent(JSON.stringify(data))));
      return this.backupCode;
    },

    async restoreFromCode(rawCode, username, newAdminPassword) {
      let data;
      try {
        data = JSON.parse(decodeURIComponent(escape(atob(rawCode.trim()))));
      } catch {
        throw new Error("That backup code couldn't be read. Make sure you copied the whole thing.");
      }
      const bundle = await api.restoreBackup(data, newAdminPassword);
      this.code = bundle.poule.code;
      this.username = (username || "").trim() || "organizer";
      this.isAdmin = false;
      this.adminPassword = null;
      this.applyBundle(bundle);
      try {
        this.team = await api.getTeam(this.code, this.username);
      } catch {
        this.team = null;
      }
      this.rememberPoule(this.code, this.poule.name, this.username);
      return bundle;
    },

    async deletePoule() {
      await api.deletePoule(this.code, this.adminPassword);
      const code = this.code;
      this.myPoules = this.myPoules.filter((p) => p.code !== code);
      localStorage.setItem(MY_POULES_KEY, JSON.stringify(this.myPoules));
      this.leavePoule();
    }
  }
});
