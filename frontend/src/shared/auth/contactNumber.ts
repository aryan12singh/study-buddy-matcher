export const CONTACT_NUMBER_HINT = 'Use 8 Singapore digits starting with 6, 8 or 9, or + and the country code with 7 to 15 digits. Spaces, hyphens, parentheses and dots are allowed.'
export const CONTACT_NUMBER_ERROR = 'Enter 8 Singapore digits starting with 6, 8 or 9, or + followed by 7 to 15 international digits starting with 1 to 9.'

/** Mirrors InputRules: normalize common separators and add +65 to local Singapore input. */
export function normalizeContactNumber(value: string) {
  const normal = value.replace(/[ \t\n\r\f\v().-]/g, '')
  return /^[689][0-9]{7}$/.test(normal) ? `+65${normal}` : normal
}

export function validateContactNumber(value: string) {
  if (value.length > 255 || !/^\+[1-9][0-9]{6,14}$/.test(normalizeContactNumber(value))) return CONTACT_NUMBER_ERROR
  return undefined
}
