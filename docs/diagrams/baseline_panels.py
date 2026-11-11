"""Architecture, use-case, class and collaborator panels for the platform and both extras."""

from diagram_canvas import AMBER, BLUE, GREEN, LINE, MUTED, PALE, TEAL, WARM, INK, Canvas


def architecture():
    c = Canvas("architecture", "Architecture: layers, boundaries and privacy",
        "Requests pass down through the layers; services return DTOs back up. The browser never reaches the database.", height=1270)
    c.panel(430,150,750,910,"SPRING BOOT BACKEND",fill="#eff4f7")
    # Left column: the browser, configuration and the database
    c.box(45,200,330,185,"React + Vite",[
        "Feature folders per screen", "One shared Axios API client", "Route guards = UI only", "No database access"
    ])
    c.box(45,440,330,120,"In-browser media",["Spotify SDK and playlist embed", "Freesound previews play here"],color=AMBER,fill=WARM,size=17)
    c.box(45,620,330,165,"Configuration",["application.yml + env vars", "21 @ConfigurationProperties", "Admin matching overrides (DB)"],size=17)
    c.box(45,880,330,170,"PostgreSQL",["Hosted by Supabase", "SQL migrations own the schema", "Hibernate only validates it", "RLS; browser grants revoked"],color=TEAL,size=17)
    # The layers, top to bottom
    c.box(455,200,700,110,"Spring Security",["JWT signature · active account · role · token version", "Loads the user through UserRepository on every request"],size=17)
    c.box(455,340,330,140,"Controllers",["Validate typed input", "Call one service method", "Return the DTO as JSON"])
    c.box(825,340,330,140,"ApiExceptionHandler",["@RestControllerAdvice", "Exceptions → error JSON", "Rollback already done by Spring"],size=17)
    c.box(455,515,700,140,"Services",[
        "@Transactional business rules; notifications saved in the same transaction",
        "AccountAccess: checks who may act, locks accounts in a fixed order",
        "Call the assemblers and return DTOs, so entities never leave the backend"
    ],size=17)
    c.box(455,695,190,170,"Entities",["Own state rules", "Change via methods:", "accept(), close(),", "deactivate()"],size=17)
    c.box(665,695,210,170,"Matching",["CriterionScorer ×5", "MatchingStrategy ×3", "Strategy + Template", "Method patterns"],size=17)
    c.box(895,695,260,170,"Client interfaces",["AiClient", "WeatherClient", "SoundSearchClient", "ExplanationProvider (E1)"],size=17)
    c.box(455,905,330,140,"JPA repositories",["Spring Data JPA", "Row locks for transitions", "Also read by some assemblers"],size=17)
    c.box(825,905,330,140,"DTO assemblers",["sealed ProfileDto:", "Public = no contact", "Connected = has contact"],color=TEAL,size=17)
    # Right column: services outside the system
    c.box(1240,515,315,145,"Spotify Accounts",["Sign-in and token refresh", "Direct concrete client:", "SpotifyAccountsClient"],color=AMBER,fill=WARM,size=17)
    c.box(1240,695,315,170,"External APIs",["Google Gemini (AI features)", "Freesound (ambient sounds)", "Open-Meteo (cached, fallback)"],color=AMBER,fill=WARM,size=17)
    # Calls (grey)
    c.line([(375,242),(455,242)],end="arrow")
    c.label(415,226,"JWT + JSON",15)
    c.line([(620,310),(620,340)],end="arrow")
    c.line([(620,480),(620,515)],end="arrow")
    c.line([(550,655),(550,695)],end="arrow")
    c.line([(770,655),(770,695)],end="arrow")
    c.line([(1025,655),(1025,695)],end="arrow")
    c.line([(655,655),(655,905)],end="arrow")
    c.label(655,885,"query",14)
    c.line([(885,655),(885,905)],end="arrow")
    c.label(885,885,"build",14)
    c.line([(550,865),(550,905)],end="arrow",start="arrow")
    c.line([(825,975),(785,975)],end="arrow")
    c.line([(455,975),(375,975)],end="arrow")
    c.label(415,1000,"JDBC",15)
    c.line([(375,700),(415,700),(415,585),(455,585)],end="arrow")
    c.label(415,645,"binds",15)
    c.line([(210,385),(210,440)],end="arrow")
    c.line([(1155,587),(1240,587)],end="arrow")
    c.line([(1155,780),(1240,780)],end="arrow")
    c.label(1198,760,"HTTPS",15)
    # DTOs returned (teal)
    c.line([(740,515),(740,480)],end="arrow",color=TEAL)
    c.label(778,503,"DTO",15,color=TEAL)
    c.line([(455,440),(405,440),(405,330),(375,330)],end="arrow",color=TEAL)
    c.label(405,392,["DTO","JSON"],15,color=TEAL)
    c.note(45,1080,1510,[
        "20 feature packages (studygroup/, matching/, profile/ …) each hold their own controller and service, plus repositories where they store data; shared: common, security, ai, user, seed.",
        "Contact goes only to its owner or an active accepted buddy; group membership and admin rights never grant it. Keys and secrets stay on the backend.",
        "Arrows: grey = calls or depends on · teal = DTO returned towards the browser."
    ],fill="#edf3f6")
    return c


