package com.mavmatch.service;

import com.mavmatch.model.*;
import com.mavmatch.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    @Autowired private StudentRepository studentRepo;
    @Autowired private StudentCourseRepository studentCourseRepo;
    @Autowired private AvailabilityRepository availabilityRepo;
    @Autowired private MatchRepository matchRepo;
    @Autowired private BlockedUserRepository blockedUserRepo;

    public List<Map<String, Object>> findMatches(Long studentId, int page) {
        Student student = studentRepo.findById(studentId).orElse(null);
        if (student == null) return new ArrayList<>();

        List<Long> blockedIds = blockedUserRepo.findByBlockerId(studentId)
                .stream().map(b -> b.getBlocked().getId()).collect(Collectors.toList());

        List<Match> existingMatches = matchRepo.findByStudentIdOrderByOverlapHoursDesc(studentId);
        existingMatches = existingMatches.stream()
                .filter(m -> {
                    Long otherId = m.getStudent1().getId().equals(studentId)
                            ? m.getStudent2().getId() : m.getStudent1().getId();
                    return !blockedIds.contains(otherId);
                }).collect(Collectors.toList());

        int start = page * 10;
        int end = Math.min(start + 10, existingMatches.size());
        if (start < existingMatches.size()) {
            return existingMatches.subList(start, end)
                    .stream().map(m -> matchToMap(m, studentId)).collect(Collectors.toList());
        }

        return computeAndSaveMatches(student, studentId, blockedIds, page);
    }

    public List<Map<String, Object>> recomputeMatches(Long studentId, int page) {
        Student student = studentRepo.findById(studentId).orElse(null);
        if (student == null) return new ArrayList<>();

        List<Long> blockedIds = blockedUserRepo.findByBlockerId(studentId)
                .stream().map(b -> b.getBlocked().getId()).collect(Collectors.toList());

        // Get my current courses
        List<StudentCourse> myCourses = studentCourseRepo.findByStudentId(studentId);
        Set<Long> myCourseIds = myCourses.stream()
                .map(sc -> sc.getCourse().getId()).collect(Collectors.toSet());

        List<Availability> myAvailability = availabilityRepo.findByStudentId(studentId);

        // Get existing matches
        List<Match> existingMatches = matchRepo.findByStudentIdOrderByOverlapHoursDesc(studentId);
        List<Match> toDelete = new ArrayList<>();
        List<Match> toKeep = new ArrayList<>();

        for (Match match : existingMatches) {
            Long otherId = match.getStudent1().getId().equals(studentId)
                    ? match.getStudent2().getId() : match.getStudent1().getId();

            // Check if the course this match was based on is still in MY courses
            Long matchCourseId = match.getCourse() != null ? match.getCourse().getId() : null;
            boolean matchCourseStillMine = matchCourseId != null && myCourseIds.contains(matchCourseId);

            if (!matchCourseStillMine || myCourseIds.isEmpty()) {
                toDelete.add(match);
            } else {
                // Update overlap hours
                List<Availability> otherAvail = availabilityRepo.findByStudentId(otherId);
                double newOverlap = calculateOverlap(myAvailability, otherAvail);
                match.setOverlapHours(newOverlap);
                matchRepo.save(match);
                toKeep.add(match);
            }
        }

        // Delete invalid matches
        if (!toDelete.isEmpty()) matchRepo.deleteAll(toDelete);

        // Re-sort and return paginated
        toKeep.sort((a, b) -> Double.compare(b.getOverlapHours(), a.getOverlapHours()));
        int start = page * 10;
        int end = Math.min(start + 10, toKeep.size());
        if (start >= toKeep.size()) return new ArrayList<>();
        return toKeep.subList(start, end)
                .stream().map(m -> matchToMap(m, studentId)).collect(Collectors.toList());
    }

    private List<Map<String, Object>> computeAndSaveMatches(Student student, Long studentId,
                                                            List<Long> blockedIds, int page) {
        // QUERY 1: My courses
        List<StudentCourse> myCourses = studentCourseRepo.findByStudentId(studentId);
        if (myCourses.isEmpty()) return new ArrayList<>();

        // QUERY 2: My availability
        List<Availability> myAvailability = availabilityRepo.findByStudentId(studentId);

        // QUERY 3: All students in my courses (bulk)
        Map<Long, List<Course>> sharedCoursesMap = new HashMap<>();
        for (StudentCourse sc : myCourses) {
            List<StudentCourse> others = studentCourseRepo.findByCourse(sc.getCourse());
            for (StudentCourse other : others) {
                Long otherId = other.getStudent().getId();
                if (otherId.equals(studentId) || blockedIds.contains(otherId)) continue;
                sharedCoursesMap.computeIfAbsent(otherId, k -> new ArrayList<>()).add(sc.getCourse());
            }
        }
        if (sharedCoursesMap.isEmpty()) return new ArrayList<>();

        // Already matched IDs (from existing matches already loaded)
        Set<Long> alreadyMatchedIds = matchRepo.findByStudentIdOrderByOverlapHoursDesc(studentId)
                .stream().map(m -> m.getStudent1().getId().equals(studentId)
                        ? m.getStudent2().getId() : m.getStudent1().getId())
                .collect(Collectors.toSet());

        Set<Long> candidateIds = sharedCoursesMap.keySet().stream()
                .filter(id -> !alreadyMatchedIds.contains(id))
                .collect(Collectors.toSet());
        if (candidateIds.isEmpty()) return new ArrayList<>();

        // QUERY 4: Bulk load availability for ALL candidates in ONE query
        Map<Long, List<Availability>> allAvailability = new HashMap<>();
        availabilityRepo.findAll().stream().filter(a -> candidateIds.contains(a.getStudent().getId())).forEach(a ->
                allAvailability.computeIfAbsent(a.getStudent().getId(), k -> new ArrayList<>()).add(a));

        // QUERY 5: Bulk load all candidate students in ONE query
        Map<Long, Student> candidateStudents = studentRepo.findAllById(candidateIds)
                .stream().collect(Collectors.toMap(Student::getId, s -> s));

        // Calculate overlaps
        List<Object[]> toSave = new ArrayList<>();
        List<Map<String, Object>> results = new ArrayList<>();

        for (Long otherId : candidateIds) {
            List<Availability> otherAvailability = allAvailability.getOrDefault(otherId, Collections.emptyList());
            double overlap = calculateOverlap(myAvailability, otherAvailability);
            if (overlap < 1.0) continue;

            Student other = candidateStudents.get(otherId);
            if (other == null) continue;

            Course topCourse = sharedCoursesMap.get(otherId).get(0);
            Match match = new Match();
            match.setStudent1(student);
            match.setStudent2(other);
            match.setCourse(topCourse);
            match.setOverlapHours(overlap);
            toSave.add(new Object[]{match, otherId, other, topCourse, overlap});
        }

        // QUERY 6: Batch save all matches
        List<Match> matchesToSave = toSave.stream()
                .map(o -> (Match) o[0]).collect(Collectors.toList());
        List<Match> saved = matchesToSave.isEmpty() ? new ArrayList<>() : matchRepo.saveAll(matchesToSave);

        for (int i = 0; i < saved.size(); i++) {
            Object[] data = toSave.get(i);
            Student other = (Student) data[2];
            Course topCourse = (Course) data[3];
            double overlap = (double) data[4];
            Map<String, Object> m = new HashMap<>();
            m.put("matchId", saved.get(i).getId());
            m.put("studentId", other.getId());
            m.put("name", other.getFirstName() + " " + other.getLastName());
            m.put("major", other.getMajor());
            m.put("courseCode", topCourse.getCourseCode());
            m.put("overlapHours", overlap);
            results.add(m);
        }

        results.sort((a, b) -> Double.compare((Double) b.get("overlapHours"), (Double) a.get("overlapHours")));

        int start = page * 10;
        int end = Math.min(start + 10, results.size());
        if (start >= results.size()) return new ArrayList<>();
        return results.subList(start, end);
    }

    private double calculateOverlap(List<Availability> a1, List<Availability> a2) {
        if (a1.isEmpty() || a2.isEmpty()) return 0;
        Map<String, List<Availability>> a2ByDay = a2.stream()
                .collect(Collectors.groupingBy(Availability::getDayOfWeek));
        double total = 0;
        for (Availability s1 : a1) {
            List<Availability> same = a2ByDay.get(s1.getDayOfWeek());
            if (same == null) continue;
            for (Availability s2 : same) {
                int os = Math.max(s1.getStartHour(), s2.getStartHour());
                int oe = Math.min(s1.getEndHour(), s2.getEndHour());
                if (oe > os) total += (oe - os);
            }
        }
        return total;
    }

    private Map<String, Object> matchToMap(Match match, Long studentId) {
        Student other = match.getStudent1().getId().equals(studentId)
                ? match.getStudent2() : match.getStudent1();
        Map<String, Object> m = new HashMap<>();
        m.put("matchId", match.getId());
        m.put("studentId", other.getId());
        m.put("name", other.getFirstName() + " " + other.getLastName());
        m.put("major", other.getMajor());
        m.put("courseCode", match.getCourse() != null ? match.getCourse().getCourseCode() : "");
        m.put("overlapHours", match.getOverlapHours());
        return m;
    }
}