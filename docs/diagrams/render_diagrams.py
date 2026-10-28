#!/usr/bin/env python3
"""Rebuild the presentation SVGs with Python 3's standard library.

The named functions below are the editable diagram source. Coordinates are deliberate:
UML roles, labels and routed links stay readable without a graph-layout dependency.
Run from any directory; --check verifies source names, XML and committed exports.
Detailed Mermaid views in PLATFORM.md / TEAM_A.md / TEAM_C.md supplement these panels.
"""

from html import escape
from pathlib import Path
import argparse
import sys
import xml.etree.ElementTree as ET


HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
INK = "#172d3b"
MUTED = "#506474"
LINE = "#647b88"
BLUE = "#254e73"
TEAL = "#176c63"
AMBER = "#8a5a1a"
BG = "#f6f8f9"
PALE = "#eaf1f7"
GREEN = "#eaf5f1"
WARM = "#fff5e5"


class Canvas:
    def __init__(self, slug, title, subtitle, width=1600, height=1100):
        self.slug, self.title = slug, title
        self.width, self.height = width, height
        self.back, self.edges, self.nodes, self.labels = [], [], [], []
        self.rect(0, 0, width, height, fill=BG, stroke="none", layer=self.back)
        self.rect(0, 0, width, 128, fill=INK, stroke="none", layer=self.back)
        self.text(46, 45, "STUDY BUDDY MATCHER  /  DESIGN REFERENCE", 15, "#a8d4ca", weight=700)
        self.text(46, 85, title, 31, "#ffffff", weight=700)
        self.text(46, 110, subtitle, 17, "#d9e5ea")
        self.line([(44, height-54), (width-44, height-54)], color="#c5d2da")
        self.text(width-46, height-27, f"docs/diagrams/exports/{slug}.svg", 14, MUTED, anchor="end")

    def rect(self, x, y, w, h, fill="white", stroke="#c5d2da", radius=0, layer=None, extra=""):
        (self.nodes if layer is None else layer).append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{radius}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="1.5" {extra}/>')

    def text(self, x, y, value, size=19, color=INK, weight=400, anchor="start", layer=None):
        (self.labels if layer is None else layer).append(
            f'<text x="{x}" y="{y}" font-size="{size}" fill="{color}" '
            f'font-weight="{weight}" text-anchor="{anchor}">{escape(str(value))}</text>')

    def lines(self, x, y, rows, size=19, color=INK, gap=27, weight=400, anchor="start"):
        for n, row in enumerate(rows):
            self.text(x, y+n*gap, row, size, color, weight, anchor)

    def panel(self, x, y, w, h, label, color=BLUE, fill=PALE):
        self.rect(x, y, w, h, fill, "#bdccd5", 12, self.back)
        self.text(x+22, y+31, label, 18, color, 700)

    def box(self, x, y, w, h, title, rows, color=BLUE, fill="white", stereotype=None, size=19):
        self.rect(x, y, w, h, fill, color, 6, extra='data-node="box"')
        self.rect(x, y, w, 6, color, "none")
        top = y+35
        if stereotype:
            self.text(x+18, y+29, f"«{stereotype}»", 15, color)
            top = y+57
        self.text(x+18, top, title, 23, color, 700)
        self.lines(x+18, top+34, rows, size, gap=27)

    def cls(self, x, y, w, name, attrs=(), methods=(), stereotype=None, color=BLUE, size=18):
        header = 70 if stereotype else 49
        h = header + (len(attrs)*26+20 if attrs else 0) + (len(methods)*26+20 if methods else 0)
        self.rect(x, y, w, h, "white", color, 0, extra=f'data-class="{escape(name)}"')
        self.rect(x, y, w, header, PALE if color == BLUE else GREEN, "none")
        if stereotype:
            self.text(x+w/2, y+25, f"«{stereotype}»", 15, color, anchor="middle")
        self.text(x+w/2, y+header-16, name, 22, color, 700, "middle")
        cursor = y+header
        for rows in (attrs, methods):
            if not rows:
                continue
            self.line([(x, cursor), (x+w, cursor)], color=color, layer=self.nodes)
            self.lines(x+16, cursor+29, rows, size, gap=26)
            cursor += len(rows)*26+20
        return h

    def line(self, points, color=LINE, dashed=False, end=None, start=None, layer=None, width=2):
        attrs = ' stroke-dasharray="7 6"' if dashed else ""
        if end:
            attrs += f' marker-end="url(#{end})"'
        if start:
            attrs += f' marker-start="url(#{start})"'
        (self.edges if layer is None else layer).append(
            f'<polyline points="{" ".join(f"{x},{y}" for x,y in points)}" fill="none" '
            f'stroke="{color}" stroke-width="{width}" stroke-linejoin="round"{attrs}/>')

    def label(self, x, y, rows, size=17, color=MUTED, anchor="middle"):
        if isinstance(rows, str):
            rows = [rows]
        width = max(len(row) for row in rows)*size*.54+16
        left = x-width/2 if anchor == "middle" else x-7
        self.rect(left, y-size, width, len(rows)*(size+5)+5, BG, "none", 3, self.labels)
        self.lines(x, y, rows, size, color, gap=size+5, anchor=anchor)

    def note(self, x, y, w, rows, title=None, fill=WARM):
        h = len(rows)*25+24+(30 if title else 0)
        self.rect(x, y, w, h, fill, "#d6c7a8" if fill == WARM else "#c5d2da", 6)
        offset = 28
        if title:
            self.text(x+17, y+28, title, 19, AMBER if fill == WARM else BLUE, 700)
            offset += 30
        self.lines(x+17, y+offset, rows, 18, gap=25)

    def actor(self, x, y, title, note=None):
        self.nodes.append(f'<circle cx="{x}" cy="{y}" r="19" fill="white" stroke="{INK}" stroke-width="2.5"/>')
        self.line([(x,y+19),(x,y+84)], color=INK, layer=self.nodes, width=2.5)
        self.line([(x-38,y+45),(x+38,y+45)], color=INK, layer=self.nodes, width=2.5)
        self.line([(x-34,y+120),(x,y+84),(x+34,y+120)], color=INK, layer=self.nodes, width=2.5)
        self.text(x,y+150,title,22,INK,700,"middle")
        if note:
            self.text(x,y+176,note,16,MUTED,anchor="middle")

    def usecase(self, x, y, w, rows, e2=False):
        self.nodes.append(f'<ellipse cx="{x}" cy="{y}" rx="{w/2}" ry="37" fill="{GREEN if e2 else PALE}" stroke="{TEAL if e2 else BLUE}" stroke-width="1.7"/>')
        self.lines(x, y-(len(rows)-1)*12+6, rows, 18, gap=24, anchor="middle")

    def fragment(self, x, y, w, h, kind, guard):
        self.rect(x,y,w,h,"#ffffff","#a7bac5",0,self.back)
        self.rect(x,y,80,29,PALE,"#a7bac5",0,self.back)
        self.text(x+12,y+21,kind,16,BLUE,700)
        self.text(x+96,y+21,guard,17,MUTED)

    def svg(self):
        return '\n'.join([
            '<?xml version="1.0" encoding="UTF-8"?>',
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.width}" height="{self.height}" '
            f'viewBox="0 0 {self.width} {self.height}" role="img" aria-labelledby="title description">',
            f'<title id="title">{escape(self.title)}</title>',
            '<desc id="description">Study Buddy Matcher design diagram. Editable source: '
            'docs/diagrams/render_diagrams.py. Supplementary text and notation are in docs/diagrams/README.md.</desc>',
            '<defs><marker id="arrow" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="8" markerHeight="8" orient="auto-start-reverse">'
            f'<path d="M 1 1 L 11 6 L 1 11" fill="none" stroke="{LINE}" stroke-width="1.6"/></marker>',
            '<marker id="triangle" viewBox="0 0 14 14" refX="13" refY="7" markerWidth="12" markerHeight="12" orient="auto-start-reverse">'
            f'<path d="M 1 1 L 13 7 L 1 13 Z" fill="white" stroke="{LINE}" stroke-width="1.4"/></marker>',
            '<marker id="diamond" viewBox="0 0 16 12" refX="1" refY="6" markerWidth="13" markerHeight="10" orient="auto-start-reverse">'
            f'<path d="M 1 6 L 8 1 L 15 6 L 8 11 Z" fill="{LINE}" stroke="{LINE}"/></marker></defs>',
            '<g font-family="Tahoma, Verdana, Arial, sans-serif">',
            *self.back, *self.edges, *self.nodes, *self.labels, '</g></svg>', ''
        ])


