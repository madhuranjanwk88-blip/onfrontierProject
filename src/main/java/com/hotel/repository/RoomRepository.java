package com.hotel.repository;

import com.hotel.model.BookingStatus;
import com.hotel.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("select r from Room r where r.active = true and r.id not in (" +
           "select b.room.id from Booking b where b.status in :statuses " +
           "and b.checkIn < :checkOut and b.checkOut > :checkIn)")
    List<Room> findAvailable(@Param("checkIn") LocalDate checkIn,
                             @Param("checkOut") LocalDate checkOut,
                             @Param("statuses") Collection<BookingStatus> statuses);
}
