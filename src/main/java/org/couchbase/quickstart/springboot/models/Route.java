package org.couchbase.quickstart.springboot.models;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Route {

    @NotBlank(message = "ID is mandatory")
    private String id;

    @NotBlank(message = "Type is mandatory")
    private String type;

    @NotBlank(message = "Airline is mandatory")
    private String airline;

    @NotBlank(message = "Airline ID is mandatory")
    private String airlineid;

    @NotBlank(message = "Source airport is mandatory")
    private String sourceairport;

    @NotBlank(message = "Destination airport is mandatory")
    private String destinationairport;

    @NotNull(message = "Stops is mandatory")
    private int stops;

    @NotBlank(message = "Equipment is mandatory")
    private String equipment;

    @Valid
    private List<Schedule> schedule;

    @NotNull(message = "Distance is mandatory")
    private double distance;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Schedule {

        @NotNull(message = "Day is mandatory")
        private int day;

        @NotBlank(message = "UTC is mandatory")
        private String utc;

        @NotBlank(message = "Flight is mandatory")
        private String flight;
        
    }
}