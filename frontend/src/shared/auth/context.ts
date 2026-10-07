import { createContext } from 'react'
import type { Role } from '../api/types'
export type Account = { id: number; email: string; role: Role; name: string | null }
export type Registration = { email: string; password: string; name: string; school: string; programme: string; yearOfStudy: number; contactNumber: string }
export type AuthContextValue = { account: Account | null; loading: boolean; error?: string; retry: () => void;
  login: (email: string, password: string) => Promise<Account>; register: (input: Registration) => Promise<Account>; logout: () => void }
export const AuthContext = createContext<AuthContextValue | null>(null)
