package com.hotel.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class BookingRequest {
    @NotNull private Long roomId;
    @NotNull private Long guestId;
    @NotNull private LocalDate checkIn;
    @NotNull private LocalDate checkOut;
}
