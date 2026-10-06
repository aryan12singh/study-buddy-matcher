import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import { useAuth } from '../auth/useAuth'
import { api } from '../api/client'
import { useResource } from '../api/useResource'
import Button from './Button'

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
        <NavLink to="/app" className="desktop-brand"><span aria-hidden="true">▤</span> Study Buddy Matcher</NavLink>
        <div className="session-menu">
          <span>{account?.name || account?.email}</span>
          <Button onClick={logout}>Log out</Button>
        </div>
      </header>
      <div className="desktop-layout">
        <nav className="desktop-navigation" aria-label="Main navigation">
          {account?.role === 'STUDENT' ? (
            <>
              <NavLink to="/connections"><span className="nav-symbol" aria-hidden="true">◇</span>Connections</NavLink>
              <NavLink to="/groups"><span className="nav-symbol" aria-hidden="true">▦</span>Study groups</NavLink>
              <NavLink to="/notifications"><span className="nav-symbol" aria-hidden="true">♧</span>Notifications{notifications.data && notifications.data.count > 0 && (
                <span className="unread-count" aria-label={`${notifications.data.count} unread notifications`}>{notifications.data.count}</span>
              )}</NavLink>
              <NavLink to={`/students/${account.id}`}><span className="nav-symbol" aria-hidden="true">▣</span>My study profile</NavLink>
              {notifications.error && (
                <span className="nav-error" role="status">Unread count unavailable. <button type="button" onClick={() => notifications.reload()}>Retry</button></span>
              )}
            </>
          ) : (
            <NavLink to="/admin/users"><span className="nav-symbol" aria-hidden="true">▦</span>User accounts</NavLink>
          )}
        </nav>
        <main
          id="main-content"
          className="desktop-main"
          tabIndex={-1}
        >
          <Outlet />
        </main>
      </div>
      <footer className="desktop-footer">Study together, at your own pace. <span>Weekly schedules use Singapore time.</span></footer>
    </div>
  )
}
