-- A pending request closed because either account was deactivated or deleted is CANCELLED,
-- not DECLINED, so the receiver's history never shows an answer they did not give.
ALTER TABLE public.match_requests DROP CONSTRAINT IF EXISTS match_requests_status_check;
ALTER TABLE public.match_requests ADD CONSTRAINT match_requests_status_check CHECK(status IN (
    'PENDING','ACCEPTED','DECLINED','CANCELLED'));

ALTER TABLE public.notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE public.notifications ADD CONSTRAINT notifications_type_check CHECK(type IN (
    'MATCH_REQUEST_RECEIVED','MATCH_REQUEST_ACCEPTED','MATCH_REQUEST_DECLINED','MATCH_REQUEST_CANCELLED',
    'CONNECTION_ENDED','GROUP_JOIN_REQUEST_RECEIVED','GROUP_JOIN_REQUEST_ACCEPTED',
    'GROUP_JOIN_REQUEST_REJECTED','GROUP_MEMBER_REMOVED','GROUP_CLOSED'));
