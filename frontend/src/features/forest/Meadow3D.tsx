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
/** Degrees per pixel. The plane turns so its near edge follows the pointer, like a turntable. */
const DRAG_TURN = .45
const DRAG_TILT = .25
const SWIPE_TURN = .3
const KEY_TURN = 15
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
  /**
   * Moves the near edge of the meadow sideways: positive goes right. In CSS a positive rotateZ is
   * clockwise, which carries the near edge left, so the spin angle changes the opposite way.
   */
  function turn(rightward: number, tilt = 0) {
    idle.current = false
    view.current.spin -= rightward
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

  // A sideways trackpad swipe turns the meadow like a drag; up and down still scroll the page.
  // Registered by hand because React's wheel listener is passive and cannot keep the page still.
  useEffect(() => {
    const stage = scene.current
    if (!stage) return
    const onWheel = (event: WheelEvent) => {
      if (Math.abs(event.deltaX) <= Math.abs(event.deltaY)) return
      event.preventDefault()
      turn(-event.deltaX * SWIPE_TURN)
    }
    stage.addEventListener('wheel', onWheel, { passive: false })
    return () => stage.removeEventListener('wheel', onWheel)
  })

  function onPointerDown(event: PointerEvent<HTMLDivElement>) {
    if ((event.target as HTMLElement).closest('button')) return
    drag.current = { x: event.clientX, y: event.clientY }
    event.currentTarget.setPointerCapture(event.pointerId)
  }
  function onPointerMove(event: PointerEvent<HTMLDivElement>) {
    if (!drag.current) return
    turn((event.clientX - drag.current.x) * DRAG_TURN, (drag.current.y - event.clientY) * DRAG_TILT)
    drag.current = { x: event.clientX, y: event.clientY }
  }
  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    const moves: Record<string, [number, number]> = { ArrowLeft: [-KEY_TURN, 0], ArrowRight: [KEY_TURN, 0], ArrowUp: [0, 6], ArrowDown: [0, -6] }
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
