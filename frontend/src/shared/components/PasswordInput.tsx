import { useState } from 'react'
import type { InputHTMLAttributes } from 'react'
import Button from './Button'

/** A password input with a button that reveals or hides what has been typed. */
export default function PasswordInput(props: Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>) {
  const [visible, setVisible] = useState(false)
  return (
    <div className="password-input">
      <input {...props} type={visible ? 'text' : 'password'} />
      <Button
        aria-controls={props.id}
        aria-pressed={visible}
        disabled={props.disabled}
        onClick={() => setVisible(shown => !shown)}
      >{visible ? 'Hide password' : 'Show password'}</Button>
    </div>
  )
}