def architecture():
    c = Canvas("architecture", "Architecture and privacy boundaries", "The browser reaches application data only through the authenticated Spring API.", height=1130)
    c.panel(455,170,720,795,"SPRING BOOT BACKEND",fill="#eff4f7")
    c.box(45,260,330,205,"React + Vite",[
        "Screens and route guards", "Shared authenticated API client", "Local drafts and music player", "Calendar snapshot download", "No database access"
    ])
    c.box(45,560,330,150,"Spotify Web Playback",["SDK script from sdk.scdn.co", "Premium: token from the API", "Free: embedded playlist player"],color=AMBER,fill=WARM,size=17)
    c.box(505,230,620,105,"Spring Security",["JWT signature · active account · role · token version"])
    c.box(505,390,280,110,"Controllers",["Validate typed input", "Delegate one service call"])
    c.box(845,390,280,110,"Services",["Authorize current actor", "Coordinate domain rules"])
    c.box(505,580,280,140,"Domain and values",["Entities retain state rules", "Scorers / strategies", "Immutable value objects"])
    c.box(845,580,280,140,"JPA repositories",["Read and persist entities", "Ordered transition locks", "Database constraints"])
    c.box(505,800,620,126,"DTO assemblers",["ProfileViewAssembler picks the permitted response shape.", "PublicProfileDto omits the contact; ConnectedProfileDto has it."],color=TEAL)
    externals=[
        ("Google Gemini",["Explanations, quizzes, study", "plans and the Pixel assistant"]),
        ("Spotify Web API",["Sign-in, token refresh,", "playlist search and playback"]),
        ("Freesound",["CC0 ambient sound search;", "the browser plays previews"]),
        ("Open-Meteo",["Campus weather at fixed", "coordinates; cached, fallback"]),
    ]
    for n,(title,rows) in enumerate(externals):
        y=175+n*136
        c.box(1240,y,315,118,title,rows,color=AMBER,fill=WARM,size=17)
        c.line([(1125,445),(1190,445),(1190,y+59),(1240,y+59)],end="arrow")
    c.label(1190,318,"HTTPS",15)
    c.box(1240,770,315,150,"PostgreSQL",["Managed by Supabase", "Backend JDBC access", "RLS; browser grants revoked"],color=TEAL)
    c.line([(375,285),(425,285),(425,280),(505,280)],end="arrow")
    c.label(393,251,"JWT + JSON",15)
    c.line([(210,465),(210,560)],end="arrow")
    c.label(210,518,"loads script",15)
    c.line([(650,335),(650,390)],end="arrow")
    c.line([(785,445),(845,445)],end="arrow")
    c.line([(930,500),(930,580)],end="arrow")
    c.line([(845,480),(805,480),(805,540),(645,540),(645,580)],end="arrow")
    c.line([(1125,700),(1205,700),(1205,845),(1240,845)],end="arrow")
    c.label(1205,790,"JDBC",16)
    c.line([(865,500),(865,555),(825,555),(825,800)],end="arrow")
    c.label(825,766,"assemble",15)
    c.line([(505,862),(420,862),(420,430),(375,430)],end="arrow",color=TEAL)
    c.label(420,650,["JSON","DTOs"],15,color=TEAL)
    c.note(45,990,1510,["Contact is returned only to its owner or an active accepted buddy. Group membership and admin authority do not grant it.", "Provider keys and the Spotify client secret stay on the backend; the browser receives only a Spotify access token for its own player."],fill="#edf3f6")
    return c


