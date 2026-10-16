import { ref } from 'vue'

/* Singleton theme state: 'light' | 'dark', persisted to localStorage. */
const theme = ref('light')
const KEY = 'meridian-theme'

function apply(t) {
  theme.value = t
  document.documentElement.dataset.theme = t
  try { localStorage.setItem(KEY, t) } catch { /* private mode */ }
}

export function useTheme() {
  return {
    theme,
    toggle() {
      apply(theme.value === 'light' ? 'dark' : 'light')
    },
    init() {
      let t = 'light'
      try {
        t = localStorage.getItem(KEY)
          || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
      } catch { /* ignore */ }
      apply(t)
    },
  }
}
