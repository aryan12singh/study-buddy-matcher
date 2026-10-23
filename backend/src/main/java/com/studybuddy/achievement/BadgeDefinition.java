package com.studybuddy.achievement;

/** The fixed catalogue of badges: what each one is called and what it counts. */
public enum BadgeDefinition {
    CONNECTOR("Connector", "Active accepted buddy connections. Disconnecting lowers this count.", "active connection", "active connections"),
    FIRST_MOVE("First Move", "Retained match requests you sent, in any status, including declined or cancelled requests.", "sent request", "sent requests"),
    TEAM_PLAYER("Team Player", "Open groups you currently belong to without leading. Leaving, removal or closure lowers this count.", "open group membership", "open group memberships"),
    FOUNDER("Founder", "Retained groups you created, including closed groups.", "created group", "created groups"),
    PLANNER("Planner", "Planned occurrences on your active buddy and group schedules, from your current join or connection date onward. Includes past and future dates, excludes cancelled sessions.", "planned occurrence", "planned occurrences"),
    GROVE_KEEPER("Grove Keeper", "Longest planned-session weekly run in your currently eligible schedules. Recomputed when those schedules change.", "consecutive planned week", "consecutive planned weeks") {
        @Override
        public String hint(long current, long target, BadgeTier next) {
            return "Plan " + target + " consecutive weeks to reach " + tierLabel(next)
                    + ". Your longest eligible run is " + current + ".";
        }
    };

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

    public String hint(long current, long target, BadgeTier next) {
        long remaining = target - current;
        return remaining + " more " + (remaining == 1 ? unitOne : unitMany) + " to reach " + tierLabel(next) + ".";
    }

    private static String tierLabel(BadgeTier tier) {
        return tier.name().charAt(0) + tier.name().substring(1).toLowerCase();
    }
}
