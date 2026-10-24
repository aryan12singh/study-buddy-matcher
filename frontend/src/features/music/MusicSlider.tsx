import type { CSSProperties, InputHTMLAttributes } from 'react'

type Props = Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'value' | 'onChange'> & {
  value: number
  min: number
  max: number
  onValue: (value: number) => void
}

/** A native range input styled to the room: the track fills up to the handle. */
export default function MusicSlider({ value, min, max, onValue, className = '', style, ...props }: Props) {
  const fill = max > min ? ((value - min) / (max - min)) * 100 : 0
  return <input {...props} type="range" className={`music-slider ${className}`} min={min} max={max} value={value}
    style={{ ...style, '--fill': `${fill}%` } as CSSProperties}
    onChange={event => onValue(Number(event.target.value))} />
}
