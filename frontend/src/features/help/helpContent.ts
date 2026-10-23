import type { PixelIconKind } from '../../shared/components/PixelIcon'

export type Explainer = { key: string; title: string; icon: PixelIconKind; summary: string; points: string[]; to: string; action: string }
export type Faq = { id: string; question: string; answer: string }

/** Plain-language explanations of each main area, written from the README. */
export const EXPLAINERS: Explainer[] = [
  {
    key: 'matching', title: 'Matching', icon: 'search',
    summary: 'Matching calculates compatibility from saved profiles and your chosen course or study goal. The ranking is deterministic.',
    points: [
      'Five criteria: course, shared weekly availability, study mode, study goals and group size.',
      'An administrator chooses enabled criteria, weights, a minimum score and the ranking strategy. Each card shows the breakdown.',
      'Your filters narrow the search without changing your saved profile or the administrator’s settings. AI never sets the score or ranking.'
    ], to: '/find-buddies', action: 'Compare study buddies'
  },
  {
    key: 'ai', title: 'AI assistance and its limits', icon: 'search',
    summary: 'Explain this match can use Gemini to word an explanation of an existing score. It is optional assistance.',
    points: [
      'AI wording is labelled and may be inaccurate. Check it against the numeric breakdown and shared hours; output checks cannot prove every statement.',
      'The provider receives anonymous score, course and study-preference evidence. Names, contact details and exact weekly schedules are excluded.',
      'When AI is disabled, limited or unavailable, a labelled standard explanation is used. If the match changed, refresh it before asking again.'
    ], to: '/find-buddies', action: 'See match scores and explanations'
  },
  {
    key: 'privacy', title: 'Requests and privacy', icon: 'connections',
    summary: 'Your contact number stays hidden until you accept someone, or they accept you.',
    points: [
      'Before a request is accepted, others see your public profile without a contact number.',
      'The server withholds the number from public profile responses. Sharing a group or room does not reveal it.',
      'Disconnecting removes future contact access and cancels upcoming buddy sessions. It cannot erase information someone already saved.'
    ], to: '/connections', action: 'Manage requests and connections'
  },
  {
    key: 'groups', title: 'Study groups', icon: 'groups',
    summary: 'Groups gather several students around one course, led by the student who created them.',
    points: [
      'Browse open groups and apply with an optional message.',
      'The leader approves or declines applications, and groups have a maximum size.',
      'Accepted members of an open group can enter its room and view its dated session agenda. The leader plans group sessions.'
    ], to: '/groups', action: 'Find a study group'
  },
  {
    key: 'rooms', title: 'Study rooms', icon: 'home',
    summary: 'Each open group has a private room for its accepted members to study together.',
    points: [
      'Open a group’s room preview and choose Join study room explicitly. Opening a group or session link does not join you.',
      'The shared Pomodoro keeps running through refreshes. The leader and authorized hosts control it; music and ambience need your opt-in, with volume local to your device.',
      'Weather scenes can use campus conditions, with selectable scenery when weather is unavailable. Leaving the group or closing it ends room access.'
    ], to: '/groups?view=mine', action: 'Open your groups to find a room'
  },
  {
    key: 'sessions', title: 'Study sessions', icon: 'clock',
    summary: 'Plan one-off or weekly sessions with a group or a connected buddy.',
    points: [
      'Leaders plan from their group’s dated agenda; buddies use Plan study sessions under Connected buddies. Suggestions use shared availability, and manual times are allowed.',
      'Plans appear on Home and Your upcoming sessions. A group session can link to its private room; membership and explicit joining are still required.',
      'Download a saved session as a calendar file (.ics). Re-export after edits or cancellation: downloads do not sync or prove attendance.'
    ], to: '/sessions', action: 'View your dated study plans'
  },
  {
    key: 'notifications', title: 'Notifications', icon: 'notifications',
    summary: 'Updates about your requests, groups, memberships and sessions collect in one place.',
    points: [
      'The navigation shows how many you have not read yet.',
      'Each notification links to the request, group or session it is about.'
    ], to: '/notifications', action: 'Read your notifications'
  }
]

