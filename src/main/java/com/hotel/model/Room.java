package com.hotel.model;

import lombok.Data;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String roomNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RoomType type;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal pricePerNight;

    @Min(1)
    private int capacity = 2;

    /** false = under maintenance / disabled */
    private boolean active = true;
}
