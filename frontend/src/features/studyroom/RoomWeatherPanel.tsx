import { useEffect, useState } from 'react'
import { api } from '../../shared/api/client'
import { useResource } from '../../shared/api/useResource'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import AtmosphereScene from './AtmosphereScene'
import { campusNight, readDaylightChoice, readSceneChoice, readWeatherChoice, roomScenes, sceneLabels, weatherConditions, weatherLabels } from './weatherTheme'
import type { Atmosphere, AtmosphereStore, DaylightChoice, RoomScene, RoomWeather, WeatherChoice } from './weatherTheme'
import './weatherTheme.css'

async function getWeather(signal: AbortSignal): Promise<RoomWeather> {
  return (await api.get('/weather', { signal })).data
}

export default function RoomWeatherPanel({ liveEnabled = false, store = 'study-room', onChange }: {
  liveEnabled?: boolean; store?: AtmosphereStore; onChange?: (atmosphere: Atmosphere) => void
}) {
  const [scene, setScene] = useState<RoomScene>(() => readSceneChoice(store))
  const [choice, setChoice] = useState<WeatherChoice>(() => readWeatherChoice(store))
  const [daylight, setDaylight] = useState<DaylightChoice>(() => readDaylightChoice(store))
  const [now, setNow] = useState(() => new Date())
  const weather = useResource('room-weather', getWeather, liveEnabled && choice === 'AUTO')
  const live = weather.data?.available ? weather.data : undefined
  const condition = choice === 'AUTO' ? live?.condition ?? 'CLEAR' : choice
  const night = daylight === 'DAY' ? false : daylight === 'NIGHT' ? true
    : choice === 'AUTO' && live ? !live.isDay : campusNight(now)
  useEffect(() => { onChange?.({ scene, condition, night }) }, [scene, condition, night, onChange])
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 60000)
    return () => window.clearInterval(timer)
  }, [])
  useEffect(() => {
    try { localStorage.setItem(`${store}-atmosphere`, choice) } catch { /* Storage is optional. */ }
  }, [store, choice])
  useEffect(() => {
    try { localStorage.setItem(`${store}-daylight`, daylight) } catch { /* Storage is optional. */ }
  }, [store, daylight])
  useEffect(() => {
    try { localStorage.setItem(`${store}-scene`, scene) } catch { /* Storage is optional. */ }
  }, [store, scene])
  return <section className="detail-panel room-atmosphere" aria-label="Room atmosphere">
    <AtmosphereScene scene={scene} condition={condition} night={night} />
    <div className="atmosphere-controls">
      <h2>Room atmosphere</h2>
      <p>Make this corner your own. Your choice changes only this browser.</p>
      <Field id="room-scene" label="Room">
        <select id="room-scene" value={scene} onChange={event => setScene(event.target.value as RoomScene)}>
          {roomScenes.map(value => <option key={value} value={value}>{sceneLabels[value]}</option>)}
        </select>
      </Field>
      <Field id="room-atmosphere" label="Window view">
        <select id="room-atmosphere" value={choice} onChange={event => setChoice(event.target.value as WeatherChoice)}>
          <option value="AUTO">{liveEnabled ? 'Follow campus weather' : 'Follow campus daylight'}</option>
          {weatherConditions.map(value => <option key={value} value={value}>{weatherLabels[value]}</option>)}
        </select>
      </Field>
      <Field id="room-daylight" label="Time of day">
        <select id="room-daylight" value={daylight} onChange={event => setDaylight(event.target.value as DaylightChoice)}>
          <option value="AUTO">Follow campus time</option>
          <option value="DAY">Day</option>
          <option value="NIGHT">Night</option>
        </select>
      </Field>
      {choice === 'AUTO' && liveEnabled ? <div className="atmosphere-status" role="status">
        {weather.loading ? <p>Checking campus weather…</p> : live ? <p>
          {live.location}: {weatherLabels[live.condition]}{live.temperatureCelsius !== null ? `, ${Math.round(live.temperatureCelsius)} °C` : ''}.
          {live.stale ? ' Last known conditions; the weather service is unavailable.' : ' Current campus conditions.'}
          {live.observedAt && <span className="muted"> Updated {new Intl.DateTimeFormat('en-SG', { timeZone: 'Asia/Singapore', hour: '2-digit', minute: '2-digit' }).format(new Date(live.observedAt))} SGT.</span>}
        </p> : <p>Live weather is unavailable. The window follows campus daylight; you can select any atmosphere.</p>}
        <Button disabled={weather.loading} onClick={() => weather.reload()}>Refresh weather</Button>
      </div> : <p className="muted">{choice !== 'AUTO' ? 'Selected atmosphere; this is not a weather forecast.' : daylight === 'AUTO' ? 'Daylight follows Singapore time.' : 'Showing your chosen time of day.'}</p>}
      {/* Open-Meteo's data is CC BY 4.0, which requires this credit wherever it is shown. */}
      {liveEnabled && <p className="field-hint">Weather by <a href="https://open-meteo.com/" target="_blank" rel="noreferrer">Open-Meteo</a> (CC BY 4.0)</p>}
    </div>
  </section>
}
