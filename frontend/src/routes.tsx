import { Suspense } from 'react'
import { createRoutesFromElements, Link, Outlet, Route } from 'react-router-dom'
import LoginPage from './features/auth/LoginPage'
import RegisterPage from './features/auth/RegisterPage'
import LandingPage from './features/landing/LandingPage'
import EditProfilePage from './features/profile/EditProfilePage'
import HomePage from './features/home/HomePage'
import AccountHome from './shared/auth/AccountHome'
import RequireRole from './shared/auth/RequireRole'
import AppShell from './shared/components/AppShell'
import WindowPage from './shared/components/WindowPage'
import ConnectionsPage from './features/connections/ConnectionsPage'
import StudentProfilePage from './features/students/StudentProfilePage'
import NotificationsPage from './features/notifications/NotificationsPage'
import GroupsPage from './features/groups/GroupsPage'
import GroupDetailPage from './features/groups/GroupDetailPage'
import GroupFormPage from './features/groups/GroupFormPage'
import GroupManagePage from './features/groups/GroupManagePage'
import AdminUsersPage from './features/admin/AdminUsersPage'
import AdminUserDetailPage from './features/admin/AdminUserDetailPage'
import AdminUserFormPage from './features/admin/AdminUserFormPage'
import MatchingSettingsPage from './features/matching/MatchingSettingsPage'
import FindBuddiesPage from './features/matching/FindBuddiesPage'
import MusicPage from './features/music/MusicPage'
import StatePanel from './shared/components/StatePanel'
import { SessionDetailPage, SessionFormPage, SessionsPage, StudyRoomPage } from './lazyPages'

export const appRoutes = createRoutesFromElements(
    <>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<RequireRole />}>
        <Route element={<AppShell />}>
          <Route path="/app" element={<AccountHome />} />
          <Route element={<RequireRole role="STUDENT" />}>
            <Route path="/home" element={<HomePage />} />
            <Route path="/find-buddies" element={<FindBuddiesPage />} />
            <Route path="/connections" element={<ConnectionsPage />} />
            <Route path="/students/:id" element={<StudentProfilePage />} />
            <Route path="/students/:id/edit" element={<EditProfilePage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/music" element={<MusicPage />} />
            <Route path="/groups" element={<GroupsPage />} />
            <Route path="/groups/new" element={<GroupFormPage />} />
            <Route path="/groups/:id" element={<GroupDetailPage />} />
            <Route path="/groups/:id/edit" element={<GroupFormPage />} />
            <Route path="/groups/:id/manage" element={<GroupManagePage />} />
            <Route element={<Suspense fallback={<StatePanel loading />}><Outlet /></Suspense>}>
              <Route path="/groups/:id/room" element={<StudyRoomPage />} />
              <Route path="/sessions" element={<SessionsPage />} />
              <Route path="/sessions/:id" element={<SessionDetailPage />} />
              <Route path="/sessions/:id/edit" element={<SessionFormPage />} />
              <Route path="/groups/:id/sessions" element={<SessionsPage kind="GROUP" />} />
              <Route path="/groups/:id/sessions/new" element={<SessionFormPage kind="GROUP" />} />
              <Route path="/connections/:id/sessions" element={<SessionsPage kind="BUDDY" />} />
              <Route path="/connections/:id/sessions/new" element={<SessionFormPage kind="BUDDY" />} />
            </Route>
          </Route>
          <Route element={<RequireRole role="ADMIN" />}>
            <Route path="/admin/users" element={<AdminUsersPage />} />
            <Route path="/admin/users/new" element={<AdminUserFormPage />} />
            <Route path="/admin/users/:id" element={<AdminUserDetailPage />} />
            <Route path="/admin/users/:id/edit" element={<AdminUserFormPage />} />
            <Route path="/admin/matching" element={<MatchingSettingsPage />} />
          </Route>
          <Route
            path="*"
            element={
              <WindowPage title="Page unavailable">
                <p>This link does not match an available page.</p>
                <Link to="/app">Return to your account</Link>
              </WindowPage>
            }
          />
        </Route>
      </Route>
    </>
  )