def use_cases():
    c = Canvas("use-cases", "UML use cases — compulsory baseline", "Actors are account capabilities; group leadership is a relationship with one particular group.",height=1350)
    c.rect(230,165,1120,1050,"white","#93a9b6",8,c.back)
    c.text(255,200,"STUDY BUDDY MATCHER",19,BLUE,700)
    c.actor(102,500,"Student")
    c.actor(1485,500,"Administrator")
    student = [
        (260,["Register student account"]),
        (345,["Sign in"]),
        (430,["Edit own profile", "and study preferences"]),
        (515,["Replace / clear", "weekly availability"]),
        (600,["Find and compare", "ranked buddies"]),
        (685,["Read student profile"]),
        (770,["Send / answer requests", "and view / end connections"]),
        (855,["Browse, create and", "apply to study groups"]),
        (940,["Manage a led group", "and membership requests"]),
        (1050,["Read / mark own", "notifications"]),
    ]
    for y, rows in student:
        c.usecase(535,y,470,rows)
        c.line([(140,545),(300,y)])
    # Shared sign-in use case: two explicit actor associations, no invented include/extend.
    c.line([(1447,545),(1290,345),(770,345)])
    for y, rows in [
        (520,["Create / edit / delete", "and activate / deactivate accounts"]),
        (700,["View account status", "and basic usage"]),
        (880,["Configure matching criteria,", "weights and active strategy"]),
    ]:
        c.usecase(1090,y,435,rows)
        c.line([(1447,545),(1307,y)])
    c.note(835,990,470,["Leader operations require group.leader.id", "to equal the current student. No leader", "subclass or global leader role exists."],title="Group-scoped authority",fill=GREEN)
    c.note(275,1130,1015,["Profile reads enforce contact privacy.", "Registration and sign-in are public; other actions need an active account."],fill="#edf3f6")
    c.note(45,1240,1510,["Notation: actor association = solid line; ellipse = use case; rectangle = application boundary. E2 use cases are a separate panel."],fill="#edf3f6")
    return c


