import { lazy } from 'react'

// The E2 screens (room audio, weather scenes, scheduling) load on first visit to keep the main bundle small.
export const StudyRoomPage = lazy(() => import('./features/studyroom/StudyRoomPage'))
export const StudyRoomsPage = lazy(() => import('./features/studyroom/StudyRoomsPage'))
export const SessionsPage = lazy(() => import('./features/sessions/SessionsPage'))
export const SessionDetailPage = lazy(() => import('./features/sessions/SessionDetailPage'))
export const SessionFormPage = lazy(() => import('./features/sessions/SessionFormPage'))
export const AchievementsPage = lazy(() => import('./features/achievements/AchievementsPage'))
