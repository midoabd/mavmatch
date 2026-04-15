package com.mavmatch.config;

import com.mavmatch.model.*;
import com.mavmatch.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(
            StudentRepository studentRepo,
            CourseRepository courseRepo,
            StudentCourseRepository studentCourseRepo,
            AvailabilityRepository availabilityRepo,
            PasswordEncoder passwordEncoder) {

        return args -> {
            long currentCount = studentRepo.count();
            System.out.println("🔍 Current student count: " + currentCount);
            if (currentCount >= 100) {
                System.out.println("✅ Already have 100+ students, skipping.");
                return;
            }

            // Create or fetch courses
            List<Course> courses = new ArrayList<>();
            String[][] courseData = {
                    {"INSY 3304", "Principles of Information Systems", "Information Systems"},
                    {"INSY 3303", "Business Programming", "Information Systems"},
                    {"INSY 4325", "Database Management", "Information Systems"},
                    {"INSY 4330", "Systems Analysis and Design", "Information Systems"},
                    {"INSY 4315", "Project Management", "Information Systems"},
                    {"INSY 4350", "Enterprise Resource Planning", "Information Systems"},
                    {"INSY 4354", "Business Intelligence", "Information Systems"},
                    {"INSY 4370", "IT Security", "Information Systems"},
                    {"BANA 3312", "Intro to Business Analytics", "Business Analytics"},
                    {"BANA 4322", "Predictive Analytics", "Business Analytics"},
                    {"BANA 4325", "Data Visualization", "Business Analytics"},
                    {"BSTAT 3321", "Business Statistics", "Business Analytics"},
                    {"ACCT 2301", "Principles of Accounting I", "Accounting"},
                    {"ACCT 2302", "Principles of Accounting II", "Accounting"},
                    {"ACCT 3310", "Intermediate Accounting I", "Accounting"},
                    {"FINA 3313", "Financial Management", "Finance"},
                    {"FINA 4315", "Investments", "Finance"},
                    {"MANA 3318", "Organizational Behavior", "Management"},
                    {"MANA 4322", "Strategic Management", "Management"},
                    {"MARK 3321", "Principles of Marketing", "Marketing"},
                    {"MARK 4351", "Digital Marketing", "Marketing"},
                    {"BLAW 3310", "Business Law", "Business Law"},
                    {"OPMA 3306", "Operations Management", "Operations"},
                    {"ECON 2301", "Principles of Macroeconomics", "Economics"},
                    {"ECON 2302", "Principles of Microeconomics", "Economics"}
            };

            for (String[] cd : courseData) {
                Course c = courseRepo.findByCourseCode(cd[0]).orElseGet(() -> {
                    Course newCourse = new Course();
                    newCourse.setCourseCode(cd[0]);
                    newCourse.setCourseName(cd[1]);
                    newCourse.setDepartment(cd[2]);
                    return courseRepo.save(newCourse);
                });
                courses.add(c);
            }

            String[] firstNames = {
                    "Ahmad","Sara","Kevin","Mia","David","Layla","Omar","Nour","Rami","Fatima",
                    "James","Emily","Carlos","Sofia","Marcus","Aisha","Tyler","Priya","Alex","Zara",
                    "Mohammed","Jessica","Andre","Hana","Chris","Yasmin","Brandon","Lena","Jason","Dina",
                    "Ryan","Maya","Nathan","Rina","Jordan","Nadia","Derek","Amira","Sean","Leila",
                    "Michael","Chloe","Daniel","Sana","Matthew","Hiba","Andrew","Tara","Joseph","Aya",
                    "William","Grace","Benjamin","Luna","Samuel","Ella","Ethan","Zoe","Noah","Lily",
                    "Liam","Olivia","Mason","Emma","Logan","Ava","Lucas","Isabella","Elijah","Sophia",
                    "Aiden","Charlotte","Jayden","Amelia","Caleb","Harper","Owen","Evelyn","Sebastian","Abigail",
                    "Jack","Aria","Carter","Scarlett","Julian","Victoria","Grayson","Madison","Wyatt","Luna",
                    "Henry","Penelope","Dylan","Layla","Luke","Riley","Gabriel","Zoey","Anthony","Nora"
            };

            String[] lastNames = {
                    "Abdallah","Al-Amin","Patel","Liu","Torres","Hassan","Khalil","Mansour","Saad","Ibrahim",
                    "Johnson","Williams","Garcia","Martinez","Brown","Davis","Miller","Wilson","Moore","Taylor",
                    "Anderson","Thomas","Jackson","White","Harris","Martin","Thompson","Young","Lee","Walker",
                    "Hall","Allen","Wright","Scott","Green","Baker","Adams","Nelson","Carter","Mitchell",
                    "Perez","Roberts","Turner","Phillips","Campbell","Parker","Evans","Edwards","Collins","Stewart",
                    "Sanchez","Morris","Rogers","Reed","Cook","Morgan","Bell","Murphy","Bailey","Rivera",
                    "Cooper","Richardson","Cox","Howard","Ward","Torres","Peterson","Gray","Ramirez","James",
                    "Watson","Brooks","Kelly","Sanders","Price","Bennett","Wood","Barnes","Ross","Henderson",
                    "Coleman","Jenkins","Perry","Powell","Long","Patterson","Hughes","Flores","Washington","Butler",
                    "Simmons","Foster","Gonzales","Bryant","Alexander","Russell","Griffin","Diaz","Hayes","Myers"
            };

            String[] majors = {
                    "Information Systems (BBA)", "Information Systems (BS)", "Business Analytics (BS)",
                    "Accounting (BBA)", "Finance (BBA)", "Management (BBA)", "Marketing (BBA)",
                    "Economics (BBA)", "Human Resource Management (BBA)"
            };

            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
            Random rand = new Random(42);

            int seeded = 0;
            for (int i = 0; i < 100; i++) {
                String email = "stu" + String.format("%04d", i + 1) + "@mavs.uta.edu";
                String utaId = "100" + String.format("%07d", i + 1);
                String major = majors[rand.nextInt(majors.length)];

                List<Course> shuffled = new ArrayList<>(courses);
                Collections.shuffle(shuffled, rand);
                int numCourses = 3 + rand.nextInt(3);
                int numSlots = 3 + rand.nextInt(4);

                if (studentRepo.existsByEmail(email)) {
                    for (int j = 0; j < numSlots; j++) {
                        rand.nextInt(days.length);
                        rand.nextInt(10);
                        rand.nextInt(3);
                    }
                    continue;
                }

                Student student = new Student();
                student.setFirstName(firstNames[i]);
                student.setLastName(lastNames[i]);
                student.setEmail(email);
                student.setUtaId(utaId);
                student.setMajor(major);
                student.setPasswordHash(passwordEncoder.encode("password123"));
                student.setActive(true);
                student = studentRepo.save(student);

                for (int j = 0; j < numCourses; j++) {
                    StudentCourse sc = new StudentCourse();
                    sc.setStudent(student);
                    sc.setCourse(shuffled.get(j));
                    sc.setScheduleDays("TBD");
                    sc.setScheduleTime("");
                    studentCourseRepo.save(sc);
                }

                for (int j = 0; j < numSlots; j++) {
                    Availability avail = new Availability();
                    avail.setStudent(student);
                    avail.setDayOfWeek(days[rand.nextInt(days.length)]);
                    int startHour = 8 + rand.nextInt(10);
                    avail.setStartHour(startHour);
                    avail.setEndHour(startHour + 1 + rand.nextInt(3));
                    availabilityRepo.save(avail);
                }
                seeded++;
            }

            System.out.println("✅ Done! Added " + seeded + " new students. Total: " + studentRepo.count());
        };
    }
}