

<script setup lang="ts">

import type { MenuItem } from '@/model/menu-item';

const props = withDefaults(
  defineProps<{
    menuItems?: MenuItem[]
  }>(),
  {
    menuItems: () => []
  }
)


</script>
<template>
  <nav
    id="main-navigation"
    class="main-navigation main-navigation--desktop"
    aria-label="Main"
  >
    <ul class="menu-desktop">
      <li v-for="item in props.menuItems" :key="item.to">
        <RouterLink
          :to="item.to"
          active-class="active"
        >
          <span>{{ item.label }}</span>
        </RouterLink>
      </li>
    </ul>
  </nav>
</template>
