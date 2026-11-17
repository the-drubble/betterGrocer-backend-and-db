package com.team6.backend.app.repositories;

import com.team6.backend.app.entities.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, String> {}

