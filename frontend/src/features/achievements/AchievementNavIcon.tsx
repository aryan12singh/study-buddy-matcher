/** A 7 x 7 pixel medal in the same CSS grid style as the shared PixelIcon set. */
const MEDAL = ['1100011', '0110110', '0011100', '0111110', '1101011', '1111111', '0111110']

export default function AchievementNavIcon() {
  return <span className="pixel-icon" aria-hidden="true">
    {MEDAL.flatMap((row, y) => Array.from(row, (pixel, x) => (
      <i key={`${y}-${x}`} className={pixel === '1' ? 'pixel-on' : undefined} />
    )))}
  </span>
}
