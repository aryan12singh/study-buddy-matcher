import { useEffect, useRef, useState } from 'react'
import { getMatchingConfig, updateMatchingConfig } from './api'
import type { MatchingCriterion } from './api'
import { addsUpToOne, formatTotal, rankedFirstCriteria, rebalance, STRATEGIES, toConfig, toForm, validateWeights, weightedCriteria, weightTotal } from './matchingSettingsForm'
import type { MatchingSettingsForm } from './matchingSettingsForm'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import ActionNotice from '../../shared/components/ActionNotice'
import FormActions from '../../shared/components/FormActions'
import { useFormExit } from '../../shared/components/useFormExit'

export default function MatchingSettingsPage() {
  const resource = useResource('admin-matching-config', signal => getMatchingConfig(signal), true, false)
  const action = useAction(),
    initialised = useRef(false)
  const [form, setForm] = useState<MatchingSettingsForm | null>(null),
    [baseline, setBaseline] = useState(''),
    [errors, setErrors] = useState<Record<string, string>>({})
  const dirty = form !== null && JSON.stringify(form) !== baseline
  const exit = useFormExit(dirty, action.pending)
  useEffect(
    () => {
      if (resource.data && !initialised.current) {
        const savedForm = toForm(resource.data)
        setForm(savedForm)
        setBaseline(JSON.stringify(savedForm))
        initialised.current = true
      }
    },
    [resource.data]
  )

  function changeWeight(key: MatchingCriterion, value: string) {
    setForm(previous => previous && {
      ...previous,
      weights: { ...previous.weights, [previous.activeStrategy]: rebalance(previous.weights[previous.activeStrategy], key, value) }
    })
    // Errors from the last save attempt no longer describe these numbers.
    setErrors({})
  }

  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!form) return
    const invalid = validateWeights(form.weights[form.activeStrategy])
    // Another strategy's weights can only be wrong if left half typed before switching.
    const otherInvalid = STRATEGIES.find(({ value }) => value !== form.activeStrategy && Object.keys(validateWeights(form.weights[value])).length)
    if (otherInvalid && !invalid.weights) invalid.weights = `Check the ${otherInvalid.label} weights: they must be numbers from 0 to 1 that add up to 1.`
    setErrors(invalid)
    if (Object.keys(invalid).length) return
    const result = await action.run(() => updateMatchingConfig(toConfig(form)), 'Matching settings saved. They apply to the next match search.')
    if (result.ok) {
      const savedForm = toForm(result.value)
      setForm(savedForm)
      setBaseline(JSON.stringify(savedForm))
    }
  }

  const shown = form && form.weights[form.activeStrategy]
  const total = shown ? weightTotal(shown) : null
  const activeLabel = form && STRATEGIES.find(({ value }) => value === form.activeStrategy)?.label
  return (
    <WindowPage
      title="Matching settings"
      description="Choose how students are ranked and how much each criterion counts. Changes apply to every student."
    >
      <StatePanel
        loading={resource.loading && !resource.data}
        error={resource.error}
        onRetry={() => resource.reload()}
      />
      {form && (
        <form
          className="stack-form"
          onSubmit={submit}
          noValidate
        >
          <fieldset className="settings-fieldset" disabled={action.pending}>
            <legend>Ranking strategy</legend>
            {STRATEGIES.map(strategy => (
              <label key={strategy.value} className="strategy-option">
                <input
                  type="radio"
                  name="activeStrategy"
                  value={strategy.value}
                  checked={form.activeStrategy === strategy.value}
                  onChange={() => { setForm({ ...form, activeStrategy: strategy.value }); setErrors({}) }}
                />
                <span>
                  <strong>{strategy.label}</strong>
                  <span className="field-hint">{strategy.description}</span>
                </span>
              </label>
            ))}
          </fieldset>
          <fieldset className="settings-fieldset form-grid" disabled={action.pending}>
            <legend>{activeLabel} weights</legend>
            <p className="field-hint form-full">Each strategy keeps its own weights, and they always add up to 1: change one and the others adjust to keep their proportions. Each weight is that criterion's share of the total score, so 0.3 is 30%.</p>
            {shown && rankedFirstCriteria(shown).map(({ key, label }) => (
              <p key={key} className="field-hint form-full">{activeLabel} already ranks by {label.toLowerCase()}, so it is not weighted again here.</p>
            ))}
            {shown && weightedCriteria(shown).map(({ key, label }) => (
              <Field
                key={key}
                id={`weight-${key}`}
                label={label}
                error={errors[`weight-${key}`]}
                hint={total !== null ? `${Math.round(Number(shown?.[key]) * 100)}% of the total score` : undefined}
              >
                <input
                  id={`weight-${key}`}
                  type="number"
                  min="0"
                  max="1"
                  step="0.05"
                  inputMode="decimal"
                  value={shown?.[key]}
                  onChange={event => changeWeight(key, event.target.value)}
                />
              </Field>
            ))}
          </fieldset>
          {total !== null && (
            <p className="field-hint" role="status">
              Total: <strong>{formatTotal(total)}</strong>{addsUpToOne(total) ? ' (adds up to 1)' : ' (must add up to 1)'}
            </p>
          )}
          {errors.weights && (
            <p className="field-error" role="alert">{errors.weights}</p>
          )}
          <ActionNotice error={action.error} success={action.success} />
          <FormActions dirty={dirty} pending={action.pending}>
            <Button
              type="submit"
              variant="primary"
              disabled={action.pending}
            >{action.pending ? 'Saving…' : 'Save matching settings'}</Button>
          </FormActions>
        </form>
      )}
      {exit.confirmation}
    </WindowPage>
  )
}
