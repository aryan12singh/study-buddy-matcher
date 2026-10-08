import { useSearchParams } from 'react-router-dom'
import { getCourses } from '../../shared/api/courses'
import { useResource } from '../../shared/api/useResource'
import StatePanel from '../../shared/components/StatePanel'
import WindowPage from '../../shared/components/WindowPage'
import { getMyProfile } from './api'
import ProfileEditor from './ProfileEditor'
import { isProfileTab } from './profileForm'

/** Loads the signed-in student's profile and the course list, then hands both to the editor. */
export default function EditProfilePage() {
  const [params] = useSearchParams()
  const welcome = params.get('welcome') === '1'
  const tab = params.get('tab')
  // No background refresh: a periodic reload must never overwrite what the student is typing.
  const profile = useResource('my-profile', getMyProfile, true, false)
  const courses = useResource('courses', getCourses, true, false)
  return (
    <WindowPage
      title={welcome ? 'Complete your study profile' : 'Edit my study profile'}
      description={welcome
        ? 'Add your courses, preferences and weekly times so matching can find study buddies for you.'
        : 'Keep your details and preferences current so matching can find compatible study buddies.'}
    >
      <StatePanel
        loading={(profile.loading && !profile.data) || (courses.loading && !courses.data)}
        error={profile.error || courses.error}
        onRetry={() => {
          profile.reload()
          courses.reload()
        }}
      />
      {profile.data && courses.data && (
        <ProfileEditor
          profile={profile.data}
          courses={courses.data}
          initialTab={isProfileTab(tab) ? tab : 'about'}
          welcome={welcome}
        />
      )}
    </WindowPage>
  )
}
