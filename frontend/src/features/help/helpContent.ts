import type { PixelIconKind } from '../../shared/components/PixelIcon'

export type Explainer = { key: string; title: string; icon: PixelIconKind; summary: string; points: string[] }
export type Faq = { id: string; question: string; answer: string }

/** Plain-language explanations of each main area, written from the README. */
export const EXPLAINERS: Explainer[] = [
  {
    key: 'matching', title: 'Matching', icon: 'search',
    summary: 'Other students get a compatibility score based on how well your study habits fit together.',
    points: [
      'Five criteria: course, shared weekly availability, study mode, study goals and group size.',
      'Each criterion has a weight set by an administrator, and each card shows the breakdown.',
      'The active strategy decides the ranking: balanced, availability first or course first.'
    ]
  },
  {
    key: 'privacy', title: 'Requests and privacy', icon: 'connections',
    summary: 'Your contact number stays hidden until you accept someone, or they accept you.',
    points: [
      'Before a request is accepted, others see your public profile without a contact number.',
      'The server withholds the number itself, so it never reaches another student’s browser.',
      'Ending a connection removes contact access again and cancels its upcoming sessions.'
    ]
  },
  {
    key: 'groups', title: 'Study groups', icon: 'groups',
    summary: 'Groups gather several students around one course, led by the student who created them.',
    points: [
      'Browse open groups and apply with an optional message.',
      'The leader approves or declines applications, and groups have a maximum size.',
      'Leaders can edit the group, remove members and close it.'
    ]
  },
  {
    key: 'rooms', title: 'Study rooms', icon: 'home',
    summary: 'Each open group has a private room for its accepted members to study together.',
    points: [
      'Members share one Pomodoro timer that keeps running through refreshes.',
      'Music and ambience play only after you choose to turn them on; volume stays on your device.',
      'A pixel weather scene follows live campus conditions.'
    ]
  },
  {
    key: 'sessions', title: 'Study sessions', icon: 'clock',
    summary: 'Plan one-off or weekly sessions with a group or a connected buddy.',
    points: [
      'Suggested times fit inside everyone’s shared availability; you can also pick a time yourself.',
      'Upcoming sessions appear on your Home page.',
      'Download any session as a calendar file (.ics) for your own calendar app.'
    ]
  },
  {
    key: 'notifications', title: 'Notifications', icon: 'notifications',
    summary: 'Updates about your requests, groups, memberships and sessions collect in one place.',
    points: [
      'The navigation shows how many you have not read yet.',
      'Each notification links to the request, group or session it is about.'
    ]
  }
]

export const FAQS: Faq[] = [
  {
    id: 'contact', question: 'When can other students see my contact number?',
    answer: 'Only after a match request between you is accepted. Being in the same study group does not reveal it, and neither does an administrator viewing your account. If either of you ends the connection, the number is hidden again.'
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
    answer: 'Matching needs your courses and weekly availability, so finish both in your profile first. Students below the minimum score set by an administrator are not shown, and search filters such as course or study mode can narrow the list further.'
  },
  {
    id: 'explain', question: 'What does “Explain this match” do?',
    answer: 'It can ask an AI model to describe, in plain words, why a match scored the way it did. AI text is labelled as possibly inaccurate. If AI is turned off or its daily allowance is used up, you see the standard explanation instead. Scores never depend on it.'
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
    answer: 'Only accepted members of an open group. If you leave or are removed, or the group closes, room access ends immediately.'
  },
  {
    id: 'schedule', question: 'Who can schedule a study session?',
    answer: 'Group leaders schedule sessions for their group, and connected buddies can schedule sessions with each other. Suggested times come from everyone’s shared availability.'
  },
  {
    id: 'calendar', question: 'Does the calendar export stay up to date?',
    answer: 'No. The .ics file is a snapshot of the session when you downloaded it. If the session changes, download it again.'
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
