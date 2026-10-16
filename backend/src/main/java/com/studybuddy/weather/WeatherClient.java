package com.studybuddy.weather;

import java.io.IOException;

public interface WeatherClient {
    WeatherSample fetch() throws IOException, InterruptedException;
}
