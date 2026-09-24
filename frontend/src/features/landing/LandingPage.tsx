import { Link } from 'react-router-dom'

const features = [
  {
    tag: 'MATCHING',
    title: 'Smart Matching',
    description: 'Matched by course, timetable overlap, and study mode.',
    cta: 'See how matching works →',
    icon: (
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path
          d="M4 5C4 4 5 3 6 3H11V19H6C5 19 4 20 4 21V5Z"
          stroke="currentColor"
          strokeWidth="1.6"
          strokeLinejoin="round"
        />
        <path
          d="M20 5C20 4 19 3 18 3H13V19H18C19 19 20 20 20 21V5Z"
          stroke="currentColor"
          strokeWidth="1.6"
          strokeLinejoin="round"
        />
      </svg>
    ),
  },
  {
    tag: 'GROUPS',
    title: 'Study Groups',
    description: 'Join a group for your course, or create and lead your own.',
    cta: 'Browse study groups →',
    icon: (
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="9" cy="8" r="3" stroke="currentColor" strokeWidth="1.6" />
        <circle cx="17" cy="9" r="2.4" stroke="currentColor" strokeWidth="1.6" />
        <path
          d="M3.5 20C3.5 16 6 14 9 14C12 14 14.5 16 14.5 20"
          stroke="currentColor"
          strokeWidth="1.6"
          strokeLinecap="round"
        />
        <path d="M14.5 15C17.5 15 19.5 16.7 19.8 20" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      </svg>
    ),
  },
  {
    tag: 'CONNECTIONS',
    title: 'Stay Connected',
    description: "See who's online and manage your study-buddy requests.",
    cta: 'Manage your requests →',
    icon: (
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path
          d="M4 6C4 4.9 4.9 4 6 4H18C19.1 4 20 4.9 20 6V14C20 15.1 19.1 16 18 16H10L6 20V16H6C4.9 16 4 15.1 4 14V6Z"
          stroke="currentColor"
          strokeWidth="1.6"
          strokeLinejoin="round"
        />
        <circle cx="9" cy="10" r="1" fill="currentColor" />
        <circle cx="12" cy="10" r="1" fill="currentColor" />
        <circle cx="15" cy="10" r="1" fill="currentColor" />
      </svg>
    ),
  },
]

const steps = [
  {
    number: '1',
    title: 'Create your profile',
    description: 'Courses, availability, and study goals.',
  },
  {
    number: '2',
    title: 'Get matched',
    description: 'A ranked list of compatible study buddies.',
  },
  {
    number: '3',
    title: 'Connect',
    description: 'Send a request and start studying together.',
  },
]

const previewTags = ['Tue & Thu evenings', 'In person', 'Exam prep']

function StepArrow() {
  return (
    <div className="flex flex-none w-6 justify-center">
      <svg width="24" height="12" viewBox="0 0 24 12" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M0 6H19M19 6L14 1M19 6L14 11" stroke="rgba(31,46,34,0.3)" strokeWidth="1.5" />
      </svg>
    </div>
  )
}

