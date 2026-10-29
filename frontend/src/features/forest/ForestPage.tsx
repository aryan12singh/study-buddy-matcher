import { Link } from 'react-router-dom'
import { useResource } from '../../shared/api/useResource'
import { useAuth } from '../../shared/auth/useAuth'
import StatePanel from '../../shared/components/StatePanel'
import WindowPage from '../../shared/components/WindowPage'
import TreeSprite from './TreeSprite'
import { LIGHT_NAMES, SPECIES_NAMES, formatMinutes, getMyForest } from './api'
import './forest.css'

const MAX_TREES = 60
const dates = new Intl.DateTimeFormat('en-SG', { day: 'numeric', month: 'short' })

export default function ForestPage() {
  const { account } = useAuth()
  const forest = useResource(`forest-${account?.id}`, getMyForest, account?.role === 'STUDENT')
  const data = forest.data
  const shown = data?.trees.slice(0, MAX_TREES) ?? []
  return (
    <WindowPage title="My forest" description="One tree for every study room visit. Stay focused longer and the tree grows grander."
      actions={<Link className="retro-button" to="/achievements">Back to achievements</Link>}>
      {!data ? (
        <StatePanel loading={forest.loading} error={forest.error} onRetry={() => forest.reload(true)} />
      ) : (
        <div className="forest">
          <dl className="forest-stats">
            <div><dt>Trees planted</dt><dd>{data.treeCount}</dd></div>
            <div><dt>Focused time</dt><dd>{formatMinutes(data.focusedMinutes)}</dd></div>
            <div><dt>Longest visit</dt><dd>{formatMinutes(data.longestMinutes)}</dd></div>
          </dl>
          {data.trees.length === 0 ? (
            <StatePanel empty emptyTitle="Your forest is a seed"
              emptyMessage={`Join a study room and stay while the focus timer runs. ${data.stages[0]?.minutes ?? 10} focused minutes plants your first tree.`}
              emptyAction={<Link className="retro-button primary" to="/study-room">Open study rooms</Link>} />
          ) : (
            <section className="forest-meadow" aria-labelledby="meadow-title">
              <h2 id="meadow-title">Meadow</h2>
              {data.trees.length > shown.length && <p className="forest-more">Showing your latest {shown.length} of {data.trees.length} trees.</p>}
              <ul className="forest-grid">
                {shown.map((tree, index) => (
                  <li key={tree.id} className={`forest-tree species-${tree.species.toLowerCase()}`}
                    style={{ animationDelay: `${Math.min(index, 20) * 40}ms` }}>
                    <TreeSprite species={tree.species} light={tree.light} />
                    <span className="forest-label">
                      <strong>{SPECIES_NAMES[tree.species]}</strong>
                      <span>{dates.format(new Date(tree.plantedAt))} · {formatMinutes(tree.focusedMinutes)}</span>
                      <span className="forest-group">{tree.groupName}, {LIGHT_NAMES[tree.light]}</span>
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          )}
          <section className="forest-legend" aria-labelledby="legend-title">
            <h2 id="legend-title">How trees grow</h2>
            <p>Time counts only while you are in the room and the shared timer is running a focus phase. Breaks and pauses do not count. Each visit plants one tree when you leave; the hour you started sets its colours.</p>
            <ol>
              {data.stages.map(stage => (
                <li key={stage.species}>
                  <TreeSprite species={stage.species} light="DAY" />
                  <span><strong>{SPECIES_NAMES[stage.species]}</strong> from {formatMinutes(stage.minutes)}</span>
                </li>
              ))}
            </ol>
          </section>
        </div>
      )}
    </WindowPage>
  )
}
