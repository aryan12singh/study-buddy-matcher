import { useEffect } from 'react'
import { Link, useLocation } from 'react-router-dom'
import WindowPage from '../../shared/components/WindowPage'
import ExplainerCard from './ExplainerCard'
import FaqSection from './FaqSection'
import { EXPLAINERS } from './helpContent'
import StartChecklist from './StartChecklist'
import './help.css'

/** A getting-started guide: first steps, how each area works, and searchable answers. */
export default function HelpPage() {
  const { hash } = useLocation()
  useEffect(() => {
    if (!hash) return
    let active = true
    // Run after the shared shell's route focus, so contextual links land on their topic.
    queueMicrotask(() => {
      if (!active) return
      const target = document.getElementById(hash.slice(1))
      target?.focus({ preventScroll: true })
      target?.scrollIntoView?.({ block: 'start' })
    })
    return () => { active = false }
  }, [hash])
  return (
    <WindowPage
      title="Help and getting started"
      description="Find your next action, understand your matches, and turn a study connection into a dated plan."
    >
      <nav className="help-jump" aria-label="On this page">
        <Link to="#help-start-title">Start here</Link>
        <Link to="#help-matching">Matching</Link>
        <Link to="#help-ai">AI and privacy</Link>
        <Link to="#help-groups">Groups and rooms</Link>
        <Link to="#help-sessions">Sessions and calendar</Link>
        <Link to="#help-faq-title">Questions</Link>
      </nav>
      <div className="help-layout">
        <StartChecklist />
        <section className="help-section" aria-labelledby="help-how-title">
          <div className="help-section-head">
            <div>
              <span className="help-eyebrow">The basics</span>
              <h2 id="help-how-title" tabIndex={-1}>How it works</h2>
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
