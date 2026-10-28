import { useRef } from 'react'
import Button from '../../shared/components/Button'
import { ACCEPTED_EXTENSIONS, MAX_FILES, MAX_NOTES_CHARACTERS } from '../quiz/api'
import type { StudyMaterial } from './api'

/**
 * Where a roadmap is drafted from: lecture files to upload, or topics pasted in. Only one kind is
 * sent, so switching keeps the other kind's draft in case the student switches back.
 */
export default function StudyMaterialInput({ kind, files, topicsText, onKindChange, onFilesChange, onTopicsChange, errors, disabled }: {
  kind: StudyMaterial['kind']
  files: File[]
  topicsText: string
  onKindChange: (kind: StudyMaterial['kind']) => void
  onFilesChange: (files: File[]) => void
  onTopicsChange: (text: string) => void
  errors: Record<string, string | undefined>
  disabled: boolean
}) {
  const fileInput = useRef<HTMLInputElement>(null)

  function add(list: FileList | null) {
    // A file chosen twice is kept once, so picking the same slides again does not double them.
    const added = Array.from(list ?? []).filter(file => !files.some(other => other.name === file.name && other.size === file.size))
    if (fileInput.current) fileInput.current.value = ''
    onFilesChange([...files, ...added])
  }

  return <fieldset className="fieldset study-material" disabled={disabled}>
    <legend>Study material</legend>
    <div className="check-options" role="radiogroup" aria-label="Study material">
      <label><input type="radio" name="material-kind" checked={kind === 'files'} onChange={() => onKindChange('files')} />Upload files</label>
      <label><input type="radio" name="material-kind" checked={kind === 'topics'} onChange={() => onKindChange('topics')} />Paste topics</label>
    </div>

    {kind === 'files' ? <div className="form-field">
      <label htmlFor="plan-files">Lecture slides or notes</label>
      <input ref={fileInput} id="plan-files" type="file" multiple accept={ACCEPTED_EXTENSIONS.join(',')}
        aria-describedby={['plan-files-hint', errors.files && 'plan-files-error'].filter(Boolean).join(' ')}
        aria-invalid={Boolean(errors.files)} onChange={event => add(event.target.files)} />
      <p className="field-hint" id="plan-files-hint">PDF, .txt or .md, up to {MAX_FILES} files and 10MB together. Export Word or PowerPoint files to PDF first.</p>
      {!!files.length && <ul className="study-material-files" aria-label="Chosen files">
        {files.map(file => <li key={`${file.name}-${file.size}`}>
          <span>{file.name}</span>
          <Button aria-label={`Remove ${file.name}`} onClick={() => onFilesChange(files.filter(other => other !== file))}>Remove</Button>
        </li>)}
      </ul>}
      {errors.files && <p className="field-error" id="plan-files-error" role="alert">{errors.files}</p>}
    </div> : <div className="form-field">
      <label htmlFor="plan-topics">Topics to cover</label>
      <textarea id="plan-topics" rows={6} value={topicsText} maxLength={MAX_NOTES_CHARACTERS}
        placeholder={'Encapsulation\nInheritance\nPolymorphism'}
        aria-describedby={['plan-topics-hint', errors.topicsText && 'plan-topics-error'].filter(Boolean).join(' ')}
        aria-invalid={Boolean(errors.topicsText)} onChange={event => onTopicsChange(event.target.value)} />
      <p className="field-hint" id="plan-topics-hint">One topic per line works best. {topicsText.length}/{MAX_NOTES_CHARACTERS} characters.</p>
      {errors.topicsText && <p className="field-error" id="plan-topics-error" role="alert">{errors.topicsText}</p>}
    </div>}

    <p className="field-hint">Your material is sent to Google Gemini to draft the roadmap and is not saved. Only the file names are kept with the plan.</p>
  </fieldset>
}
