import { useEffect, useState } from 'react'
import axios from 'axios'
import { Link, useSearchParams } from 'react-router-dom'
import { getCourses } from '../../shared/api/courses'
import type { Course } from '../../shared/api/types'
import { useResource } from '../../shared/api/useResource'
import StatePanel from '../../shared/components/StatePanel'
import WindowPage from '../../shared/components/WindowPage'
import { getMyProfile } from './api'
import type { MyProfile } from './api'
import ProfileEditor from './ProfileEditor'
import { isProfileTab } from './profileForm'

/** Keeps the owner's mounted editor through recoverable reads, until access is lost or purged. */
export default function OwnerProfilePage({ id }: { id: number }) {
  const [params] = useSearchParams()
  const welcome = params.get('welcome') === '1'
  const tab = params.get('tab')
  const [editor, setEditor] = useState<{ profile: MyProfile; courses: Course[] }>()

  async function readForEditor<T>(loader: (signal: AbortSignal) => Promise<T>, signal: AbortSignal) {
    try {
      return await loader(signal)
    } catch (failure) {
      if (!signal.aborted && axios.isAxiosError(failure)
        && [401, 403, 404].includes(failure.response?.status ?? 0)) setEditor(undefined)
      throw failure
    }
  }
  // No background refresh: a periodic reload must never overwrite what the student is typing.
  const profile = useResource(`my-profile:${id}`, async signal => {
    const loaded = await readForEditor(getMyProfile, signal)
    if (loaded.id !== id) {
      if (!signal.aborted) setEditor(undefined)
      throw new Error('This profile is no longer available to this account.')
    }
    return loaded
  }, true, false)
  const courses = useResource(`courses:${id}`, signal => readForEditor(getCourses, signal), true, false)

  useEffect(() => {
    if (profile.data && courses.data) setEditor({ profile: profile.data, courses: courses.data })
  }, [profile.data, courses.data])
  useEffect(() => {
    const onChange = (event: Event) => {
      if ((event as CustomEvent<{ purge: boolean }>).detail?.purge) setEditor(undefined)
    }
    window.addEventListener('resources-changed', onChange)
    return () => window.removeEventListener('resources-changed', onChange)
  }, [])
  return (
    <WindowPage
      title={welcome ? 'Complete your study profile' : 'Edit my study profile'}
      description={welcome
        ? 'Add your courses, preferences and weekly times so matching can find study buddies for you.'
        : 'Keep your details and preferences current so matching can find compatible study buddies.'}
      actions={<Link className="retro-button" to={`/students/${id}`}>Back to my profile</Link>}
    >
      <StatePanel
        loading={(profile.loading && !profile.data) || (courses.loading && !courses.data)}
        error={profile.error || courses.error}
        onRetry={() => {
          profile.reload()
          courses.reload()
        }}
      />
      {/* A recoverable read error must not unmount the independently saved drafts. */}
      {editor && (
        <ProfileEditor
          profile={editor.profile}
          courses={editor.courses}
          initialTab={isProfileTab(tab) ? tab : 'about'}
          welcome={welcome}
        />
      )}
    </WindowPage>
  )
}