def use_cases():
    c = Canvas("use-cases", "UML use cases — compulsory requirements",
        "The brief's three user groups. A study group leader is a student, so the leader actor specialises Student.", height=1420)
    c.rect(250, 165, 1100, 1085, "white", "#93a9b6", 8, c.back)
    c.text(275, 200, "STUDY BUDDY MATCHER", 19, BLUE, 700)
    c.actor(105, 430, "Student")
    c.actor(105, 960, "Study Group", "Leader")
    c.actor(1495, 470, "System", "Administrator")
    c.line([(105, 941), (105, 600)], color=INK, end="triangle", width=2.5)
    student = [
        (245, ["Register an account"]),
        (321, ["Sign in"]),
        (397, ["Manage profile and", "study preferences"]),
        (473, ["Set weekly availability"]),
        (549, ["Search and filter", "ranked study buddies"]),
        (625, ["View a student's public profile"]),
        (701, ["Send, accept or decline", "match requests"]),
        (777, ["View and end", "buddy connections"]),
        (853, ["Browse groups and", "request to join"]),
    ]
    for y, rows in student:
        c.usecase(560, y, 470, rows)
        c.line([(143, 475), (325, y)])
    leader = [
        (955, ["Create a study group", "for a course"]),
        (1031, ["Update or close the group"]),
        (1107, ["Accept or reject", "join requests"]),
        (1183, ["Remove a member"]),
    ]
    for y, rows in leader:
        c.usecase(560, y, 470, rows)
        c.line([(143, 1005), (325, y)])
    # The administrator signs in too: a second association to the same use case.
    c.line([(1457, 515), (795, 321)])
    for y, rows in [
        (665, ["Create, update and", "delete user accounts"]),
        (759, ["View account status", "and basic usage"]),
        (853, ["Configure matching criteria,", "weights and strategy"]),
    ]:
        c.usecase(1085, y, 430, rows)
        c.line([(1457, 515), (1300, y)])
    c.usecase(1085, 549, 430, ["Explain this match", "(extra E1, on request)"], True)
    c.line([(870, 549), (795, 549)], dashed=True, end="arrow")
    c.label(832, 528, "«extend»", 15, fill="white")
    c.note(830, 960, 480, ["A leader is the student a group references", "(group.isLeader); there is no leader subclass", "or global leader role in the code."], title="Leader = Student + one group", fill=GREEN)
    c.note(830, 1120, 480, ["Viewing a profile hides the contact number", "until a match request has been accepted."], title="Contact privacy", fill=WARM)
    c.note(45, 1275, 1510, [
        "Notation: stick figure = actor, ellipse = use case, rectangle = system boundary, solid line = association, hollow arrow = the leader actor is a kind of Student.",
        "«extend»: the optional explanation adds to the search only when a student asks for it. The collaborative-study use cases (extra E2) are a separate panel.",
    ], fill="#edf3f6")
    return c


def e2_use_cases():
    c = Canvas("e2-use-cases", "UML use cases — collaborative study (E2)", "Second course-required extra; this panel supplements the compulsory use cases.",height=1220)
    c.rect(230,170,1280,870,"white","#93a9b6",8,c.back)
    c.text(255,206,"STUDY BUDDY MATCHER / E2",19,TEAL,700)
    c.actor(100,530,"Student")
    items=[
        (275,["Join / leave private room", "and view current participants"],["Accepted active group member;", "distinct students count toward capacity."]),
        (375,["Control Pomodoro", "and room configuration"],["Group leader or present assigned host/co-host;", "only the leader assigns host roles."]),
        (475,["Send room reactions"],["Only while present in the room; a burst limit", "applies. Everyone there sees it float briefly."]),
        (575,["Grow a focus forest"],["One tree per continuous room visit, sized by", "focused time on the shared timer."]),
        (675,["Play Spotify and", "ambient sounds"],["Music tab in room settings;", "heard only in this browser."]),
        (775,["Plan / edit / cancel", "group or buddy sessions"],["Group leader or active buddy participant;", "other accepted group members may view."]),
        (875,["View mutual availability", "and export calendar snapshot"],["Current parent relationship required;", "suggestions do not confirm attendance."]),
        (975,["Choose room atmosphere", "and view campus weather"],["Selectable original scenes; optional", "cached Open-Meteo campus conditions."]),
    ]
    for y,rows,note in items:
        c.usecase(595,y,590,rows,True)
        c.line([(138,575),(300,y)])
        c.lines(955,y-8,note,18,MUTED,gap=25)
        c.line([(890,y),(935,y)],dashed=True)
    c.note(45,1068,1510,["No global online status, public room, chat, calendar synchronisation or RSVP workflow is implied. Calendar files are private snapshots.", "E1 match explanations have a separate companion view (E1.md)."],fill="#edf3f6")
    return c


