"""Behaviour panels: UML sequence diagrams for the key flows and state machines for the lifecycles.

Every message is a method that exists in backend/src/main/java; the numbering follows the order
of the calls in the named service method.
"""

from diagram_canvas import BLUE, GREEN, PALE, TEAL, Canvas, Sequence

NOTE = "#edf3f6"


def matching_sequence():
    c = Canvas("matching-sequence", "UML sequence — ranked match search",
        "GET /api/matches: MatchService.search scores every eligible candidate with the admin's current settings.",
        width=2100, height=1400)
    s = Sequence(c, "MatchService.search", [
        (110, "Student", "actor"), (330, ":MatchController", "class"), (580, ":MatchService", "class"),
        (820, ":AccountAccess", "class"), (1080, ":MatchingConfigService", "class"),
        (1360, ":WeightedMatchScorer", "class"), (1650, ":CriterionScorer", "class"),
        (1910, ":MatchResultAssembler", "class")], top=205, bottom=1215)
    s.call(0, 1, 310, "1: GET /api/matches")
    s.bar(1, 310, 1180)
    s.call(1, 2, 355, "2: search(criteria, actorId)")
    s.bar(2, 355, 1140)
    s.call(2, 3, 400, "3: requireStudent(actorId)")
    s.bar(3, 400, 435)
    s.ret(3, 2, 435, "self : Student")
    s.call(2, 3, 480, "4: eligibleStudentIds()")
    s.bar(3, 480, 515)
    s.ret(3, 2, 515, "ids : List<Long>")
    s.call(2, 4, 560, "5: snapshot()")
    s.bar(4, 560, 600)
    s.ret(4, 2, 600, "MatchingConfigSnapshot (strategy, weights, enabledCriteria, minScore)")
    s.frame("loop", "[each eligible candidate that MatchSearchFilter accepts]", 470, 2035, 635, 900)
    s.call(2, 5, 700, "6: score(searcher, candidate, search, weights, enabled)")
    s.bar(5, 700, 860)
    s.frame("loop", "[each enabled criterion]", 1270, 1790, 725, 835)
    s.call(5, 6, 785, "7: score(searcher, candidate, search)")
    s.bar(6, 785, 815)
    s.ret(6, 5, 815, "CriterionResult")
    s.ret(5, 2, 860, "MatchScore (total, breakdown, quality, sharedHoursPerWeek)")
    s.self_call(2, 925, ["8: keep total ≥ minScore and minQuality, sort with", "strategy.ranking(enabledCriteria)"])
    s.bar(2, 955, 990, level=1)
    s.frame("loop", "[each of the first `limit` ranked candidates]", 470, 2035, 1015, 1110)
    s.call(2, 7, 1070, "9: toDto(searcher, candidate, score, search, settings, relationship)")
    s.bar(7, 1070, 1098)
    s.ret(7, 2, 1098, "MatchResultDto (no contact number)")
    s.ret(2, 1, 1140, "MatchSearchResultDto")
    s.ret(1, 0, 1180, "200 OK: ranked list")
    c.note(45, 1255, 2010, [
        ":CriterionScorer stands for the five injected implementations (course, availability, study mode, study goal, group size); WeightedMatchScorer calls each through the interface.",
        "An invalid filter or limit ends the call with 400 before any scoring, and no match is an ordinary 200 with an empty list. Solid arrow = call, dashed arrow = return.",
    ], fill=NOTE)
    return c


