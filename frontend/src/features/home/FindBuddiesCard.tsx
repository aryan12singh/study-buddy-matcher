import Badge from '../../shared/components/Badge'
import PixelIcon from '../../shared/components/PixelIcon'

const STEPS = ['Pick a course', 'See ranked matches', 'Send a request']

/**
 * A preview of Team A's matching search until their page exists.
 * Replace the step row with a link to that page when it lands.
 */
export default function FindBuddiesCard() {
  return (
    <section className="home-finder" aria-labelledby="home-finder">
      <span className="home-finder-icon" aria-hidden="true"><PixelIcon kind="search" /></span>
      <div>
        <h2 id="home-finder">Find study buddies <Badge>Coming soon</Badge></h2>
        <p>Pick a course or study goal and get a ranked list of compatible students.</p>
        <ol className="home-finder-steps">
          {STEPS.map((step, index) => <li key={step}><span>{index + 1}</span>{step}</li>)}
        </ol>
      </div>
    </section>
  )
}
