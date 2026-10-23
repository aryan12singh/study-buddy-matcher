-- Applicants can withdraw a pending application, so WITHDRAWN joins the leader's answers.
-- Deactivation also withdraws, so the history never shows a rejection the leader did not give.
ALTER TABLE public.group_join_requests DROP CONSTRAINT IF EXISTS group_join_requests_status_check;
ALTER TABLE public.group_join_requests ADD CONSTRAINT group_join_requests_status_check CHECK(status IN (
    'PENDING','ACCEPTED','REJECTED','WITHDRAWN'));

-- Leaders are told when an applicant withdraws or a member leaves of their own accord.
-- This is the current list and is validated against every row; earlier files add theirs NOT VALID.
ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check CHECK(type IN (
    'MATCH_REQUEST_RECEIVED','MATCH_REQUEST_ACCEPTED','MATCH_REQUEST_DECLINED','MATCH_REQUEST_CANCELLED',
    'CONNECTION_ENDED','GROUP_JOIN_REQUEST_RECEIVED','GROUP_JOIN_REQUEST_ACCEPTED',
    'GROUP_JOIN_REQUEST_REJECTED','GROUP_JOIN_REQUEST_WITHDRAWN','GROUP_MEMBER_REMOVED',
    'GROUP_MEMBER_LEFT','GROUP_CLOSED',
    'GROUP_SESSION_CREATED','GROUP_SESSION_UPDATED','GROUP_SESSION_CANCELLED',
    'BUDDY_SESSION_CREATED','BUDDY_SESSION_UPDATED','BUDDY_SESSION_CANCELLED'));

-- Earlier deactivations recorded withdrawn applications as REJECTED. Their notification event
-- key ends in ':withdrawn', which identifies them exactly; re-running changes nothing further.
UPDATE public.group_join_requests r SET status='WITHDRAWN'
WHERE r.status='REJECTED'
  AND EXISTS(SELECT 1 FROM public.notifications n WHERE n.event_key='group-request:' || r.id || ':withdrawn');
-- Leaders' notices for applications withdrawn by deactivation or account deletion get the same type.
UPDATE public.notifications SET type='GROUP_JOIN_REQUEST_WITHDRAWN'
WHERE type='GROUP_JOIN_REQUEST_REJECTED'
  AND (event_key LIKE 'group-request:%:withdrawn' OR event_key LIKE 'account-delete:%:application:%');
