import { validateContactNumber } from './contactNumber'
import { validateSchool, validateYearOfStudy } from './studentDetails'

export type AccountForm = { email: string; password?: string; name: string; school: string; programme: string; yearOfStudy: string; contactNumber: string }
export function validateAccountFields(value: AccountForm, student: boolean, creating: boolean) {
  const errors: Record<string, string> = {}
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.email.trim()) || value.email.trim().length > 255) errors.email = 'Enter a valid email address, up to 255 characters.'
  if (creating && (!value.password || value.password.length < 8)) errors.password = 'Please enter a value, of at least 8 characters'
  else if (creating && new TextEncoder().encode(value.password!).length > 72) errors.password = 'Use at most 72 UTF-8 bytes.'
  if (student) {
    for (const field of ['name', 'programme'] as const) {
      if (!value[field].trim()) errors[field] = 'Please enter a value'
      else if (value[field].trim().length > 255) errors[field] = 'Use up to 255 characters.'
    }
    const schoolError = validateSchool(value.school)
    if (schoolError) errors.school = schoolError
    const yearError = validateYearOfStudy(value.yearOfStudy)
    if (yearError) errors.yearOfStudy = yearError
    if (creating || value.contactNumber.trim()) {
      const contactError = validateContactNumber(value.contactNumber)
      if (contactError) errors.contactNumber = contactError
    }
  }
  return errors
}
