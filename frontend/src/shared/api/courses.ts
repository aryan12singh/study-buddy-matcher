import { api } from './client'
import type { Course } from './types'
export async function getCourses(signal?: AbortSignal) { return (await api.get<Course[]>('/courses', { signal })).data }
