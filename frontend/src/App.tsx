import { RouterProvider, useRoutes } from 'react-router-dom'
import type { RouterProviderProps } from 'react-router-dom'
import AuthProvider from './shared/auth/AuthProvider'
import { appRoutes } from './routes'

export function AppRoutes() {
  return useRoutes(appRoutes)
}

export default function App({ router }: { router: RouterProviderProps['router'] }) {
  return <AuthProvider><RouterProvider router={router} /></AuthProvider>
}
