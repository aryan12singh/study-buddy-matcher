package com.studybuddy.weather;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Uses the existing authenticated-student API guard. */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private final WeatherService service;
    public WeatherController(WeatherService service) { this.service = service; }
    @GetMapping
    public WeatherDto current() { return service.current(); }
}
