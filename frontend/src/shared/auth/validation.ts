export type AccountForm = { email: string; password?: string; name: string; school: string; programme: string; yearOfStudy: string; contactNumber: string }
export function validateAccountFields(value: AccountForm, student: boolean, creating: boolean) {
  const errors: Record<string, string> = {}
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.email.trim()) || value.email.trim().length > 255) errors.email = 'Enter a valid email address, up to 255 characters.'
  if (creating && (!value.password || value.password.length < 8 || new TextEncoder().encode(value.password).length > 72)) errors.password = 'Use at least 8 characters and at most 72 UTF-8 bytes.'
  if (student) {
    for (const field of ['name', 'school', 'programme'] as const) if (!value[field].trim() || value[field].trim().length > 255) errors[field] = 'Enter a value, up to 255 characters.'
    if (!Number.isInteger(Number(value.yearOfStudy)) || Number(value.yearOfStudy) < 1 || Number(value.yearOfStudy) > 2147483647) errors.yearOfStudy = 'Enter a whole year of study of at least 1.'
    if ((creating && !value.contactNumber.trim()) || value.contactNumber.trim().length > 255) errors.contactNumber = creating ? 'Enter a contact number, up to 255 characters.' : 'Use up to 255 characters.'
  }
  return errors
}
