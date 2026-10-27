/** Mirrors the backend SmuSchool enum: the app is for SMU students, so school is one of these codes. */
export const SMU_SCHOOLS = [
  { code: 'SOA', name: 'School of Accountancy' },
  { code: 'LKCSB', name: 'Lee Kong Chian School of Business' },
  { code: 'SOE', name: 'School of Economics' },
  { code: 'SCIS', name: 'School of Computing and Information Systems' },
  { code: 'YPHSL', name: 'Yong Pung How School of Law' },
  { code: 'SOSS', name: 'School of Social Sciences' },
  { code: 'CIS', name: 'College of Integrative Studies' }
] as const

/** Mirrors app.profile.year-of-study-max; the server still has the final say. */
export const YEAR_OF_STUDY_MAX = 6

export const PROGRAMME_HINT = 'Your degree programme, for example Information Systems.'

export function validateSchool(value: string) {
  if (!value.trim()) return 'Please enter a value'
  if (!SMU_SCHOOLS.some(school => school.code === value)) return 'Choose your school from the list.'
  return undefined
}

export function validateYearOfStudy(value: string) {
  const year = Number(value)
  if (!value.trim() || !Number.isInteger(year) || year < 1 || year > YEAR_OF_STUDY_MAX) return `Enter a whole year of study from 1 to ${YEAR_OF_STUDY_MAX}.`
  return undefined
}
