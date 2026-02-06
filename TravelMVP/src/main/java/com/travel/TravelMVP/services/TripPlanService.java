package com.travel.TravelMVP.services;

import com.travel.TravelMVP.dtos.JourneyInputDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TripPlanService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.endpoint}")
    private String geminiUrl;

    public Map<String, Object> generatePlan(JourneyInputDto dto) {
        if (dto.getTravelWith() == null || dto.getBudgetType() == null ||
                dto.getTripDays() == null || dto.getTripDays() <= 0 ||
                dto.getInterests() == null || dto.getInterests().isEmpty()) {
            throw new IllegalArgumentException("Missing required trip data.");
        }

        String interestsStr = String.join(", ", dto.getInterests());

        String prompt ="""
    You are an expert travel consultant specializing in cross-country itineraries for Azerbaijan.

    GOAL: Create a comprehensive day-by-day travel itinerary that covers multiple regions of Azerbaijan based on the trip duration.

    INPUT:
    Travel destination: Azerbaijan
    Travel style: %s
    Travel with: %s
    Adults: %d
    Children: %d
    Interests: %s
    Budget: %s
    Trip days: %d
    Currency: %s

    STRICT RULES:
    - Do not limit the plan to only one city. If the trip is more than 3 days, suggest moving between regions.
    - The "trip_summary" must be exactly 2-3 sentences long.
    - DIVIDE EACH DAY into exactly three time slots: "Morning (09:00-12:00)", "Afternoon (13:00-18:00)", and "Evening (19:00+)".
    - Each time slot MUST have at least 1 specific activity.
    - "place_name" is mandatory and cannot be empty for any activity.
    - Match activities to these interests: %s.
    - If 'gastronomy' is selected, mention specific regional dishes.
    - Respond ONLY with valid JSON. No markdown (no ```json), no extra text.

    RESPONSE FORMAT:
    {
      "trip_summary": "Summary text...",
      "days": [
        {
          "day": 1,
          "city": "Current City/Region",
          "itinerary": [
            {
              "time_slot": "Morning (09:00-12:00)",
              "activities": [
                { "place_name": "Location Name", "description": "Short info" }
              ]
            },
            {
              "time_slot": "Afternoon (13:00-18:00)",
              "activities": [
                { "place_name": "Location Name", "description": "Short info" }
              ]
            },
            {
              "time_slot": "Evening (19:00+)",
              "activities": [
                { "place_name": "Location Name", "description": "Short info" }
              ]
            }
          ]
        }
      ]
    }
    """.formatted(
                dto.getTravelStyle(), dto.getTravelWith(), dto.getAdultsCount(),
                dto.getChildrenCount(), interestsStr, dto.getBudgetType(),
                dto.getTripDays(), dto.getCurrency() != null ? dto.getCurrency() : "AZN",
                interestsStr
        );
        Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        try {
            WebClient webClient = WebClient.builder().defaultHeader("Content-Type", "application/json").build();

            Map response = webClient.post()
                    .uri(geminiUrl + "?key=" + geminiApiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("candidates")) {
                throw new RuntimeException("AI response is empty");
            }

            List<Map> candidates = (List<Map>) response.get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            String aiText = parts.get(0).get("text").toString();

            Matcher matcher = Pattern.compile("\\{.*\\}", Pattern.DOTALL).matcher(aiText.trim());
            String cleanedJson = matcher.find() ? matcher.group() : aiText;

            System.out.println("======= AI CAVABI  =======");
            System.out.println(cleanedJson);
            System.out.println("======================================");

            return new ObjectMapper().readValue(cleanedJson, Map.class);

        } catch (Exception e) {
            System.err.println("Xəta baş verdi: " + e.getMessage());
            throw new RuntimeException("Could not generate trip plan. Please try again.");
        }
    }
}