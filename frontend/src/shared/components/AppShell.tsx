import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../auth/useAuth'
import { useResource } from '../api/useResource'
import Button from './Button'
import Avatar from './Avatar'
import PixelIcon from './PixelIcon'
import NavSection from './NavSection'
import MenuBar from './MenuBar'
import RouteNotice from './RouteNotice'
import AssistantLauncher from '../../features/assistant/AssistantLauncher'
import AchievementNavIcon from '../../features/achievements/AchievementNavIcon'
import { getUnreadCount } from '../../features/notifications/api'

export default function AppShell() {
  const { account, logout } = useAuth()
  const { pathname } = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuButton = useRef<HTMLButtonElement>(null)
  const navigation = useRef<HTMLElement>(null)
  const viewingStudyRoom = pathname === '/study-room' || /^\/groups\/[^/]+\/room\/?$/.test(pathname)
  // The forest is reached from the achievements page, so it keeps that sidebar entry highlighted.
  const viewingAchievements = pathname === '/achievements' || pathname === '/forest'
  const notifications = useResource('shell-unread', getUnreadCount, account?.role === 'STUDENT')
  const unread = notifications.data ?? 0
  useEffect(() => {
    document.querySelector<HTMLElement>('#main-content')?.focus()
  }, [pathname])
  // The narrow-screen menu moves focus into its links when it opens, and Escape closes it back to its button.
  useEffect(() => {
    if (!menuOpen) return
    navigation.current?.querySelector<HTMLElement>('a')?.focus()
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return
      setMenuOpen(false)
      menuButton.current?.focus()
    }
    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [menuOpen])
  return (
    <div className="desktop">
      <MenuBar brandTo="/app">
        <div className="session-menu">
          {account?.role === 'STUDENT' && (
            <div className="menubar-tools">
              <NavLink to="/notifications" className="retro-button menubar-icon-button menubar-notifications" title="Notifications">
                <PixelIcon kind="notifications" /><span className="visually-hidden">Notifications</span>
                {unread > 0 && (
                  <><span className="unread-count" aria-hidden="true">{unread}</span><span className="visually-hidden">, {unread} unread</span></>
                )}
              </NavLink>
              {notifications.error && (
                <span className="nav-error" role="status">Unread count unavailable. <button type="button" onClick={() => notifications.reload()}>Retry</button></span>
              )}
              <NavLink to="/help" className="retro-button menubar-icon-button menubar-help" title="Help">
                <PixelIcon kind="help" /><span className="visually-hidden">Help</span>
              </NavLink>
            </div>
          )}
          <div className="menubar-account">
            <Avatar name={account?.name || account?.email || 'Account'} small />
            <span className="session-identity" title={account?.email}><strong>{account?.name || (account?.role === 'ADMIN' ? 'Administrator' : 'Student')}</strong><span>{account?.role === 'ADMIN' ? 'Admin account' : 'Student account'}</span></span>
            <Button onClick={logout}>Log out</Button>
          </div>
          <button type="button" ref={menuButton} className="retro-button menubar-icon-button menubar-menu" aria-expanded={menuOpen}
            aria-controls="main-navigation" onClick={() => setMenuOpen(open => !open)}>
            <PixelIcon kind="menu" /><span className="visually-hidden">Menu</span>
          </button>
        </div>
      </MenuBar>
      <div className="desktop-layout">
        <nav id="main-navigation" ref={navigation} className={`desktop-navigation${menuOpen ? ' is-open' : ''}`} aria-label="Main navigation"
          onClick={event => { if ((event.target as Element).closest('a')) setMenuOpen(false) }}>
          {account?.role === 'STUDENT' ? (
            <>
              <NavLink to="/home"><PixelIcon kind="home" /><span>Home</span></NavLink>
              <NavSection title="Connect">
                <NavLink to="/find-buddies"><PixelIcon kind="search" /><span>Find buddies</span></NavLink>
                <NavLink to="/connections"><PixelIcon kind="connections" /><span>My connections</span></NavLink>
                <NavLink to="/groups" className={({ isActive }) => isActive && !viewingStudyRoom ? 'active' : undefined}
                  aria-current={viewingStudyRoom ? false : undefined}><PixelIcon kind="groups" /><span>Study groups</span></NavLink>
              </NavSection>
              <NavSection title="Study">
                <NavLink to="/sessions"><PixelIcon kind="clock" /><span>Sessions</span></NavLink>
                <Link to="/study-room" className={viewingStudyRoom ? 'active' : undefined}
                  aria-current={viewingStudyRoom ? 'page' : undefined}><PixelIcon kind="brand" /><span>Study Room</span></Link>
                <NavLink to="/study-plan"><PixelIcon kind="robot" /><span>Study plans</span></NavLink>
              </NavSection>
              <NavSection title="You">
                <NavLink to={`/students/${account.id}`}><PixelIcon kind="profile" /><span>My study profile</span></NavLink>
                <Link to="/achievements" className={viewingAchievements ? 'active' : undefined}
                  aria-current={viewingAchievements ? 'page' : undefined}><AchievementNavIcon /><span>Achievements</span></Link>
              </NavSection>
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
      <footer className="desktop-footer">Study together, at your own pace.</footer>
      {account?.role === 'STUDENT' && <AssistantLauncher />}
    </div>
  )
}
