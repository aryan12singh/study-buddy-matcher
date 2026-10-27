import type { RoomScene, WeatherCondition } from './weatherTheme'

/** The decorative room drawing. Every scene shares one sky; weather and daylight only change colours and effects. */
export default function AtmosphereScene({ scene, condition, night, className = '' }: {
  scene: RoomScene; condition: WeatherCondition; night: boolean; className?: string
}) {
  return <div className={`atmosphere-window atmosphere-scene ${className}`} data-scene={scene.toLowerCase()}
    data-weather={condition.toLowerCase()} data-night={night} aria-hidden="true">
    <div className="atmosphere-sky"><i className="atmosphere-orb" /><i className="atmosphere-cloud cloud-one" /><i className="atmosphere-cloud cloud-two" /><div className="atmosphere-rain" /><div className="atmosphere-fog" /><div className="atmosphere-stars" /></div>
    <div className="atmosphere-room">
      {scene === 'GARDEN' ? <><i className="atmosphere-hills" /><i className="atmosphere-meadow" /></> : <>
        {scene === 'LIBRARY' && <><i className="atmosphere-shelf shelf-left" /><i className="atmosphere-shelf shelf-right" /></>}
        {scene === 'CAFE' && <><i className="atmosphere-awning" /><i className="atmosphere-pendant pendant-left" /><i className="atmosphere-pendant pendant-right" /></>}
        <div className="atmosphere-crossbar" />
        <div className="atmosphere-desk">
          {scene !== 'CAFE' && <i className="atmosphere-books" />}
          <i className="atmosphere-cup" />
          {scene === 'DESK' && <i className="atmosphere-plant" />}
          {scene === 'LIBRARY' && <i className="atmosphere-lamp" />}
          {scene === 'CAFE' && <i className="atmosphere-cake" />}
        </div>
      </>}
    </div>
  </div>
}
