package com.studybuddy.studysession;

/** An iCalendar file for one study session: its download filename and text content. */
public record CalendarExportDto(String filename, String content) { }
