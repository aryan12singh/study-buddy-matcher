import Button from './Button'

export default function FilterSummary({ filters, onClear, result }: {
  filters: { key: string; label: string; onRemove: () => void }[]
  onClear: () => void
  result?: string
}) {
  return <div className="filter-summary">
    {result && <p className="row-meta">{result}</p>}
    {filters.length > 0 && <div className="filter-chips" role="group" aria-label="Active filters">
      {filters.map(filter => <button key={filter.key} className="filter-chip" type="button" onClick={filter.onRemove}
        aria-label={`Remove ${filter.label} filter`}>
        {filter.label} <span aria-hidden="true">×</span>
      </button>)}
      <Button onClick={onClear}>Clear filters</Button>
    </div>}
  </div>
}
