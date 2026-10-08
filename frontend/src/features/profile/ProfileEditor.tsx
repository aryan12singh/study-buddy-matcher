import { useState } from 'react'
import type { Course, WeeklySlot } from '../../shared/api/types'
import { useAction } from '../../shared/api/useAction'
import ActionNotice from '../../shared/components/ActionNotice'
import Button from '../../shared/components/Button'
import FormActions from '../../shared/components/FormActions'
import StatePanel from '../../shared/components/StatePanel'
import TabBar from '../../shared/components/TabBar'
import WeeklySlotEditor from '../../shared/components/WeeklySlotEditor'
import { useFormExit } from '../../shared/components/useFormExit'
import { replaceMyAvailability, updateMyProfile } from './api'
import type { MyProfile } from './api'
import AboutYouFields from './AboutYouFields'
import ProfileChecklist from './ProfileChecklist'
import StudyPreferencesFields from './StudyPreferencesFields'
import { ABOUT_FIELDS, PROFILE_TABS, sameSlots, toForm, toRequest, validateAvailability, validateProfile } from './profileForm'
import type { ProfileTab } from './profileForm'

/**
 * Edits the profile and preferences (one save) and the weekly availability
 * (its own save). Tabs are local state so switching never counts as leaving
 * the page while there are unsaved edits.
 */
export default function ProfileEditor({ profile, courses, initialTab, welcome }: {
  profile: MyProfile
  courses: Course[]
  initialTab: ProfileTab
  welcome: boolean
}) {
  const [tab, setTab] = useState<ProfileTab>(initialTab)
  const profileAction = useAction()
  const availabilityAction = useAction()
  const [saved, setSaved] = useState(profile)
  const [form, setForm] = useState(() => toForm(profile))
  const [savedSlots, setSavedSlots] = useState<WeeklySlot[]>(profile.availability)
  const [slots, setSlots] = useState<WeeklySlot[]>(profile.availability)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [slotError, setSlotError] = useState<string>()

  const profileDirty = JSON.stringify(form) !== JSON.stringify(toForm(saved))
  const slotsDirty = !sameSlots(slots, savedSlots)
  const pending = profileAction.pending || availabilityAction.pending
  const exit = useFormExit(profileDirty || slotsDirty, pending)
  const fieldErrors = { ...profileAction.errors, ...errors }
  const attention: Record<ProfileTab, boolean> = {
    about: ABOUT_FIELDS.some(field => fieldErrors[field]),
    preferences: Object.keys(fieldErrors).some(field => !ABOUT_FIELDS.includes(field)),
    availability: Boolean(slotError || availabilityAction.errors.availability)
  }
  const tabOptions = PROFILE_TABS.map(option => ({
    value: option.value,
    label: attention[option.value] ? `${option.label} · needs attention` : option.label
  }))

  async function saveProfile(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid = validateProfile(form)
    setErrors(invalid)
    if (Object.keys(invalid).length) {
      setTab(Object.keys(invalid).some(field => ABOUT_FIELDS.includes(field)) ? 'about' : 'preferences')
      return
    }
    const result = await profileAction.run(() => updateMyProfile(toRequest(form)), 'Profile saved.')
    if (result.ok) {
      setSaved(result.value)
      setForm(toForm(result.value))
    }
  }

  async function saveAvailability(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid = validateAvailability(slots)
    setSlotError(invalid)
    if (invalid) return
    const result = await availabilityAction.run(() => replaceMyAvailability(slots), 'Weekly availability saved.')
    if (result.ok) {
      setSavedSlots(result.value)
      setSlots(result.value)
    }
  }

  return (
    <>
      {welcome && (
        <ProfileChecklist
          about
          preferences={saved.coursesTaken.length > 0 && Boolean(saved.preferredStudyMode)}
          availability={savedSlots.length > 0}
        />
      )}
      <TabBar value={tab} options={tabOptions} onChange={setTab} label="Profile sections" />
      {tab !== 'availability' ? (
        <form onSubmit={saveProfile} noValidate className="stack-form wide-form">
          {tab === 'about'
            ? <AboutYouFields value={form} onChange={setForm} errors={fieldErrors} disabled={profileAction.pending} />
            : (
              <StudyPreferencesFields
                value={form}
                onChange={setForm}
                errors={fieldErrors}
                courses={courses}
                largestGroup={saved.groupSizeMax}
                disabled={profileAction.pending}
              />
            )}
          <ActionNotice error={profileAction.error} success={profileDirty ? undefined : profileAction.success} />
          <FormActions dirty={profileDirty} pending={profileAction.pending}>
            <Button variant="primary" type="submit" disabled={pending}>
              {profileAction.pending ? 'Saving profile…' : 'Save profile'}
            </Button>
          </FormActions>
        </form>
      ) : (
        <form onSubmit={saveAvailability} noValidate className="stack-form wide-form">
          <WeeklySlotEditor
            empty={(
              <StatePanel
                empty
                emptyKind="clock"
                emptyTitle="No weekly times yet"
                emptyMessage="Without availability, matching cannot find timetable overlap with other students, so your matches will rank lower."
              />
            )}
            value={slots}
            onChange={next => {
              setSlots(next)
              setSlotError(undefined)
            }}
            error={slotError || availabilityAction.errors.availability}
            disabled={availabilityAction.pending}
          />
          <ActionNotice error={availabilityAction.error} success={slotsDirty ? undefined : availabilityAction.success} />
          <FormActions dirty={slotsDirty} pending={availabilityAction.pending}>
            <Button variant="primary" type="submit" disabled={pending}>
              {availabilityAction.pending ? 'Saving availability…' : 'Save availability'}
            </Button>
          </FormActions>
        </form>
      )}
      {exit.confirmation}
    </>
  )
}
