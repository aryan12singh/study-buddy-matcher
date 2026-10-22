package com.studybuddy.achievement;

/** The fixed catalogue of badges: what each one is called and what it counts. */
public enum BadgeDefinition {
    CONNECTOR("Connector", "Active study connections", "connection", "connections"),
    FIRST_MOVE("First Move", "Match requests you have sent", "match request", "match requests"),
    TEAM_PLAYER("Team Player", "Study groups you joined", "group to join", "groups to join"),
    FOUNDER("Founder", "Study groups you created", "group to create", "groups to create"),
    PLANNER("Planner", "Upcoming and past sessions on your active schedule", "session to schedule", "sessions to schedule"),
    GROVE_KEEPER("Grove Keeper", "Your best run of weeks with a planned session", "week in a row", "weeks in a row");

    private final String title;
    private final String description;
    private final String unitOne;
    private final String unitMany;

    BadgeDefinition(String title, String description, String unitOne, String unitMany) {
        this.title = title;
        this.description = description;
        this.unitOne = unitOne;
        this.unitMany = unitMany;
    }

    public String key() {
        return name();
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String hint(long remaining, BadgeTier next) {
        String label = next.name().charAt(0) + next.name().substring(1).toLowerCase();
        return remaining + " more " + (remaining == 1 ? unitOne : unitMany) + " to reach " + label + ".";
    }
}