def e2_use_cases():
    c = Canvas("e2-use-cases", "UML use cases — collaborative study (E2)", "Selected extension; this panel supplements the compulsory use cases.",height=1170)
    c.rect(230,170,1280,820,"white","#93a9b6",8,c.back)
    c.text(255,206,"STUDY BUDDY MATCHER / E2",19,TEAL,700)
    c.actor(100,450,"Student")
    items=[
        (290,["Join / leave private room", "and view current participants"],["Accepted active group member;", "distinct students count toward capacity."]),
        (405,["Control Pomodoro", "and room configuration"],["Group leader or present assigned host/co-host;", "only the leader assigns host roles."]),
        (520,["Play Spotify and", "ambient sounds"],["Music tab in room settings;", "heard only in this browser."]),
        (635,["Plan / edit / cancel", "group or buddy sessions"],["Group leader or active buddy participant;", "other accepted group members may view."]),
        (750,["View mutual availability", "and export calendar snapshot"],["Current parent relationship required;", "suggestions do not confirm attendance."]),
        (865,["Choose room atmosphere", "and view campus weather"],["Selectable original scenes; optional", "cached Open-Meteo campus conditions."]),
    ]
    for y,rows,note in items:
        c.usecase(595,y,590,rows,True)
        c.line([(138,495),(300,y)])
        c.lines(955,y-8,note,18,MUTED,gap=25)
        c.line([(890,y),(935,y)],dashed=True)
    c.note(45,1020,1510,["No global online status, public room, chat, calendar synchronization or RSVP workflow is implied. Calendar files are private snapshots.", "E1 match explanations have a separate companion view (E1.md)."],fill="#edf3f6")
    return c


def identity_profile():
    c=Canvas("identity-profile", "UML classes — identity and study profile", "Concrete account and profile entities are associated through a shared identifier; there is no User inheritance tree.",height=1130)
    c.cls(60,210,350,"User",["- id: Long", "- email: String", "- passwordHash: String", "- role: Role", "- active: boolean", "- tokenVersion: long", "- lastLoginAt: Instant"])
    c.cls(610,180,480,"Student",["- id: Long", "- user: User", "- name, school, programme: String", "- yearOfStudy: Integer", "- contactNumber: String", "- coursesTaken: Set<Course>", "- targetCourse: Course", "- preferredStudyMode: StudyMode", "- preferredGroupSizeMin/Max: Integer", "- studyGoals: Set<StudyGoal>"])
    c.cls(1260,210,280,"Course",["- id: Long", "- code: String", "- name: String"])
    c.cls(610,720,480,"AvailabilitySlot",["- student: Student", "- dayOfWeek: DayOfWeek", "- startTime, endTime: LocalTime"])
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
    c=Canvas("relationship-domain", "UML classes — relationships and group authority", "Requests hold decision history; active connections and accepted memberships are distinct domain objects.",height=1390)
    c.cls(60,200,410,"MatchRequest",["- sender, receiver: Student", "- message: String", "- status: MatchRequestStatus", "- respondedAt: Instant"],["+ accept(): void", "+ decline(): void", "+ cancel(): void"])
    c.cls(1130,200,410,"Connection",["- studentA, studentB: Student", "- endedAt: Instant"],["+ isActive(): boolean", "+ involves(studentId: Long): boolean", "+ otherStudent(studentId: Long): Student", "+ end(): void"],size=16)
    c.cls(600,585,400,"Student",["- id: Long", "- name: String"])
    c.cls(60,665,410,"GroupMembership",["- studyGroup: StudyGroup", "- student: Student", "- joinedAt: Instant"])
    c.cls(1130,665,410,"GroupJoinRequest",["- studyGroup: StudyGroup", "- student: Student", "- status: GroupJoinRequestStatus"],["+ accept(): void", "+ reject(): void"])
    c.cls(600,950,400,"StudyGroup",["- leader: Student", "- course: Course [1]", "- maxGroupSize: Integer", "- active: boolean"],["+ isLeader(studentId: Long): boolean", "+ close(): void"],size=17)
    # Distinct sender/receiver and studentA/studentB roles are deliberately separate links.
    c.line([(175,451),(175,515),(600,620)])
    c.line([(365,451),(365,490),(600,654)])
    c.label(160,490,"0..*",17)
    c.label(370,474,"0..*",17)
    c.label(405,540,"sender",17)
    c.label(475,584,"receiver",17)
    c.label(575,617,"1",17)
    c.label(575,656,"1",17)
    c.line([(1235,425),(1235,500),(1000,620)])
    c.line([(1445,425),(1445,545),(1000,654)])
    c.label(1215,472,"0..*",17)
    c.label(1455,495,"0..*",17)
    c.label(1135,541,"studentA",17)
    c.label(1175,595,"studentB",17)
    c.label(1024,617,"1",17)
    c.label(1024,658,"1",17)
    c.line([(470,725),(600,690)])
    c.label(505,704,"0..*",17)
    c.label(578,713,"1",17)
    c.line([(1130,725),(1000,690)])
    c.label(1092,704,"0..*",17)
    c.label(1022,714,"1",17)
    c.line([(800,706),(800,950)])
    c.label(825,744,"1",18)
    c.label(833,921,"0..*",18)
    c.label(800,815,"leader",18)
    c.line([(265,812),(265,1060),(600,1060)])
    c.label(290,857,"0..*",17)
    c.label(565,1043,"1",17)
    c.label(403,1043,"studyGroup",17)
    c.line([(1335,884),(1335,1060),(1000,1060)])
    c.label(1360,976,"0..*",17)
    c.label(1030,1043,"1",17)
    c.label(1195,1043,"studyGroup",17)
    c.note(45,1240,1510,["Each group has exactly one student leader who also has an accepted membership. The capacity rule counts the leader.", "Each request/connection has two distinct students. Connections are active exactly while endedAt is null; accepted group membership has no request status."],fill="#edf3f6")
    return c


