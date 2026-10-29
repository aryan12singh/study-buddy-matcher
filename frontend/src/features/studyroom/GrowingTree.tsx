import { Link } from 'react-router-dom'
import { useResource } from '../../shared/api/useResource'
import TreeSprite from '../forest/TreeSprite'
import { formatMinutes, getGrowingTree } from '../forest/api'
import { treeName } from '../forest/treeSprites'

function withArticle(name: string) {
  return /^[aeiou]/.test(name) ? `an ${name}` : `a ${name}`
}

/** The student's tree for this visit, growing as focus time is credited. */
export default function GrowingTree({ groupId, accountId }: { groupId: number; accountId?: number }) {
  const growing = useResource(`growing-tree-${accountId}-${groupId}`, signal => getGrowingTree(groupId, signal))
  const tree = growing.data
  const visiting = tree?.visiting === true
  const species = visiting ? tree.species : null
  return (
    <aside className="growing-tree" aria-labelledby="growing-tree-title">
      <TreeSprite species={species} variant={visiting ? tree.variant : 0} light={visiting ? tree.light : null} className="growing-tree-sprite" />
      <div>
        <h3 id="growing-tree-title">Your tree</h3>
        {visiting ? <>
          <p><strong>{species ? treeName(species, tree.variant) : 'Seed'}</strong> · {formatMinutes(tree.focusedMinutes)} focused</p>
          <p>{tree.next && tree.minutesToNext !== null
            ? `${formatMinutes(tree.minutesToNext)} more to grow into ${withArticle(treeName(tree.next, tree.variant).toLowerCase())}.`
            : 'The grandest tree there is. Keep going!'}</p>
        </> : <p>Grows while you stay and the focus timer runs. It is planted in your forest when you leave.</p>}
        <Link className="retro-button" to="/forest">View my forest</Link>
      </div>
    </aside>
  )
}
