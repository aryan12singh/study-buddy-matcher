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
export function readWeatherChoice(): WeatherChoice {
  try {
    const stored = localStorage.getItem('study-room-atmosphere')
    return stored === 'AUTO' || weatherConditions.includes(stored as WeatherCondition) ? stored as WeatherChoice : 'AUTO'
  } catch { return 'AUTO' }
}