def matching_design():
    c=Canvas("matching-design", "UML classes — scoring and ranking", "Independent scoring, ordering and wording responsibilities keep the matching engine explainable.",width=1800,height=1330)
    c.panel(45,170,840,780,"SCORING: PURE COLLABORATORS")
    c.panel(915,170,840,780,"RANKING: STRATEGY + TEMPLATE METHOD")
    c.cls(75,245,350,"WeightedMatchScorer",[],["+ score(searcher, candidate, search,", "  weights, enabled): MatchScore"],size=17)
    c.cls(485,225,370,"CriterionScorer",[],["+ criterion(): MatchingCriterion", "+ score(searcher, candidate,", "  search): CriterionResult"],stereotype="interface",size=17)
    c.cls(75,520,350,"MatchScore",["breakdown:", "  Map<MatchingCriterion, Double>", "total: double", "sharedHoursPerWeek: double", "quality: MatchQuality", "unknownCriteria:", "  Set<MatchingCriterion>"],stereotype="record",size=17)
    implementations=["CourseScorer","AvailabilityOverlapScorer","StudyModeScorer","StudyGoalScorer","GroupSizeScorer"]
    for i,name in enumerate(implementations):
        c.cls(485,490+i*82,370,name,[],[],size=18)
        c.line([(485,515+i*82),(455,515+i*82)],dashed=True)
    c.line([(455,843),(455,415),(655,415),(655,393)],dashed=True,end="triangle")
    c.line([(425,315),(485,315)],end="arrow")
    c.label(454,282,"uses",15)
    c.line([(248,366),(248,520)],dashed=True,end="arrow")
    c.label(248,450,"creates",17)
    c.cls(1125,225,430,"MatchingStrategy",[],["+ type(): MatchingStrategyType", "+ ranking(enabled): Comparator<MatchScore>"],stereotype="interface",size=17)
    c.cls(945,495,340,"BalancedStrategy",[],["+ ranking(enabled)"])
    c.cls(1355,465,365,"PriorityTierStrategy",[],["+ tierOf(score): int", "+ ranking(enabled)", "# priority(): MatchingCriterion", "# priorityValue(score): double"],stereotype="abstract",size=17)
    c.line([(1115,495),(1115,430),(1240,430),(1240,367)],dashed=True,end="triangle")
    c.line([(1538,465),(1538,430),(1440,430),(1440,367)],dashed=True,end="triangle")
    c.cls(955,790,350,"AvailabilityFirstStrategy",[],[])
    c.cls(1370,790,350,"CourseFirstStrategy",[],[])
    c.line([(1130,790),(1130,735),(1480,735),(1480,659)],end="triangle")
    c.line([(1545,790),(1545,659)],end="triangle")
    c.note(945,875,775,["Priority bands first, then total; a disabled priority falls back to total."],fill="#edf3f6")
    c.box(45,990,530,165,"MatchService",["search(criteria, actorId): MatchSearchResultDto", "One configuration snapshot; filter, score,", "threshold, order, limit and assemble."],size=18)
    c.box(635,990,510,165,"MatchingConfigService",["snapshot(): MatchingConfigSnapshot", "Strategy + weights + enabled criteria", "+ minimum score + persistent version"],size=18)
    c.box(1205,990,550,165,"MatchResultAssembler",["toDto(...): MatchResultDto", "Uses MatchExplainer / TemplateMatchExplainer", "for deterministic text; exposes no contact."],color=TEAL,size=18)
    c.line([(575,1070),(635,1070)],end="arrow")
    c.line([(310,990),(310,973),(1480,973),(1480,990)],end="arrow")
    c.note(45,1190,1710,["Dashed line + hollow triangle = interface implementation; solid line + hollow triangle = class inheritance. Ordinary arrows are dependencies.", "Signatures omit repeated generic/parameter types for readability. CriterionResult records score + known; immutable interval values appear in the detailed matching source."],fill="#edf3f6")
    return c