def identity_profile():
    c=Canvas("identity-profile", "UML classes — identity and study profile", "Concrete account and profile entities are associated through a shared identifier; there is no User inheritance tree.",height=1130)
    c.cls(60,210,350,"User",["- id : Long", "- email : String", "- passwordHash : String", "- role : Role", "- active : boolean", "- tokenVersion : long", "- lastLoginAt : Instant"])
    c.cls(610,180,480,"Student",["- id : Long", "- user : User", "- name, school, programme : String", "- yearOfStudy : Integer", "- contactNumber : String", "- coursesTaken : Set<Course>", "- targetCourse : Course", "- preferredStudyMode : StudyMode", "- preferredGroupSizeMin/Max : Integer", "- studyGoals : Set<StudyGoal>"])
    c.cls(1260,210,280,"Course",["- id : Long", "- code : String", "- name : String"])
    c.cls(610,720,480,"AvailabilitySlot",["- student : Student", "- dayOfWeek : DayOfWeek", "- startTime, endTime : LocalTime"])
    c.cls(60,720,350,"Role",["STUDENT", "ADMIN"],stereotype="enumeration")
    c.line([(610,310),(410,310)])
    c.label(510,270,["user", "@OneToOne / @MapsId"],17)
    c.label(442,336,"1",18)
    c.label(577,336,"0..1",18)
    c.line([(215,461),(215,720)],dashed=True,end="arrow")
    c.label(215,637,"role",17)
    c.line([(1090,275),(1260,275)])
    c.label(1175,241,"coursesTaken",17)
    c.label(1117,299,"0..*",18)
    c.label(1232,299,"0..*",18)
    c.line([(1090,391),(1160,391),(1160,346),(1260,346)])
    c.label(1190,420,"targetCourse",17)
    c.label(1120,374,"0..*",18)
    c.label(1225,371,"0..1",18)
    c.line([(850,509),(850,720)])
    c.label(873,565,"1",18)
    c.label(883,694,"0..*",18)
    c.label(850,632,"student",17)
    c.note(1160,555,380,["Student ID = account ID.", "An administrator need not have", "a Student profile.", "Empty optional preferences are", "unknown to the matching engine."],title="Association, not inheritance")
    c.note(45,980,1510,["Multiplicity is written at each endpoint. Plain associations do not imply cascade deletion; SQL migrations and lifecycle services define cleanup.", "Selected attributes are shown; ordinary constructors/getters/setters are omitted. Weekly slots are recurring Asia/Singapore civil times."],fill="#edf3f6")
    return c


