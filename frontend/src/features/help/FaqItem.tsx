import { useId, useState } from 'react'
import type { Faq } from './helpContent'

/** One question as a disclosure: the button reports its state and controls the answer. */
export default function FaqItem({ faq }: { faq: Faq }) {
  const [open, setOpen] = useState(false)
  const answerId = useId()
  return (
    <li className={`help-faq-item${open ? ' is-open' : ''}`}>
      <h3>
        <button type="button" className="help-faq-toggle" aria-expanded={open} aria-controls={answerId} onClick={() => setOpen(value => !value)}>
          <span className="help-faq-chevron" aria-hidden="true" />
          <span>{faq.question}</span>
        </button>
      </h3>
      <div id={answerId} className="help-faq-answer" hidden={!open}>
        <p>{faq.answer}</p>
      </div>
    </li>
  )
}