def match_request_sequence():
    c = Canvas("match-request-sequence", "UML sequence — send and accept a match request",
        "Student A sends a request; Student B accepts it. Accepting creates the Connection that makes each contact number visible.",
        width=2430, height=1600)
    s = Sequence(c, "MatchRequestService.send / accept", [
        (110, "Student A", "actor"), (250, "Student B", "actor"), (500, ":MatchRequestController", "class"),
        (850, ":MatchRequestService", "class"), (1110, ":AccountAccess", "class"),
        (1390, "request:MatchRequest", "late"), (1690, ":MatchRequestRepository", "class"),
        (1990, ":ConnectionRepository", "class"), (2260, ":NotificationService", "class")], top=205, bottom=1405)
    s.call(0, 2, 310, "1: POST /api/match-requests")
    s.bar(2, 310, 860)
    s.call(2, 3, 355, "2: send(senderId, receiverId, message)")
    s.bar(3, 355, 825)
    s.call(3, 4, 400, "3: lockStudents(ids)")
    s.bar(4, 400, 418)
    s.call(3, 4, 445, "4: eligibleStudent(receiverId)")
    s.bar(4, 445, 480)
    s.ret(4, 3, 480, "receiver : Student")
    s.call(3, 7, 525, "5: findActiveBetween(senderId, receiverId)")
    s.bar(7, 525, 555)
    s.ret(7, 3, 555, "empty")
    s.call(3, 6, 600, "6: findPendingBetween(senderId, receiverId)")
    s.bar(6, 600, 630)
    s.ret(6, 3, 630, "empty")
    s.create(3, 5, 680, "7: «create»")
    c.label(1530, 686, "status = PENDING", 16, fill="white", anchor="start")
    s.call(3, 6, 735, "8: save(request)", lb=0)
    s.bar(6, 735, 753)
    s.call(3, 8, 790, "9: notify(receiver, MATCH_REQUEST_RECEIVED)")
    s.bar(8, 790, 808)
    s.ret(3, 2, 825, "MatchRequestDto (PENDING)")
    s.ret(2, 0, 860, "201 Created")

    s.call(1, 2, 910, "10: POST …/{id}/accept")
    s.bar(2, 910, 1370)
    s.call(2, 3, 955, "11: accept(requestId, actorId)")
    s.bar(3, 955, 1335)
    s.self_call(3, 990, ["12: forDecision(requestId,", "actorId)"], drop=44)
    s.bar(3, 1034, 1066, level=1)
    s.frame("alt", "[request is PENDING and the two students are not yet connected]", 200, 2365, 1090, 1385)
    s.call(3, 5, 1150, "13: accept()")
    s.bar(5, 1150, 1205)
    s.self_call(5, 1160, "13.1: respond(ACCEPTED)")
    s.bar(5, 1190, 1205, level=1)
    s.call(3, 7, 1230, "14: save(new Connection(sender, receiver))")
    s.bar(7, 1230, 1248)
    s.call(3, 8, 1270, "15: notify(sender, MATCH_REQUEST_ACCEPTED)")
    s.bar(8, 1270, 1288)
    s.ret(3, 2, 1300, "MatchRequestDto (ACCEPTED)")
    s.ret(2, 1, 1318, "200 OK")
    s.divider(200, 2365, 1330, "[else]")
    s.ret(3, 2, 1362, "409 Conflict", la=0)
    c.label(1000, 1366, "IllegalStateException or MatchRequestNotAllowedException", 16, fill="white", anchor="start")
    c.note(45, 1445, 2340, [
        "forDecision() runs first: it checks that the actor is the receiver (otherwise 403), locks both student rows in a fixed order through AccountAccess, then loads the request for update.",
        "MatchRequest owns its state machine: accept(), decline() and cancel() all go through the private respond(), which throws once the request has left PENDING. There is no status setter.",
        "Nothing here returns a contact number. The next profile read picks ConnectedProfileDto because an active Connection now exists (see the contact-privacy sequence).",
    ], fill=NOTE)
    return c


