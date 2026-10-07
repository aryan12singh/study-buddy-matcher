import { useEffect, useRef, useState } from 'react'
import Button from './Button'

export default function CopyButton({ value, label = 'Copy', sensitive = false }: {
  value: string; label?: string; sensitive?: boolean
}) {
  const [state, setState] = useState<{ value: string; result: 'pending' | 'copied' | 'manual' }>()
  const current = useRef<string | null>(value)
  useEffect(() => {
    current.current = value
    return () => { current.current = null }
  }, [value])
  const result = state?.value === value ? state.result : undefined
  async function copy() {
    setState({ value, result: 'pending' })
    try {
      if (!navigator.clipboard?.writeText) throw new Error('Clipboard unavailable')
      await navigator.clipboard.writeText(value)
      if (current.current === value) setState({ value, result: 'copied' })
    } catch {
      if (current.current === value) setState({ value, result: 'manual' })
    }
  }
  return <div className="copy-control">
    <Button disabled={result === 'pending' || !value} onClick={copy}>
      {result === 'pending' ? 'Copying…' : label}
    </Button>
    {result === 'copied' && <span className="copy-feedback" role="status">Copied.</span>}
    {result === 'manual' && <p className="field-hint" role="status">
      {sensitive ? 'Copy was unavailable. Select the contact number above and copy it manually.' : 'Copy was unavailable. Select and copy the link below.'}
      {!sensitive && <span className="copy-fallback">{value}</span>}
    </p>}
  </div>
}
