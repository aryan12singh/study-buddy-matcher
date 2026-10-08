import { createRoutesFromElements, Link, Route } from 'react-router-dom'
import LoginPage from './features/auth/LoginPage'
import RegisterPage from './features/auth/RegisterPage'
import LandingPage from './features/landing/LandingPage'
import AccountHome from './shared/auth/AccountHome'
import RequireRole from './shared/auth/RequireRole'
import AppShell from './shared/components/AppShell'
import WindowPage from './shared/components/WindowPage'

export const appRoutes = createRoutesFromElements(
    <>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<RequireRole />}>
        <Route element={<AppShell />}>
          <Route path="/app" element={<AccountHome />} />
          <Route
            path="*"
            element={
              <WindowPage title="Page unavailable">
                <p>This link does not match an available page.</p>
                <Link to="/app">Return to your account</Link>
              </WindowPage>
            }
          />
        </Route>
      </Route>
    </>
  )
