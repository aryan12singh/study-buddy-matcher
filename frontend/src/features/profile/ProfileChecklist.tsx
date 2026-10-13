import Badge from '../../shared/components/Badge'

/** Shown after registration so a new student sees what is left before matching works well. */
export default function ProfileChecklist({ about, preferences, availability }: { about: boolean; preferences: boolean; availability: boolean }) {
  const steps = [
    { title: '1. About you', detail: 'Saved at sign-up', done: about },
    { title: '2. Study preferences', detail: 'Courses, mode, group size, goals', done: preferences },
    { title: '3. Weekly availability', detail: 'When you can usually study', done: availability }
  ]
  return (
    <ol className="stats-strip" aria-label="Profile progress" style={{ listStyle: 'none' }}>
      {steps.map(step => (
        <li key={step.title} className="stat">
          <strong style={{ fontSize: '1rem' }}>{step.title}</strong>
          <span>{step.detail}</span>{' '}
          <Badge tone={step.done ? 'good' : 'pending'}>{step.done ? 'Done' : 'To do'}</Badge>
        </li>
      ))}
    </ol>
  )
}