def relationships():
    c = Canvas("relationship-domain", "UML classes — requests, connections and study groups",
        "A request records a decision; a Connection or a GroupMembership is the lasting relationship it creates. State changes only through named methods.",
        width=1800, height=1400)
    c.cls(60, 200, 430, "MatchRequest", ["- sender : Student", "- receiver : Student", "- message : String", "- status : MatchRequestStatus", "- respondedAt : Instant"],
        ["+ accept() : void", "+ decline() : void", "+ cancel() : void", "- respond(newStatus : MatchRequestStatus) : void"], size=17)
    c.cls(60, 590, 260, "MatchRequestStatus", ["PENDING", "ACCEPTED", "DECLINED", "CANCELLED"], stereotype="enumeration")
    c.cls(685, 250, 430, "Student", ["- id : Long", "- name : String", "- contactNumber : String"])
    c.cls(1310, 200, 430, "Connection", ["- studentA : Student", "- studentB : Student", "- createdAt : Instant", "- endedAt : Instant"],
        ["+ isActive() : boolean", "+ involves(studentId : Long) : boolean", "+ otherStudent(studentId : Long) : Student", "+ end() : void"])
    c.cls(685, 640, 430, "StudyGroup", ["- name : String", "- course : Course", "- leader : Student", "- maxGroupSize : Integer", "- active : boolean"],
        ["+ isLeader(studentId : Long) : boolean", "+ hasRoomFor(currentMemberCount : long) : boolean", "+ close() : void"], size=16)
    c.cls(1310, 640, 430, "GroupMembership", ["- studyGroup : StudyGroup", "- student : Student", "- joinedAt : Instant"])
    c.cls(1310, 870, 430, "GroupClosure", [], ["+ close(group : StudyGroup) : void"], stereotype="Component")
    c.cls(60, 850, 430, "GroupJoinRequest", ["- studyGroup : StudyGroup", "- student : Student", "- message : String", "- status : GroupJoinRequestStatus", "- respondedAt : Instant"],
        ["+ accept() : void", "+ reject() : void", "+ withdraw() : void", "+ requirePending() : void", "- respond(newStatus : GroupJoinRequestStatus) : void"], size=16)
    c.cls(685, 1020, 300, "GroupJoinRequestStatus", ["PENDING", "ACCEPTED", "REJECTED", "WITHDRAWN"], stereotype="enumeration")
    # Each link is an association for one named field; the many end is the class that holds the field.
    links = [
        ([(490, 290), (685, 290)], "sender", (588, 272), ("0..*", 520, 312), ("1", 665, 312)),
        ([(490, 350), (685, 350)], "receiver", (588, 332), ("0..*", 520, 372), ("1", 665, 372)),
        ([(1310, 290), (1115, 290)], "studentA", (1212, 272), ("0..*", 1280, 312), ("1", 1135, 312)),
        ([(1310, 350), (1115, 350)], "studentB", (1212, 332), ("0..*", 1280, 372), ("1", 1135, 372)),
        ([(900, 640), (900, 397)], "leader", (900, 525), ("0..*", 930, 620), ("1", 918, 422)),
        ([(1310, 700), (1115, 700)], "studyGroup", (1212, 682), ("0..*", 1280, 722), ("1", 1135, 722)),
        ([(1600, 640), (1600, 570), (1000, 570), (1000, 397)], "student", (1300, 552), ("0..*", 1632, 620), ("1", 1018, 422)),
        ([(490, 910), (685, 910)], "studyGroup", (588, 892), ("0..*", 520, 932), ("1", 665, 932)),
        ([(420, 850), (420, 570), (800, 570), (800, 397)], "student", (610, 552), ("0..*", 452, 830), ("1", 782, 422)),
    ]
    for points, role, (rx, ry), (many, mx, my), (one, ox, oy) in links:
        c.line(points, end="arrow")
        c.label(rx, ry, role, 16)
        c.label(mx, my, many, 16)
        c.label(ox, oy, one, 16)
    c.line([(190, 523), (190, 590)], dashed=True, end="arrow")
    c.line([(490, 1100), (685, 1100)], dashed=True, end="arrow")
    c.line([(1310, 915), (1115, 915)], dashed=True, end="arrow")
    c.label(1212, 897, "closes", 16)
    c.note(1310, 1020, 430, ["Rejects pending requests, notifies", "members, ends the room and sessions."], fill=GREEN)
    c.note(45, 1230, 1710, [
        "Solid line with an open arrowhead = association, navigable from the class that holds the field; the multiplicity at each end counts objects of that class. Dashed arrow = dependency.",
        "There is no status setter: accept(), decline(), cancel(), reject(), withdraw(), end() and close() are the only way to change state, and each refuses an illegal transition.",
        "A group's leader is an ordinary Student who also holds a GroupMembership, so the capacity rule hasRoomFor() counts the leader.",
    ], fill="#edf3f6")
    return c