def contact_privacy_sequence():
    c = Canvas("contact-privacy-sequence", "UML sequence — who receives the contact number",
        "GET /api/students/{id}/profile: the assembler chooses the DTO type on the server, so a stranger's response has no contact field at all.",
        width=2310, height=1235)
    s = Sequence(c, "ProfileViewService.view", [
        (110, "Student", "actor"), (390, ":ProfileViewController", "class"), (710, ":ProfileViewService", "class"),
        (960, ":AccountAccess", "class"), (1230, ":ProfileViewAssembler", "class"),
        (1570, ":ProfileRelationshipAssembler", "class"), (1870, ":ConnectedProfileDto", "late"),
        (2090, ":PublicProfileDto", "late")], top=205, bottom=1040)
    s.call(0, 1, 310, "1: GET /api/students/{id}/profile")
    s.bar(1, 310, 1005)
    s.call(1, 2, 355, "2: view(subjectId, viewerId)")
    s.bar(2, 355, 965)
    s.call(2, 3, 400, "3: requireStudent(viewerId)")
    s.call(2, 3, 445, "4: eligibleStudent(subjectId)")
    s.bar(3, 400, 480)
    s.ret(3, 2, 480, "subject : Student")
    s.call(2, 4, 525, "5: assemble(subject, viewerId)")
    s.bar(4, 525, 925)
    s.call(4, 5, 570, "6: assemble(subjectId, viewerId)")
    s.bar(5, 570, 610)
    s.ret(5, 4, 610, "relationship : ProfileRelationshipDto")
    s.frame("alt", "[relationship.state() is SELF or CONNECTED]", 1090, 2245, 645, 900)
    s.create(4, 6, 730, "7a: «create» (…, contactNumber, …)")
    s.divider(1090, 2245, 780, "[else: STRANGER, or a request is still pending]")
    s.create(4, 7, 855, "7b: «create» (… no contactNumber component …)")
    s.ret(4, 2, 925, "ProfileDto")
    s.ret(2, 1, 965, "ProfileDto")
    s.ret(1, 0, 1005, "200 OK, never cached")
    c.note(45, 1080, 2220, [
        "ProfileDto is a sealed interface that permits exactly these two records. PublicProfileDto has no contactNumber component, so the value cannot be serialised by mistake.",
        "CONNECTED means an active Connection exists between viewer and subject. Group membership and administrator rights never select ConnectedProfileDto.",
    ], fill=NOTE)
    return c


def group_join_sequence():
    c = Canvas("group-join-sequence", "UML sequence — group leader approves a join request",
        "POST /api/groups/{id}/join-requests/{requestId}/accept: leader check, capacity check and the new membership in one transaction.",
        width=2140, height=1360)
    s = Sequence(c, "GroupJoinRequestService.accept", [
        (120, "Group leader", "actor"), (400, ":GroupJoinRequestController", "class"),
        (770, ":GroupJoinRequestService", "class"), (1110, "request:GroupJoinRequest", "class"),
        (1380, "group:StudyGroup", "class"), (1670, ":GroupMembershipRepository", "class"),
        (1960, ":NotificationService", "class")], top=205, bottom=1180)
    s.call(0, 1, 310, "1: POST …/{requestId}/accept")
    s.bar(1, 310, 1080)
    s.call(1, 2, 355, "2: accept(groupId, requestId, actorId)")
    s.bar(2, 355, 1080)
    s.self_call(2, 395, ["3: forDecision(groupId,", "requestId, actorId)"], drop=44)
    s.bar(2, 439, 475, level=1)
    s.frame("alt", "[request PENDING; group open and not full]", 50, 2075, 505, 1100)
    s.call(2, 3, 575, "4: requirePending()")
    s.bar(3, 575, 595)
    s.call(2, 4, 620, "5: isActive()")
    s.bar(4, 620, 640)
    s.call(2, 5, 665, "6: countByStudyGroupId(groupId)")
    s.bar(5, 665, 695)
    s.ret(5, 2, 695, "memberCount")
    s.call(2, 4, 740, "7: hasRoomFor(memberCount)")
    s.bar(4, 740, 770)
    s.ret(4, 2, 770, "true")
    s.call(2, 3, 815, "8: accept()")
    s.bar(3, 815, 875)
    s.self_call(3, 825, "8.1: respond(ACCEPTED)")
    s.bar(3, 855, 875, level=1)
    s.call(2, 5, 905, "9: save(new GroupMembership(group, applicant))")
    s.bar(5, 905, 925)
    s.call(2, 6, 950, "10: notify(applicant, GROUP_JOIN_REQUEST_ACCEPTED)")
    s.bar(6, 950, 970)
    s.ret(2, 1, 1000, "GroupJoinRequestDto (ACCEPTED)")
    s.ret(1, 0, 1022, "200 OK")
    s.divider(50, 2075, 1035, "[else]")
    s.ret(2, 1, 1080, "409 Conflict: answered, closed or full")
    c.note(45, 1220, 2050, [
        "forDecision() confirms the actor leads this group (otherwise 403), locks the leader and applicant accounts, then locks the group row before loading the request for update.",
        "Because approvals queue on the group row, two approvals can never both take the last place. Leadership is a relationship with one group (group.isLeader), not a role or subclass.",
    ], fill=NOTE)
    return c


