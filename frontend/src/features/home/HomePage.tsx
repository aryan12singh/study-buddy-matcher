import { getMatchRequests } from '../connections/api'
import { getMyGroups } from '../groups/api'
import { getNotifications } from '../notifications/api'
import { getMyProfile } from '../profile/api'
import { useResource } from '../../shared/api/useResource'
import { useAuth } from '../../shared/auth/useAuth'
import WindowPage from '../../shared/components/WindowPage'
import ActivityFeed from './ActivityFeed'
import ActivityStrip from './ActivityStrip'
import FindBuddiesCard from './FindBuddiesCard'
import GroupCards from './GroupCards'
import { todayLabel } from './homeTime'
import { nextStep, setupProgress } from './nextStep'
import NextStepCard from './NextStepCard'
import ProfileStatusBadge from './ProfileStatusBadge'
import RequestCards from './RequestCards'
import WeekGlance from './WeekGlance'

/**
 * A student's overview. Each endpoint is loaded once here and handed to the
 * sections, and every section shows its own loading, error and empty state,
 * so one failed request never blanks the rest of the page.
 */
export default function HomePage() {
  const { account } = useAuth()
  const profile = useResource('home-profile', getMyProfile)
  const requests = useResource('home-incoming', signal => getMatchRequests('incoming', signal))
  const groups = useResource('home-groups', getMyGroups)
  const notifications = useResource('home-notifications', signal => getNotifications('ALL', signal))
  const studentId = account?.id ?? 0
  const pending = requests.data?.filter(request => request.status === 'PENDING') ?? []
  const setup = profile.data && setupProgress(profile.data)
  const settingUp = Boolean(setup && setup.done < setup.total)
  const step = nextStep(studentId, profile.data, pending)
  const name = account?.name ?? ''
  return (
    <WindowPage
      title={settingUp ? `Welcome, ${name}` : `Welcome back, ${name}`}
      description={`${todayLabel()} · ${settingUp ? 'A few quick steps and matching can start working for you.' : 'Your study profile is up to date.'}`}
      actions={<ProfileStatusBadge profile={profile.data} studentId={studentId} />}
    >
      <div className="home-layout">
        {step && <NextStepCard step={step} />}
        <ActivityStrip />
        <RequestCards requests={requests} pending={pending} />
        <WeekGlance profile={profile} studentId={studentId} />
        <GroupCards groups={groups} />
        <FindBuddiesCard />
        <ActivityFeed notifications={notifications} />
      </div>
    </WindowPage>
  )
}
