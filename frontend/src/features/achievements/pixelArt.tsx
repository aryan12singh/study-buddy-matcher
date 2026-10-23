/** Renders rows of palette characters as crisp SVG rectangles; '.' is transparent. */
export function PixelRects({ rows, palette, x = 0, y = 0, size = 1 }: {
  rows: readonly string[]
  palette: Record<string, string>
  x?: number
  y?: number
  size?: number
}) {
  return <g shapeRendering="crispEdges">
    {rows.flatMap((row, rowIndex) => Array.from(row, (cell, column) => cell === '.' || !palette[cell] ? null : (
      <rect key={`${rowIndex}-${column}`} x={x + column * size} y={y + rowIndex * size} width={size} height={size} fill={palette[cell]} />
    )))}
  </g>
}
