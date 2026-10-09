import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getStudentProfile } from './api'
import { decideMatchRequest, endConnection } from '../connections/api'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import { label } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Button from '../../shared/components/Button'
import Badge from '../../shared/components/Badge'
import ActionNotice from '../../shared/components/ActionNotice'
import WeeklySchedule from '../../shared/components/WeeklySchedule'
import SendRequestDialog from '../connections/SendRequestDialog'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import Avatar from '../../shared/components/Avatar'
import CopyButton from '../../shared/components/CopyButton'

export default function StudentProfilePage() {
  const { id } = useParams(),
    studentId = Number(id)
  const validId = Number.isSafeInteger(studentId) && studentId > 0
  const profile = useResource(`student-${id}`, signal => getStudentProfile(studentId, signal), validId)
  const action = useAction()
  const [requestRecipient, setRequestRecipient] = useState<{ id: number; name: string } | null>(null)
  const [ending, setEnding] = useState(false)
  const student = profile.data,
    relationship = student?.relationship

  async function disconnect() {
    if (!relationship?.connectionId) return
    const result = await action.run(() => endConnection(relationship.connectionId!), 'Connection ended.', true)
    if (result.ok) setEnding(false)
  }
  return (
    <WindowPage
      title={student?.name ? `${student.name}'s study profile` : 'Student profile'}
      description="Courses, goals and weekly availability."
      actions={(
        <>
          {relationship?.state === 'SELF' && <Link className="retro-button primary" to={`/students/${studentId}/edit`}>Edit my profile</Link>}
          <Link className="retro-button" to="/connections">Back to connections</Link>
        </>
      )}
    >
      <StatePanel
        loading={profile.loading && !student}
        error={!validId ? 'This student link is invalid.' : profile.error}
        onRetry={validId ? () => profile.reload(true) : undefined}
      />
      <ActionNotice error={ending ? undefined : action.error} success={action.success} />
      {student && (
        <>
          <div className="detail-grid">
            <section className="detail-panel">
              <div className="person-heading"><Avatar name={student.name} /><h2>{student.name}</h2></div>
              <dl className="detail-facts">
                <dt>School</dt>
                <dd>{student.school}</dd>
                <dt>Programme</dt>
                <dd>{student.programme}</dd>
                <dt>Year of study</dt>
                <dd>{student.yearOfStudy}</dd>
                <dt>Study mode</dt>
                <dd>{label(student.preferredStudyMode)}</dd>
                <dt>Preferred group size</dt>
                <dd>
                  {student.preferredGroupSizeMin == null ?
                    'Not specified'
                    :
                    student.preferredGroupSizeMin === student.preferredGroupSizeMax ?
                      student.preferredGroupSizeMin
                      :
                      `${student.preferredGroupSizeMin}–${student.preferredGroupSizeMax}`}
                </dd>
              </dl>
            </section>
            <section className="detail-panel">
              <h2>Buddy connection</h2>
              {relationship?.state === 'SELF' && (
                <p>You are viewing your own study profile.</p>
              )}
              {relationship?.state === 'STRANGER' && (
                <>
                  <p>Send a request to start studying together.</p>
                  <Button
                    variant="primary"
                    onClick={() => setRequestRecipient({ id: student.id, name: student.name })}
                  >
                    Send match request
                  </Button>
                </>
              )}
              {relationship?.state === 'OUTGOING_PENDING' && (
                <>
                  <Badge tone="pending">Request sent</Badge>
                  <p>Waiting for {student.name} to respond.</p>
                  <Link to="/connections">View your requests</Link>
                </>
              )}
              {relationship?.state === 'INCOMING_PENDING' && (
                <>
                  <Badge tone="pending">Incoming request</Badge>
                  <p>{student.name} would like to connect.</p>
                  <div className="actions">
                    <Button
                      variant="primary"
                      disabled={action.pending}
                      onClick={() => action.run(() => decideMatchRequest(relationship.requestId!, 'accept'), 'Request accepted.', true)}
                    >
                      Accept request
                    </Button>
                    <Button
                      disabled={action.pending}
                      onClick={() => action.run(() => decideMatchRequest(relationship.requestId!, 'decline'), 'Request declined.')}
                    >Decline request</Button>
                  </div>
                </>
              )}
              {relationship?.state === 'CONNECTED' && (
                <>
                  <Badge tone="good">Connected buddy</Badge>
                  <p>You can now contact each other.</p>
                  <Button
                    variant="danger"
                    disabled={action.pending}
                    onClick={() => {
                      action.clear()
                      setEnding(true)
                    }}
                  >
                    Disconnect
                  </Button>
                </>
              )}
              {'contactNumber' in student && (relationship?.state === 'CONNECTED' || relationship?.state === 'SELF') ? (
                <div className="privacy-note">
                  <h3>Contact number</h3>
                  <p>{student.contactNumber || 'No contact number provided.'}</p>
                  {student.contactNumber && <CopyButton value={student.contactNumber} label="Copy contact number" sensitive />}
                </div>
              ) : (
                <p className="privacy-note">Contact number is private until a buddy request is accepted. Group membership does not share contact numbers.</p>
              )}
            </section>
          </div>
          <section className="detail-panel">
            <h2>Courses and goals</h2>
            <h3>Courses taken</h3>
            {student.coursesTaken.length ? (
              <ul>{student.coursesTaken.map(course => (
                <li key={course.id}>{course.code} — {course.name}</li>
              ))}</ul>
            ) : (
              <p className="muted">No courses listed.</p>
            )}
            <h3>Target course</h3>
            <p>
              {student.targetCourse ? `${student.targetCourse.code} — ${student.targetCourse.name}` : 'No target course selected.'}
            </p>
            <h3>Study goals</h3>
            <div className="tag-list">
              {student.studyGoals.length ?
                student.studyGoals.map(goal => (
                  <Badge key={goal}>{label(goal)}</Badge>
                )) : (
                  <p className="muted">No study goals selected.</p>
                )}
            </div>
          </section>
          <section className="detail-panel">
            <h2>Weekly availability</h2>
            <WeeklySchedule slots={student.availability} />
          </section>
        </>
      )}
      {requestRecipient?.id === studentId &&
        <SendRequestDialog
          receiverId={requestRecipient.id}
          receiverName={requestRecipient.name}
          onClose={() => setRequestRecipient(null)}
        />}
      {ending && (
        <ConfirmDialog
          title={`Disconnect from ${student?.name || 'this student'}?`}
          confirmLabel="Disconnect"
          pending={action.pending}
          error={action.error}
          onClose={() => setEnding(false)}
          onConfirm={disconnect}
        >
          <p>Future profile views will no longer disclose either student's contact number. This ends the accepted buddy connection.</p>
        </ConfirmDialog>
      )}
    </WindowPage>
  )
}
