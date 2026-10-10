package ru.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WeatherApp {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("YANDEX_WEATHER_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println(
                    "Ошибка: не задана переменная YANDEX_WEATHER_API_KEY"
            );
            return;
        }

        int limit = 5;

        String url = "https://api.weather.yandex.ru/v2/forecast"
                + "?lat=59.93&lon=30.31&limit=" + limit;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("X-Yandex-Weather-Key", apiKey)
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("HTTP-статус: " + response.statusCode());
        if (response.statusCode() != 200) {
            System.out.println("Ошибка при получении погоды:");
            System.out.println(response.body());
            return;
        }

        System.out.println("Ответ Яндекс Погоды:");
        System.out.println(response.body());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        JsonNode factTemp = root.path("fact").path("temp");
        if (!factTemp.isNumber()) {
            System.out.println("Не удалось получить текущую температуру.");
            return;
        }

        System.out.println("\nТекущая температура: "
                + factTemp.asInt() + " °C");

        JsonNode forecasts = root.path("forecasts");
        if (!forecasts.isArray() || forecasts.isEmpty()) {
            System.out.println("Прогноз по дням отсутствует.");
            return;
        }

        int daysToCalculate = Math.min(limit, forecasts.size());

        double temperatureSum = 0;
        int daysCount = 0;

        for (int i = 0; i < daysToCalculate; i++) {
            JsonNode forecast = forecasts.get(i);
            JsonNode date = forecast.path("date");
            JsonNode tempAvg = forecast.path("parts")
                    .path("day")
                    .path("temp_avg");

            if (!tempAvg.isNumber()) {
                System.out.println("Нет средней температуры за день: "
                        + date.asText("дата неизвестна"));
                continue;
            }

            double temperature = tempAvg.asDouble();
            temperatureSum += temperature;
            daysCount++;

            System.out.printf(
                    "Дата: %s, средняя дневная температура: %.1f °C%n",
                    date.asText(),
                    temperature
            );
        }

        if (daysCount > 0) {
            double averageTemperature = temperatureSum / daysCount;
            System.out.printf(
                    "%nСредняя температура за %d дней: %.2f °C%n",
                    daysCount,
                    averageTemperature
            );
        } else {
            System.out.println(
                    "Не удалось рассчитать среднюю температуру."
            );
        }
    }
}