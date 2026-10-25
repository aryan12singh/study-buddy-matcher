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
