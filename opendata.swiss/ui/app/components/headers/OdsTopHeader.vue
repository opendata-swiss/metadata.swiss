<script lang="ts" setup>

import LanguageSelector from '../LanguageSelector.vue';
import OdsButton from '../OdsButton.vue';


import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface Props {
  enableAuthentication?: boolean;
  authenticated?: boolean;
  username: string | undefined;
}

const props = withDefaults(defineProps<Props>(), {
  enableAuthentication: false,
  authenticated: false,
});

function loginOrLogout() {
    if (props.authenticated) {
        emit('logout');
    } else {
        emit('login');
    }
}

const emit = defineEmits<{
    (e: 'login' | 'logout'): void;
}>();

</script>

<template>
    <header id="top-bar-container" class="top-bar">
        <div id="top-bar" class="top-bar__bar">
          <div class="container container--flex ">
            <div class="top-bar__btn"/>
            <div class="top-bar__right">
                <div v-if="props.enableAuthentication">
                  <OdsButton
                      v-if="props.authenticated"
                      icon="Logout"
                      variant="bare-negative"
                      size="sm"
                      :title="t('message.top_header.logout')"
                      :aria-label="t('message.top_header.logout')"
                      icon-right
                      @click="loginOrLogout"
                  >
                    <span>{{ props.username ? props.username : t('message.top_header.logout')}}</span>
                  </OdsButton>

                  <OdsButton
                      v-if="!props.authenticated"
                      icon="Login"
                      variant="bare-negative"
                      size="sm"
                      :title="t('message.top_header.login')"
                      :aria-label="t('message.top_header.login')"
                      icon-right
                      @click="loginOrLogout"
                  >
                    <span>{{ t('message.top_header.login') }}</span>
                  </OdsButton>
                </div>

               <div class="language-selector">
                    <LanguageSelector />
                </div>
            </div>
          </div>
        </div>

    </header>
</template>

<style lang="scss" scoped>

</style>