function LandingPage() {
  return (
    <div className="flex min-h-screen flex-col bg-retro-cream text-retro-green">
      {/* NAVBAR */}
      <header className="flex items-center justify-between bg-retro-cream px-14 py-[22px]">
        <span className="font-display text-xl font-bold text-retro-green">Study Buddy Matcher</span>
        <nav className="flex items-center gap-9">
          <a href="#how-it-works" className="text-[13px] font-semibold tracking-wide text-retro-green/75">
            HOW IT WORKS
          </a>
          <a href="#features" className="text-[13px] font-semibold tracking-wide text-retro-green/75">
            FEATURES
          </a>
        </nav>
        <div className="flex items-center gap-2">
          <Link to="/login" className="rounded-full px-4 py-[10px] text-sm font-medium text-retro-green">
            Log In
          </Link>
          <Link
            to="/register"
            className="rounded-full bg-retro-lime px-[22px] py-[11px] text-[13px] font-bold tracking-wide text-retro-green-dark hover:bg-retro-lime-dark"
          >
            SIGN UP
          </Link>
        </div>
      </header>

      {/* HERO (full-bleed) */}
      <section className="bg-retro-green px-14 pt-14 pb-[72px]">
        <div className="mx-auto flex max-w-[1200px] items-center gap-14">
          <div className="flex min-w-0 flex-1 flex-col items-start gap-[22px]">
            <h1 className="font-display text-[42px] font-bold leading-[1.15] text-retro-cream">
              Find your perfect study buddy.
            </h1>
            <p className="max-w-[460px] text-base leading-relaxed text-retro-cream/70">
              Match with classmates by course, availability, and study goals — stop searching group chats for a
              study partner.
            </p>
            <div className="mt-1.5 flex gap-3.5">
              <Link
                to="/register"
                className="rounded-full bg-retro-lime px-[26px] py-3.5 text-sm font-semibold text-retro-green-dark hover:bg-retro-lime-dark"
              >
                Get Started
              </Link>
              <a
                href="#how-it-works"
                className="rounded-full border border-retro-cream/35 px-[26px] py-3.5 text-sm font-semibold text-retro-cream hover:bg-white/10"
              >
                See how it works
              </a>
            </div>
            <p className="text-sm text-retro-cream/55">Built for SMU students</p>
          </div>

          {/* SAMPLE PREVIEW */}
          <div className="w-[360px] flex-none">
            <div className="mb-3 text-xs font-semibold tracking-wide text-retro-cream/55">SAMPLE PREVIEW</div>
            <div className="rounded-2xl border border-retro-cream/10 bg-white/5 p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-retro-lime text-[13px] font-bold text-retro-green-dark">
                    You
                  </div>
                  <div className="-ml-2 flex h-9 w-9 items-center justify-center rounded-full border-2 border-retro-green bg-retro-cream text-[13px] font-bold text-retro-green">
                    B
                  </div>
                </div>
                <span className="rounded-full bg-retro-lime/25 px-3 py-1 text-xs font-semibold text-retro-lime">
                  92% match
                </span>
              </div>
              <div className="mt-4 font-display text-lg font-semibold text-retro-cream">Your study buddy</div>
              <div className="mt-1 text-sm text-retro-cream/55">IS442 · Object Oriented Programming</div>
              <div className="mt-4 flex flex-wrap gap-2">
                {previewTags.map((tag) => (
                  <span
                    key={tag}
                    className="rounded-full border border-retro-cream/15 bg-white/5 px-3 py-1 text-xs text-retro-cream/80"
                  >
                    {tag}
                  </span>
                ))}
              </div>
              <button className="mt-5 w-full rounded-full bg-retro-lime py-3 text-sm font-semibold text-retro-green-dark hover:bg-retro-lime-dark">
                Send request
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* FEATURES */}
      <section id="features" className="px-14 pt-14">
        <div className="mx-auto grid max-w-[1200px] grid-cols-3 gap-6">
          {features.map((feature) => (
            <div
              key={feature.title}
              className="overflow-hidden rounded-[20px] border border-retro-green/[0.08] bg-retro-cream shadow-[0_12px_24px_rgba(22,33,26,0.12)]"
            >
              <div className="flex h-[130px] items-center justify-center bg-retro-green text-retro-lime">
                {feature.icon}
              </div>
              <div className="px-6 pb-[26px] pt-[22px]">
                <span className="mb-3 inline-block rounded-full bg-retro-lime/35 px-3 py-1 text-[11px] font-bold tracking-wide text-retro-green-dark">
                  {feature.tag}
                </span>
                <div className="mb-1.5 font-display text-lg font-semibold text-retro-green">{feature.title}</div>
                <div className="mb-3.5 text-sm leading-relaxed text-retro-green/65">{feature.description}</div>
                <a href="#" className="text-[13px] font-bold text-retro-green">
                  {feature.cta}
                </a>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* THREE STEPS */}
      <section id="how-it-works" className="mt-11 bg-[#EBE1C9] px-14 py-12">
        <div className="mx-auto max-w-[1200px]">
          <h2 className="font-display text-3xl font-bold text-retro-green">Three steps to your study buddy</h2>
          <div className="mt-7 flex items-center gap-4">
            {steps.map((step, index) => (
              <div key={step.title} className="contents">
                <div className="flex flex-1 flex-col gap-2.5 rounded-[18px] border border-retro-green/10 bg-white/50 p-6">
                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-retro-green font-display text-[15px] font-semibold text-retro-cream">
                    {step.number}
                  </div>
                  <div className="font-display text-lg font-semibold text-retro-green">{step.title}</div>
                  <div className="text-sm leading-relaxed text-retro-green/65">{step.description}</div>
                </div>
                {index < steps.length - 1 && <StepArrow />}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA BANNER */}
      <section className="bg-retro-lime px-14 py-14">
        <div className="mx-auto flex max-w-[1200px] items-center justify-between gap-6">
          <div>
            <h2 className="font-display text-[28px] font-bold text-retro-green-dark">
              Ready to find your study buddy?
            </h2>
            <p className="mt-1.5 text-sm text-retro-green-dark/75">
              Create your profile in minutes and get your first matches.
            </p>
          </div>
          <Link
            to="/register"
            className="flex-none rounded-full bg-retro-green-dark px-7 py-3.5 text-sm font-semibold text-retro-cream hover:bg-retro-green"
          >
            Get Started
          </Link>
        </div>
      </section>

      {/* FOOTER */}
      <footer className="mt-auto bg-retro-green px-14 pb-8 pt-14">
        <div className="mx-auto max-w-[1200px]">
          <div className="grid grid-cols-[2fr_1fr_1fr] gap-10 pb-9">
            <div className="flex flex-col gap-2.5">
              <div className="font-display text-lg font-bold text-retro-cream">Study Buddy Matcher</div>
              <div className="max-w-[280px] text-[13px] leading-relaxed text-retro-cream/60">
                Helping students find compatible study partners by course, availability, and study goals.
              </div>
            </div>
            <div className="flex flex-col gap-2.5">
              <div className="text-xs font-bold tracking-wide text-retro-cream/50">NAVIGATE</div>
              <a href="#how-it-works" className="text-[13px] text-retro-cream/75">
                How it Works
              </a>
              <a href="#features" className="text-[13px] text-retro-cream/75">
                Features
              </a>
            </div>
            <div className="flex flex-col gap-2.5">
              <div className="text-xs font-bold tracking-wide text-retro-cream/50">ACCOUNT</div>
              <Link to="/login" className="text-[13px] text-retro-cream/75">
                Log In
              </Link>
              <Link to="/register" className="text-[13px] text-retro-cream/75">
                Sign Up
              </Link>
            </div>
          </div>
          <div className="flex items-center justify-between border-t border-retro-cream/[0.12] pt-5">
            <div className="text-xs text-retro-cream/50">© {new Date().getFullYear()} Study Buddy Matcher</div>
            <div className="flex gap-6">
              <a href="#" className="text-xs text-retro-cream/50">
                Privacy
              </a>
              <a href="#" className="text-xs text-retro-cream/50">
                Contact
              </a>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default LandingPage
