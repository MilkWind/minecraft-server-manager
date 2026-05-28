import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './style.css';
import 'animal-island-vue/style';

createApp(App)
  .use(router)
  .mount('#app');
