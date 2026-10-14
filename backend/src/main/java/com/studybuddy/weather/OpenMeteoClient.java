package com.studybuddy.weather;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Fixed provider host and server-configured campus coordinates; never sends user input. */
@Component
public class OpenMeteoClient implements WeatherClient {
    private final WeatherSettings settings;
    private final HttpClient http;

    public OpenMeteoClient(WeatherSettings settings) {
        this.settings = settings;
        this.http = HttpClient.newBuilder().connectTimeout(settings.timeout()).build();
    }

    @Override
    public WeatherSample fetch() throws IOException, InterruptedException {
        var uri = URI.create("https://api.open-meteo.com/v1/forecast?latitude=" + settings.latitude()
            + "&longitude=" + settings.longitude() + "&current=temperature_2m,weather_code,is_day&timeformat=unixtime&timezone=UTC");
        var request = HttpRequest.newBuilder(uri).timeout(settings.timeout()).header("Accept", "application/json").GET().build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Weather provider unavailable");
        return parse(response.body());
    }

    static WeatherSample parse(String body) throws IOException {
        try {
            var current = JsonMapper.builder().build().readTree(body).path("current");
            if (!current.path("weather_code").isInt() || !current.path("temperature_2m").isNumber()
                || !current.path("is_day").isInt() || !current.path("time").isNumber()) {
                throw new IOException("Weather provider returned incomplete conditions");
            }
            double temperature = current.path("temperature_2m").asDouble();
            int day = current.path("is_day").asInt();
            if (!Double.isFinite(temperature) || temperature < -100 || temperature > 70 || (day != 0 && day != 1)) {
                throw new IOException("Weather provider returned invalid conditions");
            }
            return new WeatherSample(WeatherCondition.fromWmo(current.path("weather_code").asInt()), temperature,
                day == 1, Instant.ofEpochSecond(current.path("time").asLong()));
        } catch (RuntimeException invalid) {
            throw new IOException("Weather provider returned invalid conditions", invalid);
        }
    }
}
