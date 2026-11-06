from django.contrib.auth.models import User
from django.test import TestCase
from .models import Profile, Course, Enrollment, Availability, Block

class MatchingTests(TestCase):
    def setUp(self):
        self.a=self.student('Alice')
        self.x=Course.objects.create(code='INSY 3304',name='Databases')
        self.y=Course.objects.create(code='INSY 4325',name='Enterprise Systems')
        self.z=Course.objects.create(code='FINA 3313',name='Finance')
        self.enroll(self.a,self.x,self.y)
        self.hours(self.a,(0,10),(0,11),(6,23))
        self.client.force_login(self.a)
    def student(self,name,profile=True):
        user=User.objects.create_user(username=name,first_name=name,last_name='Student')
        if profile: Profile.objects.create(user=user)
        return user
    def enroll(self,student,*courses):
        for course in courses: Enrollment.objects.create(student=student,course=course)
    def hours(self,student,*slots):
        for day,hour in slots: Availability.objects.create(student=student,day=day,hour=hour)
    def results(self):
        response=self.client.get('/api/matches/')
        self.assertEqual(response.status_code,200,response.content)
        return response.json()
    def test_shared_time_without_shared_course_is_not_a_match(self):
        b=self.student('Bob');self.enroll(b,self.z);self.hours(b,(0,10))
        self.assertEqual(self.results(),[])
    def test_adjacent_hours_or_different_days_do_not_overlap(self):
        b=self.student('Bob');self.enroll(b,self.x);self.hours(b,(0,12),(1,10),(0,0))
        self.assertEqual(self.results(),[])
    def test_multiple_shared_courses_do_not_multiply_hours_or_duplicate_partner(self):
        b=self.student('Bob');self.enroll(b,self.x,self.y);self.hours(b,(0,10),(0,11))
        rows=self.results()
        self.assertEqual(len(rows),1)
        self.assertEqual(rows[0]['overlapHours'],2)
        self.assertEqual(rows[0]['sharedCourses'],['INSY 3304','INSY 4325'])
    def test_ranking_prioritizes_hours_then_shared_courses(self):
        b=self.student('Bob');c=self.student('Cara');d=self.student('Dan')
        self.enroll(b,self.x,self.y);self.hours(b,(0,10))
        self.enroll(c,self.x);self.hours(c,(0,10),(0,11))
        self.enroll(d,self.x);self.hours(d,(0,10))
        self.assertEqual([r['id'] for r in self.results()],[c.id,b.id,d.id])
    def test_incomplete_candidate_cannot_break_everyones_match_list(self):
        b=self.student('Bob');c=self.student('Incomplete',profile=False)
        for student in [b,c]:self.enroll(student,self.x);self.hours(student,(0,10))
        self.assertEqual([r['id'] for r in self.results()],[b.id])
    def test_reverse_block_and_deactivated_candidates_are_excluded(self):
        b=self.student('Bob');c=self.student('Cara')
        for student in [b,c]:self.enroll(student,self.x);self.hours(student,(0,10))
        Block.objects.create(actor=b,target=self.a)
        c.is_active=False;c.save(update_fields=['is_active'])
        self.assertEqual(self.results(),[])
