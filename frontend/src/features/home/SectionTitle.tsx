import { Link } from 'react-router-dom'
import PixelIcon from '../../shared/components/PixelIcon'
import type { PixelIconKind } from '../../shared/components/PixelIcon'

export default function SectionTitle({ id, icon, title, count, link }: {
  id: string
  icon: PixelIconKind
  title: string
  count?: string
  link?: { to: string; label: string }
}) {
  return (
    <div className="home-section-title">
      <h2 id={id}><PixelIcon kind={icon} />{title}{count && <span className="home-count">{count}</span>}</h2>
      {link && <Link className="home-more" to={link.to}>{link.label}</Link>}
    </div>
  )
}
