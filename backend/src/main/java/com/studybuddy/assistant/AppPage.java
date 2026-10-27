package com.studybuddy.assistant;

/**
 * The app's pages the assistant can send a student to, with what each is for. The AI only picks
 * one of these names; the wording and the link come from here, so it can never invent a page or
 * describe one wrongly. {@code {me}} in a path stands for the student's own ID.
 */
public enum AppPage {
    HOME("Home", "/home", "your next steps, requests waiting for you and upcoming sessions at a glance"),
    FIND_BUDDIES("Find buddies", "/find-buddies",
            "search for students by course or study goal, compare match scores and send a match request"),
    BUDDY_REQUESTS("Buddy requests", "/connections?view=incoming",
            "accept or decline requests other students sent you; accepting shares your contact numbers with each other"),
    CONNECTIONS("Connections", "/connections",
            "your study buddies and their contact numbers, the requests you sent, and sessions with a buddy"),
    STUDY_GROUPS("Study groups", "/groups", "browse groups by course and apply to join, or open the groups you're in"),
    CREATE_GROUP("Create a study group", "/groups/new",
            "start your own group: its name, course, study goals, size and weekly meeting times"),
    STUDY_SESSIONS("Study sessions", "/sessions",
            "your upcoming group and buddy sessions, which you can add to your calendar"),
    STUDY_ROOM("Study Room", "/study-room",
            "your focus timer and music, Quiz Buddy, and the way into your groups' shared study rooms"),
    ACHIEVEMENTS("Achievements", "/achievements", "your study streaks and badges"),
    NOTIFICATIONS("Notifications", "/notifications", "everything that happened recently, such as accepted requests and new sessions"),
    MY_PROFILE("My study profile", "/students/{me}", "what other students see about you"),
    EDIT_PROFILE("Edit my profile", "/students/{me}/edit",
            "change your courses, study goals, study mode, preferred group size and weekly availability"),
    HELP("Help", "/help", "step-by-step guides to every part of the app");

    private final String title;
    private final String path;
    private final String purpose;

    AppPage(String title, String path, String purpose) {
        this.title = title;
        this.path = path;
        this.purpose = purpose;
    }

    public String title() { return title; }
    public String purpose() { return purpose; }

    /** The page's link for this student. */
    public String pathFor(Long studentId) {
        return path.replace("{me}", String.valueOf(studentId));
    }
}
