package com.mnp.portability.operator;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NumberRangeRepository extends JpaRepository<NumberRange, Long> {


    @Query("""
            select r from NumberRange r
            join fetch r.operator
            where r.rangeStart <= :number and r.rangeEnd >= :number
            """)
    Optional<NumberRange> findContaining(@Param("number") String number);
}