package com.studybuddy.weather;

import java.io.IOException;

/**
 * Fetches the current weather from an outside provider. An interface so the service can be tested
 * without the network; {@link OpenMeteoClient} is the real one.
 */
public interface WeatherClient {
    WeatherSample fetch() throws IOException, InterruptedException;
}