def matching_design():
    c=Canvas("matching-design", "UML classes — scoring and ranking", "Independent scoring, ordering and wording responsibilities keep the matching engine explainable.",width=1800,height=1330)
    c.panel(45,170,840,800,"SCORING: PURE COLLABORATORS")
    c.panel(915,170,840,800,"RANKING: STRATEGY + TEMPLATE METHOD")
    c.cls(75,225,350,"WeightedMatchScorer",["- scorers : List<CriterionScorer>"],["+ score(searcher, candidate, search,", "  weights, enabled) : MatchScore"],size=17)
    c.cls(485,225,370,"CriterionScorer",[],["+ criterion() : MatchingCriterion", "+ score(searcher, candidate,", "  search) : CriterionResult"],stereotype="interface",size=17)
    c.cls(75,520,350,"MatchScore",["- breakdown :", "   Map<MatchingCriterion, Double>", "- total : double", "- sharedHoursPerWeek : double", "- quality : MatchQuality", "- unknownCriteria :", "   Set<MatchingCriterion>"],stereotype="record",size=17)
    implementations=["CourseScorer","AvailabilityOverlapScorer","StudyModeScorer","StudyGoalScorer","GroupSizeScorer"]
    for i,name in enumerate(implementations):
        c.cls(485,490+i*82,370,name,[],[],size=18)
        c.line([(485,515+i*82),(455,515+i*82)],dashed=True)
    c.line([(455,843),(455,415),(655,415),(655,393)],dashed=True,end="triangle")
    c.line([(425,315),(485,315)],end="arrow")
    c.label(455,297,"1..*",15)
    c.line([(248,392),(248,520)],dashed=True,end="arrow")
    c.label(248,462,"creates",17)
    c.cls(1125,225,430,"MatchingStrategy",[],["+ type() : MatchingStrategyType", "+ ranking(enabled) : Comparator<MatchScore>"],stereotype="interface",size=17)
    c.cls(945,495,340,"BalancedStrategy",[],["+ type() : MatchingStrategyType", "+ ranking(enabled) : Comparator"],size=17)
    c.cls(1355,465,365,"PriorityTierStrategy",["- tierFloors : List<Double>"],["+ tierOf(score) : int", "+ ranking(enabled) : Comparator", "~# priority() : MatchingCriterion", "~# priorityValue(score) : double"],stereotype="abstract",size=17,abstract=True)
    c.line([(1115,495),(1115,430),(1240,430),(1240,367)],dashed=True,end="triangle")
    c.line([(1538,465),(1538,430),(1440,430),(1440,367)],dashed=True,end="triangle")
    c.cls(945,752,375,"AvailabilityFirstStrategy",[],["+ type() : MatchingStrategyType", "# priority() : MatchingCriterion", "# priorityValue(score) : double"],size=17)
    c.cls(1345,752,380,"CourseFirstStrategy",[],["+ type() : MatchingStrategyType", "# priority() : MatchingCriterion", "# priorityValue(score) : double"],size=17)
    c.line([(1130,752),(1130,730),(1480,730),(1480,705)],end="triangle")
    c.line([(1545,752),(1545,705)],end="triangle")
    c.note(945,908,780,["Template method: ranking() orders by priority tier, then total; subclasses supply the tier."],fill="#edf3f6")
    c.box(45,1005,530,165,"MatchService",["search(criteria, actorId): MatchSearchResultDto", "One configuration snapshot; filter, score,", "threshold, order, limit and assemble."],size=18)
    c.box(635,1005,510,165,"MatchingConfigService",["snapshot(): MatchingConfigSnapshot", "Strategy + weights + enabled criteria", "+ minimum score + persistent version"],size=18)
    c.box(1205,1005,550,165,"MatchResultAssembler",["toDto(...): MatchResultDto", "Uses MatchExplainer / TemplateMatchExplainer", "for deterministic text; exposes no contact."],color=TEAL,size=18)
    c.line([(575,1085),(635,1085)],end="arrow")
    c.line([(310,1005),(310,988),(1480,988),(1480,1005)],end="arrow")
    c.line([(60,1005),(60,300),(75,300)],dashed=True,end="arrow")
    c.label(62,470,"uses",15)
    c.note(45,1200,1710,["Dashed line + hollow triangle = implements an interface; solid line + hollow triangle = extends a class. Italics = abstract class or operation; # = protected.", "Signatures omit repeated generic/parameter types for readability. CriterionResult records score + known; immutable interval values appear in the detailed matching source."],fill="#edf3f6")
    return c


