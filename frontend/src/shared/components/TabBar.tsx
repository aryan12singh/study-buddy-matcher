export default function TabBar<T extends string>({ value, options, onChange, label }: {
  value: T
  options: { value: T; label: string }[]
  onChange: (value: T) => void
  label: string
}) {
  return (
    <div
      className="tab-bar"
      role="group"
      aria-label={label}
    >
      {options.map(option =>
      (
        <button
          type="button"
          key={option.value}
          aria-pressed={option.value === value}
          onClick={() => onChange(option.value)}
        >{option.label}</button>
      ))}
    </div>
  )
}
