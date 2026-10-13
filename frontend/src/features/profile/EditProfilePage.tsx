import { Link, Navigate, useParams, useSearchParams } from 'react-router-dom'
import { getCourses } from '../../shared/api/courses'
import { useResource } from '../../shared/api/useResource'
import { useAuth } from '../../shared/auth/useAuth'
import StatePanel from '../../shared/components/StatePanel'
import WindowPage from '../../shared/components/WindowPage'
import { getMyProfile } from './api'
import ProfileEditor from './ProfileEditor'
import { isProfileTab } from './profileForm'

/**
 * Edits the signed-in student's own profile at /students/:id/edit. Another
 * student's edit URL goes back to their read-only profile; the server only
 * ever edits the signed-in account, so this is navigation, not security.
 */
export default function EditProfilePage() {
  const { id } = useParams()
  const { account } = useAuth()
  const [params] = useSearchParams()
  const isOwn = account?.id === Number(id)
  const welcome = params.get('welcome') === '1'
  const tab = params.get('tab')
  // No background refresh: a periodic reload must never overwrite what the student is typing.
  const profile = useResource('my-profile', getMyProfile, isOwn, false)
  const courses = useResource('courses', getCourses, isOwn, false)
  if (!isOwn) return <Navigate to={`/students/${id}`} replace />
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