def values_privacy():
    c=Canvas("values-and-privacy", "UML classes — immutable values and private response shapes", "Value objects protect interval rules; a sealed DTO boundary keeps contact disclosure on the server.",width=1800,height=1240)
    c.panel(45,170,830,850,"IMMUTABLE WEEKLY TIME VALUES")
    c.panel(905,170,850,850,"PROFILE RESPONSE POLICY",color=TEAL,fill=GREEN)
    c.cls(100,240,720,"WeeklyAvailability",["slots : List<TimeSlot>"],["+ totalMinutes() : long", "+ sharedMinutesWith(other : WeeklyAvailability) : long", "+ sharedSlotsWith(other : WeeklyAvailability) : List<TimeSlot>", "+ sharedSessionsWith(other, minimumSessionMinutes) : List<TimeSlot>"],stereotype="record",size=18)
    c.cls(100,650,720,"TimeSlot",["day : DayOfWeek", "start, end : LocalTime"],["+ durationMinutes() : long", "+ overlapMinutes(other : TimeSlot) : long", "+ intersection(other : TimeSlot) : Optional<TimeSlot>"],stereotype="record",size=18)
    c.nodes.append(f'<polygon points="460,480 452,492 460,504 468,492" fill="{LINE}" stroke="{LINE}"/>')
    c.line([(460,504),(460,650)])
    c.label(485,525,"1",18)
    c.label(502,625,"0..*",18)
    c.label(460,575,"slots (value composition)",17)
    c.cls(955,235,750,"ProfileViewAssembler",[],["+ assemble(subject : Student, viewerId : Long) : ProfileDto"],color=TEAL,size=19)
    c.cls(1090,465,480,"ProfileDto",[],["+ id() : Long"],stereotype="sealed interface",color=TEAL)
    c.line([(1330,330),(1330,465)],dashed=True,end="arrow")
    c.label(1330,405,"creates one permitted response shape",17)
    c.cls(935,720,385,"PublicProfileDto",["id : Long", "name : String", "availability : List<...>", "relationship : ProfileRelationshipDto"],stereotype="record",color=TEAL,size=16)
    c.cls(1350,720,385,"ConnectedProfileDto",["id : Long", "name : String", "contactNumber : String", "availability : List<...>", "relationship : ProfileRelationshipDto"],stereotype="record",color=TEAL,size=16)
    c.line([(1128,720),(1128,660),(1220,660),(1220,581)],dashed=True,end="triangle")
    c.line([(1543,720),(1543,660),(1440,660),(1440,581)],dashed=True,end="triangle")
    c.label(1050,636,"otherwise",17)
    c.label(1600,636,["SELF /","CONNECTED"],17)
    c.note(45,1060,1710,["WeeklyAvailability canonicalizes duplicate/overlapping/adjacent blocks, then intersects unique time. Half-open intervals that only touch share no minutes.", "PublicProfileDto has no contactNumber field. ConnectedProfileDto includes it only for the owner or an active accepted buddy; groups/admin status do not qualify.", "Selected members are shown. Both profile records implement ProfileDto independently; ConnectedProfileDto does not inherit PublicProfileDto."],fill="#edf3f6")
    return c


