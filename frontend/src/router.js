import { createRouter, createWebHistory } from "vue-router";
import HomeView from "./views/HomeView.vue";
import CreateView from "./views/CreateView.vue";
import JoinView from "./views/JoinView.vue";
import BuildTeamView from "./views/BuildTeamView.vue";
import DashboardView from "./views/DashboardView.vue";
import SiteAdminView from "./views/SiteAdminView.vue";
import { usePouleStore } from "./stores/poule";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "home", component: HomeView },
    { path: "/create", name: "create", component: CreateView },
    { path: "/join", name: "join", component: JoinView },
    { path: "/poule/:code/team", name: "build-team", component: BuildTeamView },
    { path: "/poule/:code", name: "dashboard", component: DashboardView },
    // Not linked from anywhere in the normal UI -- gated by its own SITE_ADMIN_SECRET
    // check on the backend, not by anything in this router.
    { path: "/site-admin", name: "site-admin", component: SiteAdminView }
  ]
});

// The store is the single source of truth for "which poule/user am I"; if someone lands
// directly on a /poule/:code URL without that state (e.g. a fresh tab), bounce them home
// rather than rendering a half-loaded dashboard.
router.beforeEach((to) => {
  const needsActivePoule = to.name === "dashboard" || to.name === "build-team";
  if (!needsActivePoule) return true;
  const store = usePouleStore();
  if (!store.code || !store.poule) {
    return { name: "home" };
  }
  return true;
});

export default router;
