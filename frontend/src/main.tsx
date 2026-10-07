import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './shared/desktop.css'
import { createBrowserRouter } from 'react-router-dom'
import App from './App.tsx'
import { appRoutes } from './routes'

const router = createBrowserRouter(appRoutes)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App router={router} />
  </StrictMode>,
)
