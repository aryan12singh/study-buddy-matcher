import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import { useAuth } from '../auth/useAuth'
import { api } from '../api/client'
import { useResource } from '../api/useResource'
import Button from './Button'
import Avatar from './Avatar'
import PixelIcon from './PixelIcon'
import RouteNotice from './RouteNotice'

export default function AppShell() {
  const { account, logout } = useAuth()
  const { pathname } = useLocation()
  const notifications = useResource(
    'shell-unread',
    async signal => (await api.get<{ count: number }>('/notifications/unread-count', { signal })).data,
    account?.role === 'STUDENT'
  )
  useEffect(() => {
    document.querySelector<HTMLElement>('#main-content')?.focus()
  }, [pathname])
  return (
    <div className="desktop">
      <a href="#main-content" className="skip-link">Skip to content</a>
      <header className="desktop-menubar">
        <NavLink to="/app" className="desktop-brand"><PixelIcon kind="brand" /><span>Study Buddy Matcher</span></NavLink>
        <div className="session-menu">
          <Avatar name={account?.name || account?.email || 'Account'} small />
          <span className="session-identity" title={account?.email}><strong>{account?.name || (account?.role === 'ADMIN' ? 'Administrator' : 'Student')}</strong><span>{account?.role === 'ADMIN' ? 'Admin account' : 'Student account'}</span></span>
          <Button onClick={logout}>Log out</Button>
        </div>
      </header>
      <div className="desktop-layout">
        <nav className="desktop-navigation" aria-label="Main navigation">
          {account?.role === 'STUDENT' ? (
            <>
              <NavLink to="/home"><PixelIcon kind="home" /><span>Home</span></NavLink>
              <NavLink to="/connections"><PixelIcon kind="connections" /><span>Connections</span></NavLink>
              <NavLink to="/groups"><PixelIcon kind="groups" /><span>Study groups</span></NavLink>
              <NavLink to="/notifications"><PixelIcon kind="notifications" /><span>Notifications</span>{notifications.data && notifications.data.count > 0 && (
                <span className="unread-count" aria-label={`${notifications.data.count} unread notifications`}>{notifications.data.count}</span>
              )}</NavLink>
              <NavLink to={`/students/${account.id}`}><PixelIcon kind="profile" /><span>My study profile</span></NavLink>
              {notifications.error && (
                <span className="nav-error" role="status">Unread count unavailable. <button type="button" onClick={() => notifications.reload()}>Retry</button></span>
              )}
            </>
          ) : (
            <>
              <NavLink to="/admin/users"><PixelIcon kind="accounts" /><span>User accounts</span></NavLink>
              <NavLink to="/admin/matching"><PixelIcon kind="search" /><span>Matching settings</span></NavLink>
            </>
          )}
        </nav>
        <main
          id="main-content"
          className="desktop-main"
          tabIndex={-1}
        >
          <RouteNotice />
          <Outlet />
        </main>
      </div>
      <footer className="desktop-footer">Study together, at your own pace. <span>Weekly schedules use Singapore time.</span></footer>
    </div>
  )
}
