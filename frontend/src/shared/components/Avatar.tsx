export default function Avatar({ name, small = false }: { name: string; small?: boolean }) {
  const words = name.trim().split(/\s+/).filter(Boolean)
  const initials = words.length > 1
    ? `${Array.from(words[0])[0]}${Array.from(words[words.length - 1])[0]}`
    : Array.from(words[0] || '?').slice(0, 2).join('')
  const tone = Array.from(name).reduce((total, character) => total + character.codePointAt(0)!, 0) % 4
  return <span className={`initials-avatar avatar-tone-${tone}${small ? ' avatar-small' : ''}`} aria-hidden="true">
    {initials.toLocaleUpperCase()}
  </span>
}
