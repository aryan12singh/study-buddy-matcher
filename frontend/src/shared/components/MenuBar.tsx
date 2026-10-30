import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import PixelIcon from './PixelIcon'

/** The skip link and the menu bar across the top of every screen: the brand on the left, the screen's own tools on the right. */
export default function MenuBar({ brandTo, children }: { brandTo: string; children: ReactNode }) {
  return (
    <>
      <a href="#main-content" className="skip-link">Skip to content</a>
      <header className="desktop-menubar">
        <Link to={brandTo} className="desktop-brand"><PixelIcon kind="brand" /><span>Study Buddy Matcher</span></Link>
        {children}
      </header>
    </>
  )
}
