import { useEffect, useRef, useState } from 'react'
import type { KeyboardEvent, PointerEvent } from 'react'
import TreeSprite from './TreeSprite'
import { LIGHT_NAMES, formatMinutes } from './api'
import type { Tree } from './api'
import { layOut } from './meadowLayout'
import { treeName } from './treeSprites'

const MEADOW_PIXELS = 400
const START = { spin: -35, tilt: 58 }
const TILT_LIMITS = { min: 30, max: 72 }
const IDLE_DEGREES_PER_SECOND = 6
const dates = new Intl.DateTimeFormat('en-SG', { day: 'numeric', month: 'short', year: 'numeric' })

function describe(tree: Tree) {
  return `${treeName(tree.species, tree.variant)}, ${formatMinutes(tree.focusedMinutes)} focused in ${tree.groupName}, `
    + `planted ${dates.format(new Date(tree.plantedAt))} at ${LIGHT_NAMES[tree.light]}`
}

/**
 * A small pixel diorama of the forest that turns in 3D. The view angle lives in CSS custom
 * properties set directly on the scene, so dragging or the idle spin never re-renders the trees.
 */
export default function Meadow3D({ trees }: { trees: Tree[] }) {
  const scene = useRef<HTMLDivElement>(null)
  const view = useRef({ ...START })
  const drag = useRef<{ x: number; y: number } | null>(null)
  const idle = useRef(true)
  const [selected, setSelected] = useState<Tree | null>(null)
  const { placed, decorations } = layOut(trees, MEADOW_PIXELS)

  function apply() {
    scene.current?.style.setProperty('--spin', `${view.current.spin}deg`)
    scene.current?.style.setProperty('--tilt', `${view.current.tilt}deg`)
  }
  function turn(spin: number, tilt = 0) {
    idle.current = false
    view.current.spin += spin
    view.current.tilt = Math.min(TILT_LIMITS.max, Math.max(TILT_LIMITS.min, view.current.tilt + tilt))
    apply()
  }

  useEffect(() => {
    apply()
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return
    let frame = 0, last = performance.now()
    const step = (now: number) => {
      if (idle.current && !document.hidden) {
        view.current.spin += IDLE_DEGREES_PER_SECOND * (now - last) / 1000
        apply()
      }
      last = now
      frame = requestAnimationFrame(step)
    }
    frame = requestAnimationFrame(step)
    return () => cancelAnimationFrame(frame)
  }, [])

  function onPointerDown(event: PointerEvent<HTMLDivElement>) {
    if ((event.target as HTMLElement).closest('button')) return
    drag.current = { x: event.clientX, y: event.clientY }
    event.currentTarget.setPointerCapture(event.pointerId)
  }
  function onPointerMove(event: PointerEvent<HTMLDivElement>) {
    if (!drag.current) return
    turn((event.clientX - drag.current.x) * .45, (drag.current.y - event.clientY) * .25)
    drag.current = { x: event.clientX, y: event.clientY }
  }
  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const moves: Record<string, [number, number]> = { ArrowLeft: [-15, 0], ArrowRight: [15, 0], ArrowUp: [0, 6], ArrowDown: [0, -6] }
    const move = moves[event.key]
    if (!move) return
    event.preventDefault()
    turn(...move)
  }
  function reset() {
    idle.current = false
    view.current = { ...START }
    apply()
  }

  return (
    <div className="meadow3d">
      <div ref={scene} className="meadow-stage" tabIndex={0} role="group"
        aria-label="3D forest. Drag, or use the arrow keys, to turn and tilt it. Tab to each tree for its details."
        onPointerDown={onPointerDown} onPointerMove={onPointerMove}
        onPointerUp={() => { drag.current = null }} onPointerCancel={() => { drag.current = null }} onKeyDown={onKeyDown}>
        <div className="meadow-world" style={{ width: MEADOW_PIXELS, height: MEADOW_PIXELS }}>
          <div className="meadow-ground" />
          <div className="meadow-side side-south" /><div className="meadow-side side-north" />
          <div className="meadow-side side-east" /><div className="meadow-side side-west" />
          {decorations.map((decoration, index) => (
            <span key={index} className={`meadow-decoration is-${decoration.kind}`} aria-hidden="true"
              style={{ left: `${decoration.x * 100}%`, top: `${decoration.y * 100}%` }} />
          ))}
          {placed.map(({ tree, x, y, width }) => (
            <div key={tree.id} className="meadow-spot" style={{ left: `${x * 100}%`, top: `${y * 100}%` }}>
              <span className="meadow-shadow" style={{ width: width * .8 }} />
              <div className="meadow-billboard">
                <button type="button" className={`meadow-tree${selected?.id === tree.id ? ' is-selected' : ''}`}
                  style={{ width, height: width * 1.5 }} aria-label={describe(tree)}
                  onClick={() => setSelected(tree)} onFocus={() => setSelected(tree)}>
                  <TreeSprite species={tree.species} variant={tree.variant} light={tree.light} soil={false} />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
      <div className="meadow-controls">
        <button type="button" className="retro-button" onClick={() => turn(-30)}>◀ Turn left</button>
        <button type="button" className="retro-button" onClick={() => turn(30)}>Turn right ▶</button>
        <button type="button" className="retro-button" onClick={reset}>Reset view</button>
      </div>
      <p className="meadow-detail" aria-live="polite">
        {selected
          ? <><strong>{treeName(selected.species, selected.variant)}</strong> · {describe(selected).split(', ').slice(1).join(', ')}</>
          : 'Select a tree to see when and where it grew.'}
      </p>
    </div>
  )
}
