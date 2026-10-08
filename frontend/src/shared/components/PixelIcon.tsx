const patterns = {
  brand: ['1111100', '1000100', '1011111', '1010001', '1011101', '1010001', '1111111'],
  connections: ['0110110', '1111111', '0110110', '0000000', '1111111', '1101011', '1101011'],
  groups: ['1111111', '1001001', '1111111', '1001001', '1111111', '1001001', '1111111'],
  notifications: ['0001000', '0011100', '0111110', '0111110', '1111111', '0000000', '0011100'],
  profile: ['0111110', '1100011', '1001001', '1011101', '1000001', '1100011', '0111110'],
  accounts: ['1111111', '1000001', '1011001', '1011011', '1000001', '1111101', '1111111'],
  clock: ['0011100', '0100010', '1001001', '1001001', '1000101', '0100010', '0011100']
} as const

export type PixelIconKind = keyof typeof patterns

/** Small original CSS pixel drawings; decorative wherever a text label is present. */
export default function PixelIcon({ kind = 'brand' }: { kind?: PixelIconKind }) {
  return <span className="pixel-icon" aria-hidden="true">
    {patterns[kind].flatMap((row, y) => Array.from(row, (pixel, x) => (
      <i key={`${y}-${x}`} className={pixel === '1' ? 'pixel-on' : undefined} />
    )))}
  </span>
}
