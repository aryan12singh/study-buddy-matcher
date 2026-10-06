import { render } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { vi } from 'vitest'
import { AuthContext } from '../shared/auth/context'
import type { Account } from '../shared/auth/context'

export const studentAccount: Account = { id: 1, email: 'student@example.test', name: 'Priya', role: 'STUDENT' }
export const adminAccount: Account = { id: 99, email: 'admin@example.test', name: null, role: 'ADMIN' }
export function renderPage(page: ReactNode, path = '/', route = '/', account = studentAccount) {
  const auth = { account, loading: false, login: vi.fn(), register: vi.fn(), logout: vi.fn(), retry: vi.fn() }
  return { ...render(<AuthContext.Provider value={auth}><MemoryRouter initialEntries={[path]}><Routes><Route path={route} element={page} /><Route path="*" element={<p>Destination page</p>} /></Routes></MemoryRouter></AuthContext.Provider>), auth }
}
export function response<T>(data: T) { return { data } as never }
export function deferred<T>() {
  let resolve!: (value: T) => void, reject!: (reason?: unknown) => void
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
