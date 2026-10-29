import { useState } from 'react'
import { useAction } from '../../shared/api/useAction'
import { markEntry } from './api'
import type { StudyPlan, StudyPlanEntry } from './api'

/**
 * Ticking a plan's topics off. A tick shows the server's reply at once; the next load of the plan
 * replaces it, so a background refresh never keeps showing an older tick.
 */
export function useEntryProgress(loaded: StudyPlan) {
  const progress = useAction()
  const [saving, setSaving] = useState<number>()
  const [saved, setSaved] = useState<StudyPlan>()
  const [shownLoad, setShownLoad] = useState(loaded)
  if (shownLoad !== loaded) {
    setShownLoad(loaded)
    setSaved(undefined)
  }

  async function toggle(entry: StudyPlanEntry, completed: boolean) {
    setSaving(entry.id)
    const result = await progress.run(() => markEntry(loaded.id, entry.id, completed))
    if (result.ok) setSaved(result.value)
    setSaving(undefined)
  }

  return { plan: saved ?? loaded, savingEntryId: saving, toggle, error: progress.error }
}
