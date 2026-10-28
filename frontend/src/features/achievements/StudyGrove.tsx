import type { Streak } from './api'
import { PixelRects } from './pixelArt'
import { Link } from 'react-router-dom'

const LEAVES = { l: '#9fd36f', m: '#4f9a4a', d: '#2f6b3a', t: '#6b4426', s: '#c9a46a' }
const GROUND = { g: '#5aa83e', h: '#7cc457', s: '#8a5a32', r: '#6b4426' }
const BLANK = '............'

const SAPLING = [BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK,
  '.....ll.....', '....lmml....', '...lm..ml...', '.....mt.....', '.....t......', '.....t......', '....ttt.....']
const YOUNG = [BLANK, BLANK, BLANK, BLANK, BLANK,
  '.....ll.....', '....lllm....', '...llmmmd...', '...lmmmmdd..', '...mmmmmdd..', '....dmmdd...', '.....tt.....',
  '.....tt.....', '.....tt.....', '.....tt.....', '....tttt....']
const GROWN = ['....llll....', '...lllmmm...', '..llmmmmmd..', '.llmmmmmmdd.', '.lmmmmmmmdd.', 'llmmmmmmmddd',
  'lmmmmmmmdddd', '.mmmmmmmddd.', '.dmmmmmdddd.', '..ddddddd...', '....dttd....', '.....tt.....',
  '.....tt.....', '.....tt.....', '.....tt.....', '....tttt....']
const SOIL = ['hghghghghghg', 'gggggggggggg', 'ssrssssrssss', 'srssrssssrss']
const SEED = ['.....ss.....', '....sstt....', '.....tt.....']

const MAX_TREES = 12
const formatter = new Intl.DateTimeFormat('en-SG', { day: 'numeric', month: 'short' })

function stageFor(weeksOld: number) {
  if (weeksOld === 0) return { rows: SAPLING, name: 'sapling' }
  if (weeksOld === 1) return { rows: YOUNG, name: 'young tree' }
  return { rows: GROWN, name: 'full tree' }
}

function Plot({ rows, label, delay }: { rows: readonly string[]; label: string; delay?: number }) {
  return (
    <li className="grove-plot" style={delay === undefined ? undefined : { animationDelay: `${delay}ms` }}>
      <svg viewBox="0 0 12 20" aria-hidden="true" focusable="false" shapeRendering="crispEdges">
        <g className="grove-tree"><PixelRects rows={rows} palette={LEAVES} /></g>
        <PixelRects rows={SOIL} palette={GROUND} y={16} />
      </svg>
      <span className="grove-label">{label}</span>
    </li>
  )
}

/** One pixel tree per week of the current streak; the newest week is still a sapling. */
export default function StudyGrove({ streak }: { streak: Streak }) {
  const weeks = streak.currentRunWeeks.slice(-MAX_TREES)
  const hidden = streak.currentRunWeeks.length - weeks.length
  return (
    <section className="grove" aria-labelledby="grove-title">
      <div className="grove-heading">
        <h2 id="grove-title">Planned-session grove</h2>
        <p>One tree per consecutive week with an eligible planned session, with weeks starting on Monday. A session later this week counts now; future weeks wait until they begin.</p>
      </div>
      {weeks.length === 0 ? (
        <div className="grove-empty">
          <ul className="grove-row" aria-hidden="true">
            <Plot rows={[BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, BLANK, ...SEED]} label="" />
          </ul>
          <div>
            <h3>Your grove is a seed</h3>
            <p>Plan a session with a buddy or group this week to plant your first tree. If your last run ended more than a week ago, a new plan starts a fresh grove.</p>
            <div className="actions">
              <Link className="retro-button primary" to="/sessions">Open study sessions</Link>
              <Link className="retro-button" to="/find-buddies">Find a study buddy</Link>
            </div>
          </div>
        </div>
      ) : (
        <>
          {hidden > 0 && <p className="grove-more">Showing the latest {weeks.length} of {streak.currentRunWeeks.length} weeks.</p>}
          <ul className="grove-row" aria-label={`${streak.currentWeeks} week planned-session streak`}>
            {weeks.map((week, index) => {
              const stage = stageFor(weeks.length - 1 - index)
              const date = formatter.format(new Date(`${week}T00:00:00`))
              return <Plot key={week} rows={stage.rows} delay={index * 60} label={`Week of ${date}: ${stage.name}`} />
            })}
          </ul>
        </>
      )}
    </section>
  )
}
