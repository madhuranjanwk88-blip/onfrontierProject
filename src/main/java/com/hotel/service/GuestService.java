package com.hotel.service;

import com.hotel.exception.NotFoundException;
import com.hotel.model.Guest;
import com.hotel.repository.GuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class GuestService {
    private final GuestRepository repo;

    public GuestService(GuestRepository repo) { this.repo = repo; }

    public Guest create(Guest g) { g.setId(null); return repo.save(g); }

    @Transactional(readOnly = true)
    public List<Guest> search(String name) {
        return (name == null || name.trim().isEmpty()) ? repo.findAll()
                : repo.findByFullNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public Guest get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Guest " + id + " not found"));
    }

    public Guest update(Long id, Guest in) {
        Guest g = get(id);
        g.setFullName(in.getFullName());
        g.setEmail(in.getEmail());
        g.setPhone(in.getPhone());
        g.setIdProof(in.getIdProof());
        return repo.save(g);
    }

    public void delete(Long id) { repo.delete(get(id)); }
}
