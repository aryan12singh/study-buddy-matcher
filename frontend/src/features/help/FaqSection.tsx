import { useRef, useState } from 'react'
import Button from '../../shared/components/Button'
import StatePanel from '../../shared/components/StatePanel'
import FaqItem from './FaqItem'
import { FAQS, filterFaqs } from './helpContent'

export default function FaqSection() {
  const [query, setQuery] = useState('')
  const searchInput = useRef<HTMLInputElement>(null)
  const results = filterFaqs(FAQS, query)
  const searching = query.trim().length > 0
  return (
    <section className="help-section" aria-labelledby="help-faq-title">
      <div className="help-section-head">
        <div>
          <span className="help-eyebrow">Questions</span>
          <h2 id="help-faq-title" tabIndex={-1}>Frequently asked questions</h2>
        </div>
      </div>
      <div className="help-search">
        <label htmlFor="help-faq-search">Search questions</label>
        <input
          id="help-faq-search"
          ref={searchInput}
          type="search"
          value={query}
          placeholder="Try “contact number” or “calendar”"
          onChange={event => setQuery(event.target.value)}
        />
        <p className="help-search-count" role="status">
          {searching ? `${results.length} of ${FAQS.length} questions shown` : `${FAQS.length} questions`}
        </p>
      </div>
      {results.length ? (
        <ul className="help-faq-list">
          {results.map(faq => <FaqItem key={faq.id} faq={faq} />)}
        </ul>
      ) : (
        <StatePanel
          empty
          emptyKind="search"
          emptyTitle={`No answers match “${query.trim()}”`}
          emptyMessage="Try a shorter phrase, such as “groups” or “sessions”."
          emptyAction={<Button onClick={() => { setQuery(''); searchInput.current?.focus() }}>Clear search</Button>}
        />
      )}
    </section>
  )
}
