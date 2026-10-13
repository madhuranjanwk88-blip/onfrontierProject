package com.hotel.controller;

import com.hotel.dto.BookingRequest;
import com.hotel.model.Booking;
import com.hotel.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@Valid @RequestBody BookingRequest req) { return service.create(req); }

    @GetMapping
    public List<Booking> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public Booking one(@PathVariable Long id) { return service.get(id); }

    @GetMapping("/guest/{guestId}")
    public List<Booking> byGuest(@PathVariable Long guestId) { return service.byGuest(guestId); }

    @PostMapping("/{id}/check-in")
    public Booking checkIn(@PathVariable Long id) { return service.checkIn(id); }

    @PostMapping("/{id}/check-out")
    public Booking checkOut(@PathVariable Long id) { return service.checkOut(id); }

    @PostMapping("/{id}/cancel")
    public Booking cancel(@PathVariable Long id) { return service.cancel(id); }
}
