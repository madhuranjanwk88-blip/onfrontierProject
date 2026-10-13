package com.hotel.repository;

import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByGuestId(Long guestId);

    @Query("select count(b) from Booking b where b.room.id = :roomId and b.status in :statuses " +
           "and b.checkIn < :checkOut and b.checkOut > :checkIn")
    long countOverlaps(@Param("roomId") Long roomId,
                       @Param("checkIn") LocalDate checkIn,
                       @Param("checkOut") LocalDate checkOut,
                       @Param("statuses") Collection<BookingStatus> statuses);
}
