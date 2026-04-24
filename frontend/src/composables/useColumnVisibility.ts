import { ref, watch } from 'vue'

export interface ColumnDef {
  key: string
  label: string
  visible: boolean
  width?: string
  minWidth?: string
}

export function useColumnVisibility(storageKey: string, defaultColumns: ColumnDef[]) {
  const saved = localStorage.getItem(`columns-${storageKey}`)
  const columns = ref<ColumnDef[]>(
    saved ? JSON.parse(saved) : defaultColumns
  )

  watch(columns, (val) => {
    localStorage.setItem(`columns-${storageKey}`, JSON.stringify(val))
  }, { deep: true })

  const visibleColumns = ref(new Map(
    columns.value.filter(c => c.visible).map(c => [c.key, c])
  ))

  watch(columns, () => {
    visibleColumns.value = new Map(
      columns.value.filter(c => c.visible).map(c => [c.key, c])
    )
  }, { deep: true })

  const toggle = (key: string) => {
    const col = columns.value.find(c => c.key === key)
    if (col) {
      col.visible = !col.visible
    }
  }

  const isVisible = (key: string) => visibleColumns.value.has(key)

  const reset = () => {
    columns.value = defaultColumns.map(c => ({ ...c }))
  }

  return { columns, visibleColumns, toggle, isVisible, reset }
}
