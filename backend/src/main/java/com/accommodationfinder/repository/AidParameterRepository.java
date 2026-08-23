package com.accommodationfinder.repository;

import com.accommodationfinder.model.AidParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AidParameterRepository extends JpaRepository<AidParameter, Long> {

    Optional<AidParameter> findByKey(String key);

    List<AidParameter> findAllByOrderByKeyAsc();
}