def collaborative_study():
    c=Canvas("collaborative-study", "UML collaborators — rooms, reactions, forest, sessions and weather", "Four bounded E2 responsibilities; room presence, short-lived reactions, focus visits and dated plans have separate lifecycles.",width=2380,height=1380)
    c.panel(45,170,560,1000,"PRIVATE GROUP ROOM",color=TEAL,fill=GREEN)
    c.panel(625,170,560,1000,"ROOM REACTIONS AND FOCUS FOREST",color=TEAL,fill=GREEN)
    c.panel(1205,170,560,1000,"DATED GROUP / BUDDY SESSIONS",color=TEAL,fill=GREEN)
    c.panel(1785,170,550,1000,"OPTIONAL CAMPUS WEATHER",color=TEAL,fill=GREEN)
    c.cls(85,235,480,"StudyRoomService",[],["+ state(groupId, actorId) : StudyRoomDto", "+ join(...), heartbeat(...) : StudyRoomDto", "+ leave(...) : void", "+ controlTimer(...),", "  configure(...) : StudyRoomDto"],color=TEAL,size=17)
    c.cls(85,505,480,"StudyRoom",["- studyGroup : StudyGroup [1]", "- host, coHost : Student [0..1 each]", "- version : long"],["+ timer() : PomodoroTimer", "+ applyTimer(timer) : void"],color=TEAL,size=18)
    c.cls(85,795,480,"RoomPresence",["- studyRoom : StudyRoom [1]", "- student : Student [1]", "- clientId : UUID", "- expiresAt, observedAt : Instant"],["+ renew(presence, now, expiresAt) : void"],color=TEAL,size=17)
    c.line([(325,435),(325,505)],end="arrow")
    c.label(325,477,"coordinates",17)
    c.line([(325,724),(325,795)])
    c.label(349,748,"1",17)
    c.label(361,779,"0..*",17)
    c.note(75,1065,500,["Read / renew: membership check; no group lock.", "Join / control: account locks → group lock."],fill="white")
    c.cls(665,225,480,"RoomReactionService",[],["+ send(groupId, actorId, type) : RoomReactionDto", "+ recent(room, now) : List<RoomReaction>"],color=TEAL,size=17)
    c.cls(665,400,480,"RoomReaction",["- studyRoom : StudyRoom [1]", "- student : Student [1]", "- type : RoomReactionType", "- sentAt : Instant"],[],color=TEAL,size=17)
    c.line([(905,346),(905,400)],end="arrow")
    c.label(905,380,"saves / lists",17)
    c.line([(565,300),(665,300)],end="arrow")
    c.label(615,286,"recent",15)
    c.cls(665,665,480,"ForestService",[],["+ recordTick(tick) : void", "+ endVisit(ended) : void", "+ forestOf(studentId) : ForestDto"],color=TEAL,size=17)
    c.cls(665,885,480,"FocusVisit",["- studentId, studyGroupId : Long", "- startedAt, lastSeenAt : Instant", "- focusedMillis : long", "- variant, plot : int"],["+ isOver(now, leaseLifetime) : boolean", "+ plantedAt() : Instant"],color=TEAL,size=17)
    c.line([(905,812),(905,885)],end="arrow")
    c.label(905,856,"one per room visit",17)
    c.line([(565,400),(615,400),(615,720),(665,720)],end="arrow",dashed=True)
    c.label(905,610,["RoomVisitTicked / RoomVisitEnded after commit,", "via ForestVisitListener"],15)
    c.cls(1245,235,480,"StudySessionService",[],["+ createGroup(...), createBuddy(...)", "+ edit(...), cancel(...) : SessionDto", "+ calendar(id, actorId) : CalendarExportDto"],color=TEAL,size=17)
    c.cls(1245,460,480,"StudySession",["- group : StudyGroup [0..1]", "- connection : Connection [0..1]", "- version : long", "- status : SessionStatus"],["+ schedule() : SessionSchedule", "+ edit(details, now) : void", "+ cancel(now) : void"],color=TEAL,size=18)
    c.cls(1245,825,480,"SessionSchedule",["startLocal, endLocal : LocalDateTime", "timeZone : String", "occurrenceCount : int"],["+ occurrences() : List<SessionOccurrenceDto>"],stereotype="record",color=TEAL,size=17)
    c.line([(1485,362),(1485,460)],end="arrow")
    c.label(1485,416,"authorizes via SessionAccess",17)
    c.line([(1485,711),(1485,825)],end="arrow",dashed=True)
    c.label(1485,786,"constructs immutable value",17)
    c.note(1235,1090,500,["Exactly one parent: group XOR connection.", "SessionCalendar emits private UTC snapshots."],fill="white")
    c.cls(1825,235,470,"WeatherService",[],["+ current() : WeatherDto"],color=TEAL,size=18)
    c.cls(1825,455,470,"WeatherClient",[],["+ fetch() : WeatherSample"],stereotype="interface",color=TEAL,size=18)
    c.cls(1825,685,470,"OpenMeteoClient",[],["+ fetch() : WeatherSample"],color=TEAL,size=18)
    c.line([(2060,330),(2060,455)],end="arrow")
    c.label(2060,401,"bounded cache / retry",17)
    c.line([(2060,685),(2060,571)],end="triangle",dashed=True)
    c.box(1825,915,470,135,"Open-Meteo API",["Fixed campus coordinates and weather fields.", "No student identity, location or schedule."],color=AMBER,fill=WARM,size=18)
    c.line([(2060,780),(2060,915)],end="arrow")
    c.label(2060,856,"Java HTTP client + timeout",17)
    c.note(45,1210,2290,["RoomAccess / SessionAccess check current account and parent authority; RoomAssembler / SessionAssembler / WeatherAssembler / ForestAssembler return DTOs.", "PomodoroTimer is an immutable server-time value; FocusCredit measures focus time from it and TreeGrowth maps minutes to a TreeSpecies. SessionAvailability reuses WeeklyAvailability.", "SessionEvents writes recipient notifications transactionally. Detailed E2 transaction and ER diagrams are in TEAM_C.md; E1 match explanations are in E1.md."],fill="#edf3f6")
    return c