def matching_sequence():
    c=Canvas("matching-sequence", "UML sequence — deterministic match discovery", "GET /api/matches: source-ordered filtering and ranking, safe DTO output, explicit failure and empty-result paths.",width=1800,height=1390)
    xs=[150,440,760,1085,1390,1660]
    names=[["Find buddies /", "Home preview"],["Security +", "MatchController"],["MatchService"],["AccountAccess / config", "/ repositories"],["WeightedMatchScorer"],["MatchResultAssembler"]]
    for x,name in zip(xs,names):
        c.rect(x-122,170,244,70,"white",BLUE,4)
        c.lines(x,198 if len(name)>1 else 211,name,18,BLUE,gap=25,anchor="middle")
        c.line([(x,240),(x,1225)],dashed=True,color="#8ca1ad")
    def msg(a,b,y,rows,dashed=False):
        c.line([(xs[a],y),(xs[b],y)],dashed=dashed,end="arrow")
        c.label((xs[a]+xs[b])/2,y-13,rows,17)
    msg(0,1,285,"course / goal + filters + JWT")
    c.fragment(45,310,1710,94,"break","[missing / invalid authentication or wrong role]")
    msg(1,0,375,"401 / 403; no matching evaluation",True)
    msg(1,2,440,"search(criteria, actorId)")
    c.fragment(45,462,1710,95,"break","[invalid context / limit / filter or unknown course; active student rechecked]")
    msg(2,1,515,"400 / 404 (or revoked-account 401)",True)
    msg(1,0,545,"safe problem response",True)
    c.label(900,586,["requireStudent; eligible profiles + slots;", "config.snapshot(); relationships.of(actor)"],17)
    c.line([(760,627),(1085,627)],end="arrow")
    c.line([(1085,650),(760,650)],end="arrow",dashed=True)
    c.label(916,684,"one repeatable-read database snapshot",16)
    c.fragment(45,715,1710,155,"loop","[each active candidate except self / current buddies, accepted by MatchSearchFilter]")
    msg(2,4,785,"score(searcher, candidate, search, weights, enabled)")
    msg(4,2,842,"MatchScore: total, quality, unknown criteria, shared hours",True)
    c.rect(565,900,605,118,PALE,BLUE,5)
    c.lines(585,930,["Keep total ≥ admin minScore and quality ≥ minQuality.", "Sort: chosen order → strategy → shared hours → name → ID.", "Record totalMatches, then apply the result limit."],17,gap=29)
    msg(2,5,1060,"toDto(public profile, score, shared time, request state)")
    msg(5,2,1110,"MatchResultDto list; template explanation added",True)
    msg(2,1,1160,"MatchSearchResultDto + configurationVersion",True)
    msg(1,0,1210,"200: ordered matches, or [] with totalMatches=0",True)
    c.note(45,1250,1710,["No contact, email, credential, token or persistence entity is returned in discovery. The card renders scores, bars and shared time; wording is fetched on request (E1).", "Grouped lifelines are named collaborators, not additional classes. The optional E1 explanation request has a separate companion view."],fill="#edf3f6")
    return c


