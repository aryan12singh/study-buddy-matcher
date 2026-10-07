import type { ButtonHTMLAttributes } from 'react'

type Props = ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger' }

export default function Button({ variant = 'secondary', className = '', type = 'button', ...props }: Props) {
  return <button
    type={type}
    className={`retro-button ${variant} ${className}`}
    {...props}
  />
}