def ai_study_tools():
    c=Canvas("ai-study-tools", "UML collaborators — quizzes, study plans and Pixel", "Three student features share one Gemini client; each checks the model's reply before anything is saved or shown.",width=1800,height=1560)
    c.panel(45,170,560,1010,"QUIZZES (GROUP / PERSONAL)",color=AMBER,fill=WARM)
    c.panel(625,170,560,1010,"STUDY PLANS (PERSONAL / SESSION)",color=AMBER,fill=WARM)
    c.panel(1205,170,550,1010,"PIXEL STUDY ASSISTANT",color=AMBER,fill=WARM)
    c.cls(85,230,480,"QuizCreationService",[],["+ startForGroup(groupId, actorId,", "  input, files) : QuizDto", "+ startPersonal(actorId, input,", "  files) : QuizDto"],size=17)
    c.cls(85,470,480,"QuizWriter",[],["+ write(studentId, course, focus,", "  material) : QuizDraft"],size=17)
    c.cls(85,650,480,"QuizOutputValidator",[],["+ validate(raw) : Optional<QuizDraft>"],size=17)
    c.cls(85,800,480,"Quiz",["- scope : QuizScope", "- group : StudyGroup [0..1]", "- session : StudySession [0..1]"],["+ statusAt(now) : QuizStatus", "+ close(now) : void"],size=17)
    c.line([(325,413),(325,470)],end="arrow")
    c.line([(325,589),(325,650)],end="arrow")
    c.label(325,628,"checks reply",15)
    c.cls(665,230,480,"StudyPlanCreationService",[],["+ createPersonal(actorId, input,", "  files) : StudyPlanDto", "+ createForSession(actorId,", "  sessionId, input, files)"],size=17)
    c.cls(665,470,480,"RoadmapWriter",[],["+ write(studentId, course, goal,", "  frame, material) : RoadmapDraft"],size=17)
    c.cls(665,650,480,"StudyPlanOutputValidator",[],["+ validate(raw, frame):", "  Optional<RoadmapDraft>"],size=17)
    c.cls(665,800,480,"StudyPlan",["- scope : StudyPlanScope", "- student : Student [0..1]", "- session : StudySession [0..1]"],["+ markEntry(entryId, completed,", "  now) : boolean"],size=17)
    c.line([(905,413),(905,470)],end="arrow")
    c.line([(905,589),(905,650)],end="arrow")
    c.label(905,628,"checks reply",15)
    c.cls(1245,230,470,"AssistantService",[],["+ reply(actorId, input):", "  AssistantReplyDto"],size=17)
    c.cls(1245,410,470,"QuickIntentReader",[],["+ read(message):", "  Optional<AssistantRequest>"],size=17)
    c.cls(1245,590,470,"IntentParser",[],["+ parse(studentId, message):", "  AssistantRequest"],size=17)
    c.cls(1245,770,470,"AssistantIntent",[],["+ type() : AssistantIntentType", "+ reply(actorId, request)"],stereotype="interface",size=17)
    c.line([(1480,365),(1480,410)],end="arrow")
    c.label(1480,393,"rules first",15)
    c.line([(1715,300),(1735,300),(1735,650),(1715,650)],end="arrow",dashed=True)
    c.label(1700,520,["else","AI"],15)
    c.line([(1260,365),(1260,385),(1225,385),(1225,830),(1245,830)],end="arrow")
    c.label(1225,700,"answers",15)
    c.note(1235,985,490,["Nine intents (find buddies, meet times,", "to-dos, plan a session, ...) answer from", "existing services; Gemini only classifies."],fill="white")
    c.note(75,1030,500,["QuizMarker scores; QuizLeaderboard ranks.", "Group answers stay on the server until", "the quiz closes."],fill="white")
    c.note(655,1030,500,["StudyMaterialReader checks file types.", "StudyPlanCalendar / SessionMeetingFrames", "build the frame the reply must fill."],fill="white")
    c.cls(85,1225,520,"AiClient",[],["+ generateJson(studentId, prompt,", "  attachments) : String"],stereotype="interface",color=BLUE,size=17)
    c.cls(665,1225,420,"GeminiClient",[],["+ generateJson(...) : String"],color=BLUE,size=17)
    c.cls(1145,1225,420,"AiQuota",[],["+ tryAcquire(studentId) : boolean", "+ pauseAfterRateLimit() : void"],color=BLUE,size=17)
    c.line([(665,1275),(605,1275)],end="triangle",dashed=True)
    c.line([(1085,1275),(1145,1275)],end="arrow")
    c.line([(325,1180),(325,1225)],end="arrow")
    c.line([(905,1180),(905,1205),(345,1205),(345,1225)],end="arrow")
    c.line([(1480,1180),(1480,1195),(365,1195),(365,1225)],end="arrow")
    c.note(45,1400,1710,["QuizWriter, RoadmapWriter and AssistantAi depend on the AiClient interface, so tests supply a fake. Pixel uses a lighter model with its own allowance.", "Failed, refused or over-quota calls return 503 AI_UNAVAILABLE and save nothing; prompts are versioned files under resources/prompts."],fill="#edf3f6")
    return c


BUILDERS = [architecture, use_cases, e2_use_cases, identity_profile, relationships, matching_design,
    values_privacy, collaborative_study, ai_study_tools]
