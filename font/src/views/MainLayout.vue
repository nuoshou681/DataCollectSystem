<script setup lang="ts">
import { HomeIcon, RectangleStackIcon, DocumentIcon,MagnifyingGlassIcon,BellIcon } from '@heroicons/vue/24/outline';
import { BugAntIcon } from '@heroicons/vue/24/solid'
import { navActive,iconActive } from '@/style';
import { useRoute } from 'vue-router';

const route = useRoute();
function getNavClass(path:string, type:number) {
  if(type===0&&route.path===path) return navActive;
  if(type===1&&route.path===path) return iconActive;
  return '';
}
</script>
<template>
  <div class="flex text-black h-screen">
    <!-- Sidebar -->
    <aside class="w-64 shrink-0 felx flex-col border-2 border-gray-100">
      <div class="felx items-center px-2 py-4 font-bold text-xl ">
        <!-- Logo -->
        <BugAntIcon class="text-indigo-500 w-8 h-8" />
      </div>
      <!-- Nav -->
      <nav class="flex flex-col px-2 py-2 space-y-2">
        <div class="w-full rounded-lg">
          <RouterLink :class="route.path==='/'?'bg-gray-50':''" class="flex items-center rounded px-2 py-2 space-x-4" to="/">
            <HomeIcon :class="getNavClass('/',1)" class="w-6 h-6 text-gray-400" />
            <span :class="getNavClass('/',0)" class="font-medium text-gray-600">Dashboard</span>
          </RouterLink>
        </div>
        <div class="w-full rounded-lg ">
          <RouterLink :class="route.path==='/task'?'bg-gray-50':''"  class="flex items-center rounded px-2 py-2 space-x-4" to="/task">
            <RectangleStackIcon :class="getNavClass('/task',1)" class="w-6 h-6 text-gray-400" />
            <span :class="getNavClass('/task',0)" class="font-medium text-gray-600">Task</span>
          </RouterLink>
        </div>
        <div class="w-full rounded-lg ">
          <RouterLink :class="route.path==='/log'?'bg-gray-50':''"  class="flex items-center rounded px-2 py-2 space-x-4" to="/log">
            <DocumentIcon :class="getNavClass('/log',1)" class="w-6 h-6 text-gray-400" />
            <span :class="getNavClass('/log',0)" class="font-medium text-gray-600">Log</span>
          </RouterLink>
        </div>
      </nav>
    </aside>
    <!-- MainContent -->
    <div class="flex flex-col flex-1">
      <!-- Topbar -->
      <header class="w-full h-16 flex items-center px-6 border-b border-gray-100">
        <div class="relative w-2xl">
          <input type="text" id="rounded-email"
            class="pl-10 w-full rounded-lg appearance-none border border-gray-300 py-2 px-4 bg-white text-gray-700 placeholder-gray-400 shadow-sm text-base focus:outline-none focus:ring-2 focus:ring-purple-600 focus:border-transparent"
            placeholder="Search" />
          <MagnifyingGlassIcon class="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
        </div>
        <!-- Message Login -->
        <div class="ml-auto flex items-center space-x-4 px-8 py-4">
          <BellIcon class="w-6 h-6 hover:text-indigo-500" />
          <div class="flex items-center space-x-2">
            <!-- 退出登陆时记得要清除token localStorage.removeItem('token') -->
            <img src="https://randomuser.me/api/portraits/men/32.jpg" class="w-8 h-8 rounded-full" />
            <span>Tom Cook</span>
          </div>
        </div>
      </header>
      <!-- Content -->
      <div>
        <main>
          <RouterView />
        </main>
      </div>
    </div>
  </div>
</template>
