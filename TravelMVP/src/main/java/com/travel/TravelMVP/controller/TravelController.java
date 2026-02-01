package com.travel.TravelMVP.controller;

import com.travel.TravelMVP.dtos.JourneyInputDto;
import com.travel.TravelMVP.services.TripPlanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@Controller
public class TravelController {

    private final TripPlanService tripPlanService;

    public TravelController(TripPlanService tripPlanService) {
        this.tripPlanService = tripPlanService;
    }

    @GetMapping("/")
    public String getTravelPage() {
        return "index";
    }

    @PostMapping("/get-plan")
    public String getPlanFromAI(JourneyInputDto dto, Model model) {

        try {
            Map<String, Object> plan = tripPlanService.generatePlan(dto);
            model.addAttribute("aiFeedback", plan);
        } catch (IllegalArgumentException e) {
            model.addAttribute("aiFeedback", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("aiFeedback",
                    "Could not generate trip plan. Please try again.");
        }

        return "result";
    }
}
