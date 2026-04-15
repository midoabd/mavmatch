package com.mavmatch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student1_id", nullable = false)
    private Student student1;

    @ManyToOne
    @JoinColumn(name = "student2_id", nullable = false)
    private Student student2;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private double overlapHours;

    @Column(nullable = false, updatable = false)
    private LocalDateTime matchedAt = LocalDateTime.now();
}