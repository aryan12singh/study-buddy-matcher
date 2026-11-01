import type { Course } from '../../shared/api/types'

/** A course code typed anywhere in a message, such as "IS442" or "is 212". */
export const COURSE_CODE = /\b([a-z]{2,4})\s?(\d{3}[a-z]?)\b/i

/** The course a student named, by code or by name; null when nothing matches. */
export function findCourse(text: string, courses: Course[]): Course | null {
  const code = COURSE_CODE.exec(text)
  if (code) {
    const typed = (code[1] + code[2]).toUpperCase()
    const byCode = courses.find(course => course.code.replace(/\s/g, '').toUpperCase() === typed)
    if (byCode) return byCode
  }
  const words = text.trim().toLowerCase()
  if (words.length < 3) return null
  return courses.find(course => course.name.toLowerCase().includes(words) || words.includes(course.name.toLowerCase())) ?? null
}

/** The courses to suggest first, in this order, followed by the rest in catalogue order. */
export function preferCourses(courses: Course[], preferred: (Course | null | undefined)[]): Course[] {
  const ids: number[] = []
  for (const course of preferred) if (course && !ids.includes(course.id)) ids.push(course.id)
  const first = ids.flatMap(id => courses.filter(course => course.id === id))
  return [...first, ...courses.filter(course => !ids.includes(course.id))]
}
