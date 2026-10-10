import { Link } from 'react-router-dom'
import PixelIcon from '../../shared/components/PixelIcon'

const STEPS = ['Pick a course', 'See ranked matches', 'Send a request']

/** Introduces the matching search and opens it on the Connections page. */
export default function FindBuddiesCard() {
  return (
    <section className="home-finder" aria-labelledby="home-finder">
      <span className="home-finder-icon" aria-hidden="true"><PixelIcon kind="search" /></span>
      <div>
        <h2 id="home-finder">Find study buddies</h2>
        <p>Pick a course or study goal and get a ranked list of compatible students.</p>
        <ol className="home-finder-steps">
          {STEPS.map((step, index) => <li key={step}><span>{index + 1}</span>{step}</li>)}
        </ol>
        <p><Link className="retro-button primary" to="/connections?view=find">Search for study buddies</Link></p>
      </div>
    </section>
  )
}
