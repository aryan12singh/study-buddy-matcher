package com.studybuddy.studysession;

import com.studybuddy.common.InputRules;
import com.studybuddy.common.error.InvalidInputException;
import java.net.URI;
import java.net.URISyntaxException;

public record SessionDetails(String title, SessionSchedule schedule, String agenda, String location, String roomLink) {
    public static SessionDetails validate(SessionInput input, SessionSettings settings, Long groupId) {
        String title = InputRules.required(input.title(), "Title", InputRules.TEXT_LIMIT);
        String agenda = InputRules.optional(input.agenda(), "Agenda", InputRules.DESCRIPTION_LIMIT);
        String location = InputRules.optional(input.location(), "Location", InputRules.TEXT_LIMIT);
        String link = InputRules.optional(input.roomLink(), "roomLink", 2048);
        if (link != null) validateLink(link, groupId);
        if (title.indexOf('\0') >= 0 || (agenda != null && agenda.indexOf('\0') >= 0)
            || (location != null && location.indexOf('\0') >= 0)) {
            throw new InvalidInputException("title", "Session text cannot contain null characters");
        }
        return new SessionDetails(title, SessionSchedule.validate(input, settings), agenda, location, link);
    }

    private static void validateLink(String link, Long groupId) {
        if (groupId != null && link.equals("/groups/" + groupId + "/room")) return;
        try {
            URI uri = new URI(link);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getHost().contains(".")
                && uri.getUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null) return;
        } catch (URISyntaxException ignored) { }
        throw new InvalidInputException("roomLink", "Use this group's room path or an HTTPS link without credentials, query parameters or fragments");
    }
}
