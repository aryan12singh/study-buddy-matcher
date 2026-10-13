import { useEffect, useRef, useState } from 'react'
import { getMatchingConfig, restoreMatchingDefaults, updateMatchingConfig } from './api'
import type { MatchingConfig, MatchingCriterion } from './api'
import { addsUpToOne, CRITERIA, formatPercentage, formatTotal, rebalance, STRATEGIES, toConfig, toForm, toggleCriterion, validateSettings, weightTotal } from './matchingSettingsForm'
import type { MatchingSettingsForm } from './matchingSettingsForm'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import ActionNotice from '../../shared/components/ActionNotice'
import FormActions from '../../shared/components/FormActions'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import { useFormExit } from '../../shared/components/useFormExit'

export default function MatchingSettingsPage() {
  const resource = useResource('admin-matching-config', getMatchingConfig, true, false)
  const action = useAction(), initialised = useRef(false)
  const [form, setForm] = useState<MatchingSettingsForm | null>(null),
    [baseline, setBaseline] = useState(''),
    [saved, setSaved] = useState<MatchingConfig>(),
    [errors, setErrors] = useState<Record<string, string>>({}),
    [confirmation, setConfirmation] = useState<'restore' | 'reload'>()
  const dirty = form !== null && JSON.stringify(form) !== baseline
  const exit = useFormExit(dirty, action.pending)

  useEffect(() => {
    if (resource.data && !initialised.current) {
      const loaded = toForm(resource.data)
      setForm(loaded)
      setBaseline(JSON.stringify(loaded))
      setSaved(resource.data)
      initialised.current = true
    }
  }, [resource.data])

  function adopt(config: MatchingConfig) {
    const next = toForm(config)
    setForm(next)
    setBaseline(JSON.stringify(next))
    setSaved(config)
    setErrors({})
  }

  function changeWeight(key: MatchingCriterion, value: string) {
    setForm(previous => previous && {
      ...previous,
      weights: { ...previous.weights, [previous.activeStrategy]: rebalance(previous.weights[previous.activeStrategy], key, value, previous.enabledCriteria) }
    })
    setErrors({})
  }

  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!form || !saved) return
    const invalid = validateSettings(form)
    setErrors(invalid)
    if (Object.keys(invalid).length) return
    const result = await action.run(() => updateMatchingConfig({ ...toConfig(form), expectedVersion: saved.version }), 'Matching settings saved. They apply to the next match search.')
    if (result.ok) adopt(result.value)
  }

  async function confirm() {
    if (confirmation === 'restore' && saved) {
      const result = await action.run(() => restoreMatchingDefaults(saved.version), 'Startup matching defaults restored. They apply to the next match search.')
      if (!result.ok) return
      adopt(result.value)
    } else {
      initialised.current = false
      setForm(null)
      setErrors({})
      action.clear()
      resource.reload(true)
    }
    setConfirmation(undefined)
  }

  const shown = form && form.weights[form.activeStrategy]
  const total = shown ? weightTotal(shown) : null
  const activeLabel = form && STRATEGIES.find(({ value }) => value === form.activeStrategy)?.label
  return (
    <WindowPage title="Matching settings" description="Choose how students are ranked and how much each criterion counts. Changes apply to every student.">
      <StatePanel loading={resource.loading && !resource.data} error={resource.error} onRetry={() => resource.reload(true)} />
      {form && saved && (
        <form className="stack-form" onSubmit={submit} noValidate>
          <p className="row-meta">Saved configuration version {saved.version} · {saved.usingDefaults ? 'Startup defaults' : 'Saved database override'}. Overrides survive restart; restoring defaults uses the current startup configuration.</p>
          <fieldset className="settings-fieldset" disabled={action.pending}>
            <legend>Ranking strategy</legend>
            {STRATEGIES.map(strategy => (
              <label key={strategy.value} className="strategy-option">
                <input type="radio" name="activeStrategy" value={strategy.value} checked={form.activeStrategy === strategy.value}
                  onChange={() => { setForm({ ...form, activeStrategy: strategy.value }); setErrors({}) }} />
                <span><strong>{strategy.label}</strong><span className="field-hint">{strategy.description}</span></span>
              </label>
            ))}
          </fieldset>
          <fieldset className="settings-fieldset" disabled={action.pending}>
            <legend>Enabled criteria</legend>
            <p className="field-hint">Applies to every strategy. Disabled criteria contribute nothing, including a strategy's priority criterion.</p>
            <div className="actions">
              {CRITERIA.map(({ key, label }) => (
                <label key={key} className="strategy-option">
                  <input type="checkbox" checked={form.enabledCriteria.includes(key)} onChange={() => { setForm(toggleCriterion(form, key)); setErrors({}) }} />
                  Enable {label.toLowerCase()}
                </label>
              ))}
            </div>
          </fieldset>
          <fieldset className="settings-fieldset form-grid" disabled={action.pending}>
            <legend>{activeLabel} weights</legend>
            <p className="field-hint form-full">All enabled weights add up to 1. Editing one scales the others without rounding saved precision; 0.3 means 30%. Priority strategies compare their enabled priority first, then this weighted compatibility score.</p>
            {shown && CRITERIA.map(({ key, label }) => (
              <Field key={key} id={`weight-${key}`} label={label} error={errors[`weight-${key}`]}
                hint={!form.enabledCriteria.includes(key) ? 'Disabled: does not affect ranking.' : total !== null ? `${formatPercentage(shown[key])} of the total score` : undefined}>
                <input id={`weight-${key}`} type="number" min="0" max="1" step="any" inputMode="decimal" value={shown[key]}
                  disabled={!form.enabledCriteria.includes(key)} onChange={event => changeWeight(key, event.target.value)} />
              </Field>
            ))}
          </fieldset>
          {total !== null && <p className="field-hint" role="status">Total: <strong>{formatTotal(total)}</strong>{addsUpToOne(total) ? ' (adds up to 1)' : ' (must add up to 1)'}</p>}
          {errors.weights && <p className="field-error" role="alert">{errors.weights}</p>}
          <Field id="matching-min-score" label="Minimum compatibility score" hint="A score from 0 to 1. Students below this threshold are excluded from every search." error={errors.minScore}>
            <input id="matching-min-score" type="number" min="0" max="1" step="any" inputMode="decimal" value={form.minScore} disabled={action.pending}
              onChange={event => { setForm({ ...form, minScore: event.target.value }); setErrors({}) }} />
          </Field>
          <ActionNotice error={action.error} success={action.success} />
          <FormActions dirty={dirty} pending={action.pending}>
            <Button type="button" disabled={action.pending} onClick={() => setConfirmation('reload')}>Reload saved settings</Button>
            <Button type="button" disabled={action.pending} onClick={() => setConfirmation('restore')}>Restore startup defaults</Button>
            <Button type="submit" variant="primary" disabled={action.pending}>{action.pending ? 'Saving…' : 'Save matching settings'}</Button>
          </FormActions>
        </form>
      )}
      {confirmation && <ConfirmDialog title={confirmation === 'restore' ? 'Restore startup defaults?' : 'Reload saved settings?'}
        confirmLabel={confirmation === 'restore' ? 'Restore defaults' : 'Reload settings'} onConfirm={() => { void confirm() }} onClose={() => setConfirmation(undefined)} pending={action.pending} error={action.error}>
        <p>{confirmation === 'restore' ? 'Replace the saved override with the startup strategy, enabled criteria, weights and threshold for everyone.' : 'Load the currently active settings from the server.'} {dirty && 'Your unsaved edits will be discarded.'}</p>
      </ConfirmDialog>}
      {exit.confirmation}
    </WindowPage>
  )
}
