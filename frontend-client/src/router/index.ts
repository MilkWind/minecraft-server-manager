import { createRouter, createWebHistory } from 'vue-router';
import ServerDirectoryPage from '@/views/ServerDirectoryPage.vue';
import ServerWorkspacePage from '@/views/ServerWorkspacePage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/servers',
    },
    {
      path: '/servers',
      name: 'server-directory',
      component: ServerDirectoryPage,
    },
    {
      path: '/servers/:serverId/:clientType(visitor|manager)',
      name: 'server-workspace',
      component: ServerWorkspacePage,
    },
  ],
});

export default router;