def matching_config_sequence():
    c = Canvas("matching-config-sequence", "UML sequence — administrator retunes the matching engine",
        "PUT /api/admin/matching-config saves a new version; the next search reads it and ranks through the chosen strategy.",
        width=2440, height=1500)
    s = Sequence(c, "MatchingConfigService.update, then MatchService.search", [
        (120, "Administrator", "actor"), (280, "Student", "actor"),
        (560, ":AdminMatchingConfigController", "class"), (930, ":MatchingConfigService", "class"),
        (1180, ":AccountAccess", "class"), (1480, "setting:MatchingStrategySetting", "class"),
        (1790, ":MatchController", "class"), (2030, ":MatchService", "class"),
        (2270, ":MatchingStrategy", "class")], top=205, bottom=1290)
    s.call(0, 2, 310, "1: PUT /api/admin/matching-config")
    s.bar(2, 310, 900)
    s.call(2, 3, 355, "2: update(request, actorId)")
    s.bar(3, 355, 865)
    s.call(3, 4, 400, "3: lockAdmin(actorId)")
    s.bar(4, 400, 420)
    s.self_call(3, 440, "4: validWeights(request.weights(), enabled)")
    s.bar(3, 470, 500, level=1)
    s.self_call(3, 520, "5: lockSettings(request.expectedVersion())")
    s.bar(3, 550, 580, level=1)
    s.frame("alt", "[expectedVersion equals setting.getConfigVersion()]", 60, 2375, 605, 915)
    s.self_call(3, 665, "6: saveWeights(type, weights)  for each strategy")
    s.bar(3, 695, 725, level=1)
    s.call(3, 5, 760, "7: saveOverride(strategy, enabled, minScore)")
    s.bar(5, 760, 815)
    s.self_call(5, 770, "7.1: changed()  (configVersion++)")
    s.bar(5, 800, 815, level=1)
    s.ret(3, 2, 832, "MatchingConfigDto (new version)")
    s.ret(2, 0, 852, "200 OK")
    s.divider(60, 2375, 862, "[else]")
    s.ret(3, 2, 898, "409 STALE_MATCHING_CONFIG")

    s.call(1, 6, 990, "8: GET /api/matches")
    s.bar(6, 990, 1255)
    s.call(6, 7, 1030, "9: search(criteria, actorId)")
    s.bar(7, 1030, 1220)
    s.call(7, 3, 1070, "10: snapshot()")
    s.bar(3, 1070, 1110)
    s.ret(3, 7, 1110, "MatchingConfigSnapshot (strategy, weights, enabledCriteria, minScore, version)")
    s.call(7, 8, 1155, "11: ranking(enabledCriteria)")
    s.bar(8, 1155, 1185)
    s.ret(8, 7, 1185, "Comparator<MatchScore>")
    s.ret(7, 6, 1220, "MatchSearchResultDto")
    s.ret(6, 1, 1255, "200 OK: results ranked with the new settings")
    c.note(45, 1330, 2350, [
        ":MatchingStrategy is whichever implementation the administrator chose: BalancedStrategy, AvailabilityFirstStrategy or CourseFirstStrategy. MatchService only knows the interface.",
        "validWeights() builds a MatchingWeights per strategy (each weight 0..1, sum 1); validEnabled() and validThreshold() run just before it. After saveOverride() the setting row is saved through its repository.",
        "lockSettings() takes the row lock and compares versions, so when two administrators save at once the second gets 409 and reloads. Each search reads one snapshot of the settings.",
    ], fill=NOTE)
    return c


def _machine(c, x, y, w, h, title, color=BLUE, fill="white"):
    c.rect(x, y, w, h, fill, "#93a9b6", 10, c.back)
    c.text(x+22, y+34, title, 19, color, 700, layer=c.back)


def _transition(c, points, rows, lx, ly, anchor="middle"):
    c.line(points, end="arrow")
    c.label(lx, ly, rows, 17, fill="white", anchor=anchor)


