<script setup lang="ts">
import SvgIcon from '../components/SvgIcon.vue'
import { computed } from 'vue'

const props = defineProps({
  context: {
    type: String,
    required: true,
    validator: (prop) => ['desktop', 'mobile'].includes(prop as string),
  },
  // Allow disabling active navigation item highlighting for pages like shopping cart
  showActiveNavigation: {
    type: Boolean,
  },
})

const mainNavigationClass = computed(() => {
  let base = `main-navigation `
  if (props.context) base += `main-navigation--${props.context} `
  return base
})
</script>


<template>
  <nav id="main-navigation" :class="mainNavigationClass" aria-label="Main">
    <ul>
        <li>
          <RouterLink to="/" active-class="active" :aria-current="showActiveNavigation ? 'page' : false">
            <span>home RT</span>
          </RouterLink>
      </li>
      <li>
          <RouterLink to="datasets" active-class="active" :aria-current="showActiveNavigation ? 'page' : false">
            <span>Datasets RT</span>
          </RouterLink>
      </li>
       <li>
         <a
          href="javascript:void(0)"
          role="button"
          class="navy__has-children"
          :aria-current="showActiveNavigation"
        >
            <span>my menu</span>
        </a>
      </li>
      <li>
        <a href="javascript:alert('link')">
          <span>Geoportale</span>
        </a>
      </li>
      <li>
        <a
          href="javascript:void(0)"
          role="button"
          class="navy__has-children"
          :class="{ active: showActiveNavigation }"
          :aria-current="showActiveNavigation"
        >
          <span>Dienstleistungen</span>
          <SvgIcon v-if="context == 'mobile'" icon="ArrowRight" size="lg" />
        </a>
        <ul>
          <li>
            <a href="javascript:void(0)" role="button" class="navy__back">
              <SvgIcon icon="ArrowLeft" size="lg" />
              <span>Back</span>
            </a>
            <h2 class="navy__title">Dienstleistungen</h2>
            <a href="javascript:alert('link')">Überblick</a>
          </li>
          <li><a href="javascript:alert('link')">Datenmodellablage</a></li>
          <li>
            <a
              href="javascript:void(0)"
              role="button"
              class="navy__has-children"
              :class="{ active: showActiveNavigation }"
              :aria-current="showActiveNavigation"
            >
              <span>Geodienste</span>
              <SvgIcon icon="ArrowRight" size="lg" />
            </a>
            <ul>
              <li>
                <a href="javascript:void(0)" role="button" class="navy__back">
                  <SvgIcon icon="ArrowLeft" size="lg" />
                  <span>Back</span>
                </a>
                <h2 class="navy__title">Geodienste</h2>
                <a href="javascript:alert('link')">Überblick</a>
              </li>
              <li>
                <a
                  href="javascript:void(0)"
                  class="navy__has-children"
                  :class="{ active: showActiveNavigation }"
                  :aria-current="showActiveNavigation"
                >
                  <span>Darstellungsdienste</span>
                  <SvgIcon icon="ArrowRight" size="lg" />
                </a>
                <ul>
                  <li>
                    <a
                      href="javascript:void(0)"
                      role="button"
                      class="navy__back"
                    >
                      <SvgIcon icon="ArrowLeft" size="lg" />
                      <span>Back</span>
                    </a>
                    <h2 class="navy__title">Darstellungsdienste</h2>
                    <a href="javascript:alert('link')">Überblick</a>
                  </li>
                  <li>
                    <a
                      href="javascript:alert('link')"
                      :class="{ active: showActiveNavigation }"
                      :aria-current="showActiveNavigation ? 'page' : false"
                    >
                      Web Map Services
                    </a>
                  </li>
                  <li>
                    <a href="javascript:alert('link')">Web tiling Services</a>
                  </li>
                  <li>
                    <a href="javascript:alert('link')">Vector Tiles Service</a>
                  </li>
                  <li>
                    <a href="javascript:alert('link')"
                      >Web Integration: iFrame</a
                    >
                  </li>
                  <li><a href="javascript:alert('link')">FAQ API</a></li>
                </ul>
              </li>
              <li><a href="javascript:alert('link')">Download-Dienste</a></li>
              <li>
                <a href="javascript:alert('link')"
                  >Linked Data Dienst: GeoDaten semantisch verlinken</a
                >
              </li>
              <li>
                <a href="javascript:alert('link')"
                  >Allgemeine Nutzungsbedingungen und Betriebsbestimmungen der
                  Bundes Geodaten-Infrastruktur BGDI</a
                >
              </li>
              <li><a href="javascript:alert('link')">Suchdienst CSW</a></li>
              <li>
                <a href="javascript:alert('link')"
                  >Konformitätsprüfung Geobasisdienste nach eCH-0056</a
                >
              </li>
              <li><a href="javascript:alert('link')">INSPIRE Dienste</a></li>
            </ul>
          </li>
          <li><a href="javascript:alert('link')">Geodatenmodelle</a></li>
          <li><a href="javascript:alert('link')">FAQ</a></li>
          <li>
            <a href="javascript:alert('link')">Koordinatenreferenzsysteme</a>
          </li>
          <li>
            <a href="javascript:alert('link')">Geografische Bezeichnungen</a>
          </li>
          <li><a href="javascript:alert('link')">Verwaltungseinheiten</a></li>
        </ul>
      </li>
    </ul>
  </nav>
</template>
