/** Upload limits for quizzes and study plans, matching app.ai.assistant.study-plan.*; the server checks them again. */
export const MAX_FILES = 6
export const MAX_UPLOAD_BYTES = 10 * 1024 * 1024
export const MAX_NOTES_CHARACTERS = 4000
export const ACCEPTED_EXTENSIONS = ['.pdf', '.txt', '.md']

/** Says what is wrong with a set of files before anything is sent, or nothing when they are fine. */
export function checkFiles(files: File[]): string | undefined {
  if (!files.length) return 'Choose at least one file.'
  if (files.length > MAX_FILES) return `Upload at most ${MAX_FILES} files.`
  const wrong = files.find(file => !ACCEPTED_EXTENSIONS.some(extension => file.name.toLowerCase().endsWith(extension)))
  if (wrong) return `${wrong.name} is not a PDF, .txt or .md file. Export Word or PowerPoint files to PDF first.`
  const empty = files.find(file => file.size === 0)
  if (empty) return `${empty.name} is empty.`
  if (files.reduce((total, file) => total + file.size, 0) > MAX_UPLOAD_BYTES) return 'The files must add up to at most 10MB.'
  return undefined
}
