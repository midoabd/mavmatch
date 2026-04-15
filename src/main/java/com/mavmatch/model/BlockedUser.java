package com.mavmatch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "blocker_id", nullable = false)
    private Student blocker;

    @ManyToOne
    @JoinColumn(name = "blocked_id", nullable = false)
    private Student blocked;

    @Column(nullable = false, updatable = false)
    private LocalDateTime blockedAt = LocalDateTime.now();
}