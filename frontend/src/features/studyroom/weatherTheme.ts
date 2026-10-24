export const weatherConditions = ['CLEAR', 'CLOUDY', 'RAIN', 'STORM', 'SNOW', 'FOG'] as const
export type WeatherCondition = typeof weatherConditions[number]
export type WeatherChoice = 'AUTO' | WeatherCondition
export const weatherLabels: Record<WeatherCondition, string> = {
  CLEAR: 'Clear skies', CLOUDY: 'Cloud cover', RAIN: 'Rainy window',
  STORM: 'Storm shelter', SNOW: 'Snowfall', FOG: 'Misty morning',
}
export type RoomWeather = {
  available: boolean; condition: WeatherCondition; temperatureCelsius: number | null
  isDay: boolean; observedAt: string | null; stale: boolean; location: string
}
export function campusNight(now: Date) {
  const hour = Number(new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Singapore', hour: '2-digit', hourCycle: 'h23' }).format(now))
  return hour < 7 || hour >= 19
}
export const roomScenes = ['DESK', 'LIBRARY', 'CAFE', 'GARDEN'] as const
export type RoomScene = typeof roomScenes[number]
export const sceneLabels: Record<RoomScene, string> = {
  DESK: 'Study desk', LIBRARY: 'Library nook', CAFE: 'Corner café', GARDEN: 'Garden meadow',
}
/** Each kind of room keeps its own atmosphere; group rooms keep the original storage keys. */
export type AtmosphereStore = 'study-room' | 'personal-room'
export function readSceneChoice(store: AtmosphereStore): RoomScene {
  try {
    const stored = localStorage.getItem(`${store}-scene`)
    return roomScenes.includes(stored as RoomScene) ? stored as RoomScene : 'DESK'
  } catch { return 'DESK' }
}
/** What a room's background shows; every scene shares the same sky, weather and daylight. */
export type Atmosphere = { scene: RoomScene; condition: WeatherCondition; night: boolean }
export const DEFAULT_ATMOSPHERE: Atmosphere = { scene: 'DESK', condition: 'CLEAR', night: false }
export type DaylightChoice = 'AUTO' | 'DAY' | 'NIGHT'
export function readDaylightChoice(store: AtmosphereStore): DaylightChoice {
  try {
    const stored = localStorage.getItem(`${store}-daylight`)
    return stored === 'DAY' || stored === 'NIGHT' ? stored : 'AUTO'
  } catch { return 'AUTO' }
}
export function readWeatherChoice(store: AtmosphereStore): WeatherChoice {
  try {
    const stored = localStorage.getItem(`${store}-atmosphere`)
    return stored === 'AUTO' || weatherConditions.includes(stored as WeatherCondition) ? stored as WeatherChoice : 'AUTO'
  } catch { return 'AUTO' }
}
