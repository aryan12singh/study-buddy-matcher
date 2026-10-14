import { Link } from 'react-router-dom'
import Avatar from '../../shared/components/Avatar'
import Badge from '../../shared/components/Badge'
import PixelIcon, { type PixelIconKind } from '../../shared/components/PixelIcon'

const features: { icon: PixelIconKind; title: string; description: string; cta: string; href: string }[] = [
  {
    icon: 'search',
    title: 'Smart matching',
    description: 'Ranked by shared courses, timetable overlap, study mode and goals.',
    cta: 'See how matching works',
    href: '#how-it-works',
  },
  {
    icon: 'groups',
    title: 'Study groups',
    description: 'Join a group for your course, or create and lead your own.',
    cta: 'Browse study groups',
    href: '/groups',
  },
  {
    icon: 'connections',
    title: 'Stay connected',
    description: 'Send match requests and keep track of the buddies who accepted.',
    cta: 'Manage your requests',
    href: '/connections',
  },
]

const steps = [
  { title: 'Create your profile', description: 'Courses, weekly availability and study goals.' },
  { title: 'Get matched', description: 'A ranked list of compatible study buddies.' },
  { title: 'Connect', description: 'Send a match request and start studying together.' },
]

/** Static sample scores for the preview window; the real values come from the matching engine. */
const sampleScores = [
  { label: 'Course', className: 'score-course', score: 1, verdict: 'Same course' },
  { label: 'Availability', className: 'score-availability', score: 0.8, verdict: '6 h a week' },
  { label: 'Study mode', className: 'score-study-mode', score: 1, verdict: 'In person' },
]

const previewTags = ['Tue & Thu evenings', 'In person', 'Exam prep']

function LandingWindow({ title, className = '', children }: { title: string; className?: string; children: React.ReactNode }) {
  return (
    <section className={`retro-window ${className}`} aria-label={title}>
      <div className="window-titlebar">
        <span className="window-dots" aria-hidden="true">
          <i />
          <i />
          <i />
        </span>
        <span>{title}</span>
      </div>
      <div className="window-content">{children}</div>
    </section>
  )
}

function LandingPage() {
  return (
    <div className="desktop">
      <a href="#main-content" className="skip-link">Skip to content</a>
      <header className="desktop-menubar">
        <Link to="/" className="desktop-brand"><PixelIcon kind="brand" /><span>Study Buddy Matcher</span></Link>
        <nav className="landing-menu" aria-label="Landing page">
          <a href="#how-it-works" className="landing-menu-link">How it works</a>
          <a href="#features" className="landing-menu-link">Features</a>
          <Link to="/login" className="retro-button">Log in</Link>
          <Link to="/register" className="retro-button primary">Sign up</Link>
        </nav>
      </header>

      <main id="main-content" className="landing-layout" tabIndex={-1}>
        <LandingWindow title="Welcome.exe">
          <div className="landing-hero">
            <div className="landing-hero-text">
              <span className="home-eyebrow">Built for SMU students</span>
              <h1>Find your perfect study buddy.</h1>
              <p>
                Match with classmates by course, availability and study goals. Stop searching group chats for a
                study partner.
              </p>
              <div className="actions">
                <Link to="/register" className="retro-button primary">Create a student account</Link>
                <a href="#how-it-works" className="retro-button">See how it works</a>
              </div>
            </div>

            <figure className="landing-preview" aria-label="Sample match preview">
              <figcaption className="home-eyebrow">Sample preview</figcaption>
              <div className="landing-preview-card">
                <div className="person-heading">
                  <Avatar name="Your Buddy" />
                  <div>
                    <strong>Your study buddy</strong>
                    <div className="row-meta">IS442 · Object Oriented Programming</div>
                  </div>
                </div>
                <Badge tone="good">92% match</Badge>
                <ul className="landing-preview-scores">
                  {sampleScores.map(item => (
                    <li key={item.label} className={item.className}>
                      <div className="score-caption"><span>{item.label}</span><span>{item.verdict}</span></div>
                      <div className="score-track"><span style={{ width: `${item.score * 100}%` }} /></div>
                    </li>
                  ))}
                </ul>
                <div className="home-tags">
                  {previewTags.map(tag => <span key={tag} className="home-chip">{tag}</span>)}
                </div>
              </div>
            </figure>
          </div>
        </LandingWindow>

        <div id="features" className="landing-features">
          {features.map(feature => (
            <LandingWindow key={feature.title} title={feature.title}>
              <div className="home-tile-icon"><PixelIcon kind={feature.icon} /></div>
              <h2>{feature.title}</h2>
              <p>{feature.description}</p>
              {feature.href.startsWith('#')
                ? <a href={feature.href} className="home-more">{feature.cta} ›</a>
                : <Link to={feature.href} className="home-more">{feature.cta} ›</Link>}
            </LandingWindow>
          ))}
        </div>

        <div id="how-it-works">
          <LandingWindow title="How it works">
            <h2>Three steps to your study buddy</h2>
            <ol className="landing-steps">
              {steps.map((step, index) => (
                <li key={step.title}>
                  <span className="landing-step-number">{index + 1}</span>
                  <div>
                    <strong>{step.title}</strong>
                    <p>{step.description}</p>
                  </div>
                </li>
              ))}
            </ol>
          </LandingWindow>
        </div>

        <div className="home-next">
          <div className="home-next-text">
            <span className="home-eyebrow">Get started</span>
            <strong>Ready to find your study buddy?</strong>
            <span>Create your profile in minutes and get your first matches.</span>
          </div>
          <Link to="/register" className="retro-button primary">Create a student account</Link>
        </div>
      </main>

      <footer className="desktop-footer">
        <span>© {new Date().getFullYear()} Study Buddy Matcher</span>
        <span>Already have an account? <Link to="/login">Log in</Link></span>
      </footer>
    </div>
  )
}

export default LandingPage
