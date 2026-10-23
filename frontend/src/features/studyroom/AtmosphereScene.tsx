import type { WeatherCondition } from './weatherTheme'

/** The decorative study-window drawing; weather and daylight only change its colours and effects. */
export default function AtmosphereScene({ condition, night, className = '' }: {
  condition: WeatherCondition; night: boolean; className?: string
}) {
  return <div className={`atmosphere-window atmosphere-scene ${className}`} data-weather={condition.toLowerCase()} data-night={night} aria-hidden="true">
    <div className="atmosphere-sky"><i className="atmosphere-orb" /><i className="atmosphere-cloud cloud-one" /><i className="atmosphere-cloud cloud-two" /><div className="atmosphere-rain" /><div className="atmosphere-fog" /><div className="atmosphere-stars" /></div>
    <div className="atmosphere-crossbar" /><div className="atmosphere-desk"><i className="atmosphere-books" /><i className="atmosphere-cup" /><i className="atmosphere-plant" /></div>
    <span className="atmosphere-caption">{night ? 'Night study' : 'Day study'}</span>
  </div>
}