def values_privacy():
    c=Canvas("values-and-privacy", "UML classes — immutable values and private response shapes", "Value objects protect interval rules; a sealed DTO boundary keeps contact disclosure on the server.",width=1800,height=1240)
    c.panel(45,170,830,850,"IMMUTABLE WEEKLY TIME VALUES")
    c.panel(905,170,850,850,"PROFILE RESPONSE POLICY",color=TEAL,fill=GREEN)
    c.cls(100,240,720,"WeeklyAvailability",["slots: List<TimeSlot>"],["+ totalMinutes(): long", "+ sharedMinutesWith(other: WeeklyAvailability): long", "+ sharedSlotsWith(other: WeeklyAvailability): List<TimeSlot>", "+ sharedSessionsWith(other, minimumSessionMinutes): List<TimeSlot>"],stereotype="record",size=18)
    c.cls(100,650,720,"TimeSlot",["day: DayOfWeek", "start, end: LocalTime"],["+ durationMinutes(): long", "+ overlapMinutes(other: TimeSlot): long", "+ intersection(other: TimeSlot): Optional<TimeSlot>"],stereotype="record",size=18)
    c.nodes.append(f'<polygon points="460,480 452,492 460,504 468,492" fill="{LINE}" stroke="{LINE}"/>')
    c.line([(460,504),(460,650)])
    c.label(485,525,"1",18)
    c.label(502,625,"0..*",18)
    c.label(460,575,"slots (value composition)",17)
    c.cls(955,235,750,"ProfileViewAssembler",[],["+ assemble(subject: Student, viewerId: Long): ProfileDto"],color=TEAL,size=19)
    c.cls(1090,465,480,"ProfileDto",[],["+ id(): Long"],stereotype="sealed interface",color=TEAL)
    c.line([(1330,330),(1330,465)],dashed=True,end="arrow")
    c.label(1330,405,"creates one permitted response shape",17)
    c.cls(935,720,385,"PublicProfileDto",["id: Long", "name: String", "availability: List<...>", "relationship: ProfileRelationshipDto"],stereotype="record",color=TEAL,size=16)
    c.cls(1350,720,385,"ConnectedProfileDto",["id: Long", "name: String", "contactNumber: String", "availability: List<...>", "relationship: ProfileRelationshipDto"],stereotype="record",color=TEAL,size=16)
    c.line([(1128,720),(1128,660),(1220,660),(1220,581)],dashed=True,end="triangle")
    c.line([(1543,720),(1543,660),(1440,660),(1440,581)],dashed=True,end="triangle")
    c.label(1050,636,"otherwise",17)
    c.label(1600,636,["SELF /","CONNECTED"],17)
    c.note(45,1060,1710,["WeeklyAvailability canonicalizes duplicate/overlapping/adjacent blocks, then intersects unique time. Half-open intervals that only touch share no minutes.", "PublicProfileDto has no contactNumber field. ConnectedProfileDto includes it only for the owner or an active accepted buddy; groups/admin status do not qualify.", "Selected members are shown. Both profile records implement ProfileDto independently; ConnectedProfileDto does not inherit PublicProfileDto."],fill="#edf3f6")
    return c


