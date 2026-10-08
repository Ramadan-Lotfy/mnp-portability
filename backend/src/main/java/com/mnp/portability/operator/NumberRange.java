package com.mnp.portability.operator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "number_ranges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NumberRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "range_start", nullable = false, length = 11)
    private String rangeStart;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "range_end", nullable = false, length = 11)
    private String rangeEnd;
}