export const FAQS: Faq[] = [
  {
    id: 'contact', question: 'When can other students see my contact number?',
    answer: 'Only after a match request between you is accepted. Being in the same study group or room does not reveal it, and neither does an administrator viewing your account. If either of you ends the connection, future profile responses hide it again; information someone already saved cannot be recalled.'
  },
  {
    id: 'score', question: 'How is my match score worked out?',
    answer: 'The score combines five criteria: shared courses, overlapping weekly availability, study mode, study goals and preferred group size. Each criterion is weighted by an administrator, and the bars on a match card show how much each one contributed.'
  },
  {
    id: 'strategy', question: 'What do balanced, availability first and course first mean?',
    answer: 'They are ranking strategies chosen by an administrator. Balanced ranks by the weighted total. Availability first groups students by how many hours you share each week, and course first groups them by how closely your courses match, before ranking by the total within each group.'
  },
  {
    id: 'no-matches', question: 'Why am I not seeing any matches?',
    answer: 'Save your courses, study mode and weekly availability, then choose a course or study goal for the search. Students below the administrator’s minimum score are not shown. Filters can narrow the list further, and existing connected buddies are excluded. Try fewer filters or another course or goal; an empty search does not mean your profile is broken.'
  },
  {
    id: 'explain', question: 'What does “Explain this match” do?',
    answer: 'It can ask Gemini to describe the evidence behind the selected score. AI wording is labelled and may be inaccurate; it does not change the deterministic score or ranking. A labelled standard explanation appears when AI is disabled, its allowance is exhausted or wording is unavailable. A changed match needs a refresh before another explanation.'
  },
  {
    id: 'ai-data', question: 'What data is sent for an AI explanation?',
    answer: 'The server sends anonymous compatibility evidence such as scores, relevant course details, shared hours and study preferences. It excludes names, account IDs, contact details, school, year of study and exact weekly schedules. This evidence still leaves the app for an external provider when AI wording is requested and available.'
  },
  {
    id: 'group-size', question: 'What does preferred group size change?',
    answer: 'You choose one-to-one, small group or either. Matching scores how well your choice overlaps with another student’s, so someone open to either fits both kinds of student.'
  },
  {
    id: 'end-connection', question: 'Can I end a connection?',
    answer: 'Yes. Open the Connected tab on your Connections page and choose Disconnect, then confirm. Ending it removes contact access for both of you and cancels any upcoming sessions you planned together. Your history is kept.'
  },
  {
    id: 'join-group', question: 'How do I join a study group?',
    answer: 'Open Groups, choose a group and apply, optionally with a short message. The group leader decides. You get a notification when they decide, and a full group cannot take new members.'
  },
  {
    id: 'room', question: 'Who can enter a group’s study room?',
    answer: 'Only accepted members of an open group. Open Your groups, choose the group and open its study room preview, then select Join study room. A pending group application or a session link does not grant access. If you leave or are removed, or the group closes, room access ends immediately.'
  },
  {
    id: 'room-audio', question: 'Why is a study room silent, or why can I not control the timer?',
    answer: 'Join the room and choose to enable music or ambience in your browser; sound does not start automatically. Volume and mute stay on your device, and you can study silently if playback fails. The group leader and authorized host or co-host control the shared timer; other members can view it and set their own focus or break status.'
  },
  {
    id: 'schedule', question: 'Who can schedule a study session?',
    answer: 'Group leaders open their group’s dated session agenda and choose Plan study session. Connected buddies use Plan study sessions on their Connections card. Other accepted group members can view the agenda and ask the leader to plan. Choose a suggested or manual time, add meeting details and save. Weekly profile availability is recurring free time, not a booked session.'
  },
  {
    id: 'calendar', question: 'Does the calendar export stay up to date?',
    answer: 'No. Open a saved session and download its .ics calendar file, then import it into your calendar app. The file is a snapshot, including its time zone and weekly occurrences. Re-export after edits or cancellation and manage the older copy in your calendar; there is no live calendar sync.'
  },
  {
    id: 'guide-progress', question: 'Why does this guide show an unchecked or untracked step?',
    answer: 'Profile, request, open-group membership and dated-session checks use current records. A past declined request, closed group or ended session may no longer satisfy a current check. Loading and failed checks are labelled separately. Searches, room visits, calendar downloads and attendance have no completion history in this guide, so they are never marked done from another action.'
  },
  {
    id: 'availability-time', question: 'Which time zone is my availability in?',
    answer: 'Weekly availability is recorded in Singapore time. Slots that only touch end to start, such as 09:00–10:00 and 10:00–11:00, do not count as shared time.'
  }
]

/** Case-insensitive match on the question and answer; every word must appear. */
export function filterFaqs(faqs: Faq[], query: string) {
  const words = query.trim().toLowerCase().split(/\s+/).filter(Boolean)
  if (!words.length) return faqs
  return faqs.filter(faq => {
    const text = `${faq.question} ${faq.answer}`.toLowerCase()
    return words.every(word => text.includes(word))
  })
}
