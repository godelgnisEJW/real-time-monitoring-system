import Vue from 'vue';
import VueRouter from 'vue-router';
import App from './App.vue';
import Routers from './router';
import ViewUI from 'view-design';
import ws from './api/websocket'
import 'view-design/dist/styles/iview.css';

Vue.prototype.ws = ws
Vue.use(VueRouter);
Vue.use(ViewUI);

// The routing configuration
const RouterConfig = {
  routes: Routers
};
const router = new VueRouter(RouterConfig);
Vue.config.productionTip = false
new Vue({
  el: '#app',
  router: router,
  render: h => h(App)
});