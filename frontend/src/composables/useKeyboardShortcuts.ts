import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

type ShortcutHandler = (e: KeyboardEvent) => void

interface Shortcut {
  key: string
  ctrl?: boolean
  alt?: boolean
  shift?: boolean
  handler: ShortcutHandler
  description: string
}

export function useKeyboardShortcuts(shortcuts: Shortcut[]) {
  const onKeyDown = (e: KeyboardEvent) => {
    // Don't trigger shortcuts when user is typing in input/textarea
    const target = e.target as HTMLElement
    const isInput = target.tagName === 'INPUT' ||
                    target.tagName === 'TEXTAREA' ||
                    target.isContentEditable

    for (const s of shortcuts) {
      const ctrlMatch = s.ctrl ? (e.ctrlKey || e.metaKey) : true
      const altMatch = s.alt ? e.altKey : !e.altKey
      const shiftMatch = s.shift ? e.shiftKey : !e.shiftKey
      const keyMatch = e.key.toLowerCase() === s.key.toLowerCase()

      if (keyMatch && ctrlMatch && altMatch && shiftMatch) {
        // Allow Ctrl-based shortcuts even in inputs (except Ctrl+S)
        if (isInput && !s.ctrl) continue
        e.preventDefault()
        s.handler(e)
        return
      }
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', onKeyDown)
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', onKeyDown)
  })
}

// Predefined shortcuts for different pages
export function useGlobalShortcuts() {
  const router = useRouter()

  const shortcuts: Shortcut[] = [
    {
      key: 'k',
      ctrl: true,
      handler: () => {
        router.push('/search')
      },
      description: '全局搜索'
    }
  ]

  return useKeyboardShortcuts(shortcuts)
}

export type { Shortcut }