def collaborative_study():
    c=Canvas("collaborative-study", "UML collaborators — rooms, sessions and weather", "Three bounded E2 responsibilities; room presence, durable shared state and dated plans have separate lifecycles.",width=1800,height=1380)
    c.panel(45,170,560,1000,"PRIVATE GROUP ROOM",color=TEAL,fill=GREEN)
    c.panel(625,170,560,1000,"DATED GROUP / BUDDY SESSIONS",color=TEAL,fill=GREEN)
    c.panel(1205,170,550,1000,"OPTIONAL CAMPUS WEATHER",color=TEAL,fill=GREEN)
    c.cls(85,235,480,"StudyRoomService",[],["+ state(groupId, actorId): StudyRoomDto", "+ join(...), heartbeat(...): StudyRoomDto", "+ leave(...): void", "+ controlTimer(...),", "  configure(...): StudyRoomDto"],color=TEAL,size=17)
    c.cls(85,505,480,"StudyRoom",["- studyGroup: StudyGroup [1]", "- host, coHost: Student [0..1 each]", "- version: long"],["+ timer(): PomodoroTimer", "+ applyTimer(timer): void"],color=TEAL,size=18)
    c.cls(85,795,480,"RoomPresence",["- studyRoom: StudyRoom [1]", "- student: Student [1]", "- clientId: UUID", "- expiresAt, observedAt: Instant"],["+ renew(presence, now, expiresAt): void"],color=TEAL,size=17)
    c.line([(325,435),(325,505)],end="arrow")
    c.label(325,477,"coordinates",17)
    c.line([(325,724),(325,795)])
    c.label(349,748,"1",17)
    c.label(361,779,"0..*",17)
    c.note(75,1065,500,["Read / renew: membership check; no group lock.", "Join / control: account locks → group lock."],fill="white")
    c.cls(665,235,480,"StudySessionService",[],["+ createGroup(...), createBuddy(...)", "+ edit(...), cancel(...): SessionDto", "+ calendar(id, actorId): CalendarExportDto"],color=TEAL,size=17)
    c.cls(665,460,480,"StudySession",["- group: StudyGroup [0..1]", "- connection: Connection [0..1]", "- version: long", "- status: SessionStatus"],["+ schedule(): SessionSchedule", "+ edit(details, now): void", "+ cancel(now): void"],color=TEAL,size=18)
    c.cls(665,825,480,"SessionSchedule",["startLocal, endLocal: LocalDateTime", "timeZone: String", "occurrenceCount: int"],["+ occurrences(): List<SessionOccurrenceDto>"],stereotype="record",color=TEAL,size=17)
    c.line([(905,362),(905,460)],end="arrow")
    c.label(905,416,"authorizes via SessionAccess",17)
    c.line([(905,711),(905,825)],end="arrow",dashed=True)
    c.label(905,786,"constructs immutable value",17)
    c.note(655,1090,500,["Exactly one parent: group XOR connection.", "SessionCalendar emits private UTC snapshots."],fill="white")
    c.cls(1245,235,470,"WeatherService",[],["+ current(): WeatherDto"],color=TEAL,size=18)
    c.cls(1245,455,470,"WeatherClient",[],["+ fetch(): WeatherSample"],stereotype="interface",color=TEAL,size=18)
    c.cls(1245,685,470,"OpenMeteoClient",[],["+ fetch(): WeatherSample"],color=TEAL,size=18)
    c.line([(1480,330),(1480,455)],end="arrow")
    c.label(1480,401,"bounded cache / retry",17)
    c.line([(1480,685),(1480,571)],end="triangle",dashed=True)
    c.box(1245,915,470,135,"Open-Meteo API",["Fixed campus coordinates and weather fields.", "No student identity, location or schedule."],color=AMBER,fill=WARM,size=18)
    c.line([(1480,780),(1480,915)],end="arrow")
    c.label(1480,856,"Java HTTP client + timeout",17)
    c.note(45,1210,1710,["RoomAccess / SessionAccess check current account and parent authority; RoomAssembler / SessionAssembler / WeatherAssembler return DTOs.", "PomodoroTimer is an immutable server-time value. SessionAvailability reuses WeeklyAvailability; SessionEvents writes recipient notifications transactionally.", "Detailed E2 transaction and ER diagrams are in TEAM_C.md. E1 match explanations have their own companion view in E1.md."],fill="#edf3f6")
    return c


BUILDERS = [architecture,use_cases,e2_use_cases,identity_profile,relationships,matching_design,values_privacy,matching_sequence,collaborative_study]


def verify_classes(svg):
    """Catch renamed/nonexistent Java classes in the presentation class panels."""
    root=ET.fromstring(svg)
    source_names={path.stem for path in (ROOT/"backend/src/main/java").rglob("*.java")}
    missing={element.attrib["data-class"] for element in root.iter() if "data-class" in element.attrib} - source_names
    if missing:
        raise ValueError("Diagram references absent Java classes: "+", ".join(sorted(missing)))


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check",action="store_true",help="Validate SVGs and fail if committed exports differ from source")
    args=parser.parse_args()
    changed=[]
    for builder in BUILDERS:
        diagram=builder()
        svg=diagram.svg()
        verify_classes(svg)
        output=HERE/"exports"/(diagram.slug+".svg")
        if args.check:
            if not output.exists() or output.read_text()!=svg:
                changed.append(str(output.relative_to(ROOT)))
        else:
            output.parent.mkdir(exist_ok=True)
            output.write_text(svg)
            print(output.relative_to(ROOT))
    if changed:
        print("Stale or missing exports:\n"+"\n".join(changed),file=sys.stderr)
        return 1
    if args.check:
        print(f"Verified {len(BUILDERS)} deterministic SVG exports, XML syntax and referenced Java class names.")
    return 0


if __name__=="__main__":
    raise SystemExit(main())
