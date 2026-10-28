export const CONTACT_NUMBER_HINT = 'For example 9123 4567 or +1 202 555 0123. Use 8 Singapore digits starting with 6, 8 or 9 (with or without +65), or + and the country code with 7 to 15 digits. Spaces, hyphens, parentheses and dots are allowed.'
export const CONTACT_NUMBER_ERROR = 'Enter 8 Singapore digits starting with 6, 8 or 9 (with or without +65), or + followed by 7 to 15 international digits starting with 1 to 9.'

/** Mirrors InputRules: normalize common separators and add +65 to local Singapore input. */
export function normalizeContactNumber(value: string) {
  const normal = value.replace(/[ \t\n\r\f\v().-]/g, '')
  return /^[689][0-9]{7}$/.test(normal) ? `+65${normal}` : normal
}

/** A +65 number must be a full Singapore number, even though shorter numbers fit the international length. */
export function validateContactNumber(value: string) {
  const normal = normalizeContactNumber(value)
  if (value.length > 255 || !/^\+[1-9][0-9]{6,14}$/.test(normal)) return CONTACT_NUMBER_ERROR
  if (normal.startsWith('+65') && !/^\+65[689][0-9]{7}$/.test(normal)) return CONTACT_NUMBER_ERROR
  return undefined
}
