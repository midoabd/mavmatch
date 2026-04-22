package com.mavmatch.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRequest {

    public enum Status { PENDING, CONFIRMED, DECLINED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private Student requester;

    private Long receiverId;
    private String partnerName;
    private String courseCode;
    private String meetingDate;
    private String dayOfWeek;
    private String meetingTime;
    private String duration;
    private String location;
    private String message;

    @Enumerated(EnumType.STRING)
    private Status status = Status.CONFIRMED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}