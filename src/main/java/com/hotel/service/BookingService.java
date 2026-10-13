package com.hotel.service;

import com.hotel.dto.BookingRequest;
import com.hotel.exception.BadRequestException;
import com.hotel.exception.NotFoundException;
import com.hotel.model.*;
import com.hotel.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class BookingService {
    private final BookingRepository bookings;
    private final RoomService rooms;
    private final GuestService guests;

    public BookingService(BookingRepository bookings, RoomService rooms, GuestService guests) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.guests = guests;
    }

    public Booking create(BookingRequest req) {
        LocalDate in = req.getCheckIn(), out = req.getCheckOut();
        if (in.isBefore(LocalDate.now())) throw new BadRequestException("Check-in cannot be in the past");
        if (!out.isAfter(in)) throw new BadRequestException("Check-out must be after check-in");

        Room room = rooms.get(req.getRoomId());
        Guest guest = guests.get(req.getGuestId());
        if (!room.isActive()) throw new BadRequestException("Room is not available (maintenance)");

        if (bookings.countOverlaps(room.getId(), in, out, RoomService.ACTIVE) > 0)
            throw new BadRequestException("Room " + room.getRoomNumber() + " is already booked for these dates");

        long nights = ChronoUnit.DAYS.between(in, out);
        Booking b = new Booking();
        b.setRoom(room);
        b.setGuest(guest);
        b.setCheckIn(in);
        b.setCheckOut(out);
        b.setTotalAmount(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)));
        b.setStatus(BookingStatus.BOOKED);
        return bookings.save(b);
    }

    @Transactional(readOnly = true)
    public List<Booking> findAll() { return bookings.findAll(); }

    @Transactional(readOnly = true)
    public List<Booking> byGuest(Long guestId) { return bookings.findByGuestId(guestId); }

    @Transactional(readOnly = true)
    public Booking get(Long id) {
        return bookings.findById(id).orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));
    }

    public Booking checkIn(Long id) {
        Booking b = get(id);
        if (b.getStatus() != BookingStatus.BOOKED) throw new BadRequestException("Only BOOKED reservations can check in");
        b.setStatus(BookingStatus.CHECKED_IN);
        return b;
    }

    public Booking checkOut(Long id) {
        Booking b = get(id);
        if (b.getStatus() != BookingStatus.CHECKED_IN) throw new BadRequestException("Guest is not checked in");
        b.setStatus(BookingStatus.CHECKED_OUT);
        return b;
    }

    public Booking cancel(Long id) {
        Booking b = get(id);
        if (b.getStatus() != BookingStatus.BOOKED) throw new BadRequestException("Only BOOKED reservations can be cancelled");
        b.setStatus(BookingStatus.CANCELLED);
        return b;
    }
}