def _request_machine(c, top, title, initial, outcomes, color):
    """Three-outcome request lifecycle: one PENDING state and three final states."""
    _machine(c, 45, top, 1150, 460, title, color)
    middle = top+255
    c.initial(100, middle)
    c.state(360, middle-40, 200, 80, "PENDING", color=color, fill=PALE if color == BLUE else GREEN)
    _transition(c, [(111, middle), (360, middle)], initial, 235, middle-70)
    for offset, (name, rows) in zip((-150, 0, 150), outcomes):
        y = middle+offset
        c.state(930, y-35, 200, 70, name, color=color, fill=PALE if color == BLUE else GREEN)
        start = (560, middle+offset/6)
        route = [start, (930, y)] if offset == 0 else [start, (610, middle+offset/6), (610, y), (930, y)]
        _transition(c, route, rows, 770, y-39)
        c.line([(1130, y), (1153, y)], end="arrow")
        c.final(1170, y)


def _pair_machine(c, top, title, first, second, created, ended, color, loop=None):
    """Two-state lifecycle drawn top to bottom: created, then ended and kept as history."""
    _machine(c, 1215, top, 640, 460, title, color)
    fill = PALE if color == BLUE else GREEN
    c.initial(1400, top+62)
    c.state(1270, top+100, 260, 110, first[0], first[1], color, fill)
    c.state(1270, top+290, 260, 110, second[0], second[1], color, fill)
    _transition(c, [(1400, top+73), (1400, top+100)], created, 1422, top+82, "start")
    _transition(c, [(1400, top+210), (1400, top+290)], ended, 1422, top+238, "start")
    c.line([(1400, top+400), (1400, top+420)], end="arrow")
    c.final(1400, top+437)
    if loop:
        c.line([(1530, top+135), (1575, top+135), (1575, top+180), (1530, top+180)], end="arrow")
        c.label(1590, top+152, loop, 17, fill="white", anchor="start")


def request_lifecycles():
    c = Canvas("request-lifecycles", "UML state machines — requests, connections and groups",
        "Each entity changes state only through named methods (accept, decline, cancel, reject, withdraw, end, close); there is no status setter.",
        width=1900, height=1300)
    _request_machine(c, 170, "MatchRequest  (status : MatchRequestStatus)",
        ["send()", "[not self, not connected,", "no pending request]"], [
            ("ACCEPTED", ["accept()  [actor is receiver]", "/ create Connection, notify sender"]),
            ("DECLINED", ["decline()  [actor is receiver]", "/ notify sender"]),
            ("CANCELLED", ["cancel()  [actor is sender]", "/ notify receiver"]),
        ], BLUE)
    _pair_machine(c, 170, "Connection  (active while endedAt is null)",
        ("ACTIVE", ["endedAt = null", "contact number visible"]), ("ENDED", ["endedAt set, row kept", "contact hidden again"]),
        "MatchRequest accepted", ["end()  [actor is a participant]", "/ notify the other student"], BLUE)
    _request_machine(c, 660, "GroupJoinRequest  (status : GroupJoinRequestStatus)",
        ["request()", "[group open, has room,", "not member, none pending]"], [
            ("ACCEPTED", ["accept()  [leader; open, not full]", "/ create GroupMembership, notify"]),
            ("REJECTED", ["reject()  [leader; group open]", "/ notify applicant"]),
            ("WITHDRAWN", ["withdraw()  [actor is applicant]", "/ notify leader"]),
        ], TEAL)
    _pair_machine(c, 660, "StudyGroup  (active : boolean)",
        ("OPEN", ["active = true", "leader is the first member"]), ("CLOSED", ["active = false", "members kept as history"]),
        "new StudyGroup(…)", ["close()  [actor is leader]", "/ GroupClosure.close(group)"], TEAL, loop="setters  [leader]")
    c.note(45, 1150, 1810, [
        "Label format: event [guard] / action. A transition out of a final state throws IllegalStateException, which the API reports as 409 STATE_CONFLICT.",
        "Deactivating an account also cancels its pending match requests, withdraws its applications, ends its connections and closes the groups it leads (StudentDeactivation).",
    ], fill=NOTE)
    return c


BUILDERS = [matching_sequence, match_request_sequence, contact_privacy_sequence, group_join_sequence,
    matching_config_sequence, request_lifecycles]
