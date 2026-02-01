package com.travel.TravelMVP.dtos;

import lombok.Data;

import java.util.List;
@Data
public class JourneyInputDto {
    String travelStyle;
    String travelWith;
    int adultsCount;
    int childrenCount;
    List<String> interests;
    String budgetType;
    Integer tripDays;
    private String currency;
}
