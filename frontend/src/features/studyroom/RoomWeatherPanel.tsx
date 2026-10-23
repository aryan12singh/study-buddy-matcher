import { useEffect, useState } from 'react'
import { api } from '../../shared/api/client'
import { useResource } from '../../shared/api/useResource'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import AtmosphereScene from './AtmosphereScene'
import { campusNight, readWeatherChoice, weatherConditions, weatherLabels } from './weatherTheme'
import type { RoomWeather, WeatherChoice, WeatherCondition } from './weatherTheme'
import './weatherTheme.css'

async function getWeather(signal: AbortSignal): Promise<RoomWeather> {
  return (await api.get('/weather', { signal })).data
}

export default function RoomWeatherPanel({ liveEnabled = false, onChange }: {
  liveEnabled?: boolean; onChange?: (condition: WeatherCondition, night: boolean) => void
}) {
  const [choice, setChoice] = useState<WeatherChoice>(readWeatherChoice)
  const [now, setNow] = useState(() => new Date())
  const weather = useResource('room-weather', getWeather, liveEnabled && choice === 'AUTO')
  const live = weather.data?.available ? weather.data : undefined
  const condition = choice === 'AUTO' ? live?.condition ?? 'CLEAR' : choice
  const night = choice === 'AUTO' && live ? !live.isDay : campusNight(now)
  useEffect(() => { onChange?.(condition, night) }, [condition, night, onChange])
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 60000)
    return () => window.clearInterval(timer)
  }, [])
  useEffect(() => {
    try { localStorage.setItem('study-room-atmosphere', choice) } catch { /* Storage is optional. */ }
  }, [choice])
  return <section className="detail-panel room-atmosphere" aria-label="Room atmosphere">
    <AtmosphereScene condition={condition} night={night} />
    <div className="atmosphere-controls">
      <h2>Room atmosphere</h2>
      <p>Make this corner your own. Your choice changes only this browser.</p>
      <Field id="room-atmosphere" label="Window view">
        <select id="room-atmosphere" value={choice} onChange={event => setChoice(event.target.value as WeatherChoice)}>
          <option value="AUTO">{liveEnabled ? 'Follow campus weather' : 'Follow campus daylight'}</option>
          {weatherConditions.map(value => <option key={value} value={value}>{weatherLabels[value]}</option>)}
        </select>
      </Field>
      {choice === 'AUTO' && liveEnabled ? <div className="atmosphere-status" role="status">
        {weather.loading ? <p>Checking campus weather…</p> : live ? <p>
          {live.location}: {weatherLabels[live.condition]}{live.temperatureCelsius !== null ? `, ${Math.round(live.temperatureCelsius)} °C` : ''}.
          {live.stale ? ' Last known conditions; the weather service is unavailable.' : ' Current campus conditions.'}
          {live.observedAt && <span className="muted"> Updated {new Intl.DateTimeFormat('en-SG', { timeZone: 'Asia/Singapore', hour: '2-digit', minute: '2-digit' }).format(new Date(live.observedAt))} SGT.</span>}
        </p> : <p>Live weather is unavailable. The window follows campus daylight; you can select any atmosphere.</p>}
        <Button disabled={weather.loading} onClick={() => weather.reload()}>Refresh weather</Button>
      </div> : <p className="muted">{choice === 'AUTO' ? 'Daylight follows Singapore time.' : 'Selected atmosphere; this is not a weather forecast.'}</p>}
      <p className="field-hint">{liveEnabled && <>Weather data: <a href="https://open-meteo.com/" target="_blank" rel="noreferrer">Open-Meteo</a> (CC BY 4.0). </>}The scene is decorative. It does not change shared audio or timer settings.</p>
    </div>
  </section>
}
