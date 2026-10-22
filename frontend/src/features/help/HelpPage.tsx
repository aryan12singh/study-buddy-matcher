import WindowPage from '../../shared/components/WindowPage'
import ExplainerCard from './ExplainerCard'
import FaqSection from './FaqSection'
import { EXPLAINERS } from './helpContent'
import StartChecklist from './StartChecklist'
import './help.css'

/** A getting-started guide: first steps, how each area works, and searchable answers. */
export default function HelpPage() {
  return (
    <WindowPage
      title="Help and getting started"
      description="New here? Work through the first steps, then read how each part of Study Buddy Matcher works."
    >
      <nav className="help-jump" aria-label="On this page">
        <a href="#help-start-title">Start here</a>
        <a href="#help-how-title">How it works</a>
        <a href="#help-faq-title">Questions</a>
      </nav>
      <div className="help-layout">
        <StartChecklist />
        <section className="help-section" aria-labelledby="help-how-title">
          <div className="help-section-head">
            <div>
              <span className="help-eyebrow">The basics</span>
              <h2 id="help-how-title">How it works</h2>
            </div>
          </div>
          <div className="help-cards">
            {EXPLAINERS.map(explainer => <ExplainerCard key={explainer.key} explainer={explainer} />)}
          </div>
        </section>
        <FaqSection />
      </div>
    </WindowPage>
  )
}
