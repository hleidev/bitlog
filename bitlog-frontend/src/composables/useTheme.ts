import { ref, computed } from 'vue'

export type ThemeMode = 'system' | 'light' | 'dark'

const STORAGE_KEY = 'themeMode'

function readSaved(): ThemeMode {
  if (typeof localStorage === 'undefined') return 'system'
  const saved = localStorage.getItem(STORAGE_KEY) as ThemeMode | null
  if (saved === 'light' || saved === 'dark' || saved === 'system') return saved
  const old = localStorage.getItem('theme')
  return old === 'dark' ? 'dark' : old === 'light' ? 'light' : 'system'
}

const systemDark =
  typeof window !== 'undefined' ? window.matchMedia('(prefers-color-scheme: dark)') : null

const systemIsDark = ref(systemDark ? systemDark.matches : false)
const themeMode = ref<ThemeMode>(readSaved())

if (systemDark) {
  systemDark.addEventListener('change', (e) => {
    systemIsDark.value = e.matches
    if (themeMode.value === 'system') {
      document.documentElement.setAttribute('data-theme', e.matches ? 'dark' : 'light')
    }
  })
}

function applyTheme() {
  if (typeof document === 'undefined') return
  const isDark = themeMode.value === 'dark' || (themeMode.value === 'system' && systemIsDark.value)
  document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light')
}

applyTheme()

export function useTheme() {
  const isDark = computed(
    () => themeMode.value === 'dark' || (themeMode.value === 'system' && systemIsDark.value),
  )

  function setTheme(mode: ThemeMode) {
    themeMode.value = mode
    if (typeof localStorage !== 'undefined') localStorage.setItem(STORAGE_KEY, mode)
    applyTheme()
  }

  function toggleTheme() {
    setTheme(isDark.value ? 'light' : 'dark')
  }

  return { themeMode, isDark, setTheme, toggleTheme }
}
