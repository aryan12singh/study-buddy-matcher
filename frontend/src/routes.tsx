import { createRoutesFromElements, Link, Route } from 'react-router-dom'
import LoginPage from './features/auth/LoginPage'
import RegisterPage from './features/auth/RegisterPage'
import LandingPage from './features/landing/LandingPage'
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
import StudyRoomPage from './features/studyroom/StudyRoomPage'
import AdminUsersPage from './features/admin/AdminUsersPage'
import AdminUserDetailPage from './features/admin/AdminUserDetailPage'
import AdminUserFormPage from './features/admin/AdminUserFormPage'

export const appRoutes = createRoutesFromElements(
    <>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<RequireRole />}>
        <Route element={<AppShell />}>
          <Route path="/app" element={<AccountHome />} />
          <Route element={<RequireRole role="STUDENT" />}>
            <Route path="/connections" element={<ConnectionsPage />} />
            <Route path="/students/:id" element={<StudentProfilePage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/groups" element={<GroupsPage />} />
            <Route path="/groups/new" element={<GroupFormPage />} />
            <Route path="/groups/:id" element={<GroupDetailPage />} />
            <Route path="/groups/:id/edit" element={<GroupFormPage />} />
            <Route path="/groups/:id/manage" element={<GroupManagePage />} />
            <Route path="/groups/:id/room" element={<StudyRoomPage />} />
          </Route>
          <Route element={<RequireRole role="ADMIN" />}>
            <Route path="/admin/users" element={<AdminUsersPage />} />
            <Route path="/admin/users/new" element={<AdminUserFormPage />} />
            <Route path="/admin/users/:id" element={<AdminUserDetailPage />} />
            <Route path="/admin/users/:id/edit" element={<AdminUserFormPage />} />
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
