package com.hotel.service;

import com.hotel.exception.NotFoundException;
import com.hotel.model.BookingStatus;
import com.hotel.model.Room;
import com.hotel.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class RoomService {
    static final List<BookingStatus> ACTIVE = Arrays.asList(BookingStatus.BOOKED, BookingStatus.CHECKED_IN);

    private final RoomRepository repo;

    public RoomService(RoomRepository repo) { this.repo = repo; }

    public Room create(Room r) { r.setId(null); return repo.save(r); }

    @Transactional(readOnly = true)
    public List<Room> findAll() { return repo.findAll(); }

    @Transactional(readOnly = true)
    public Room get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Room " + id + " not found"));
    }

    public Room update(Long id, Room in) {
        Room r = get(id);
        r.setRoomNumber(in.getRoomNumber());
        r.setType(in.getType());
        r.setPricePerNight(in.getPricePerNight());
        r.setCapacity(in.getCapacity());
        r.setActive(in.isActive());
        return repo.save(r);
    }

    public void delete(Long id) { repo.delete(get(id)); }

    @Transactional(readOnly = true)
    public List<Room> available(LocalDate in, LocalDate out) {
        return repo.findAvailable(in, out, ACTIVE);
    }
}
