import json
from datetime import timedelta
from django.test import TestCase, Client, override_settings
from django.contrib.auth.models import User
from django.utils import timezone
from .models import Profile, Course, Enrollment, Availability, Conversation, Message, Meeting, Report

@override_settings(PASSWORD_HASHERS=['django.contrib.auth.hashers.MD5PasswordHasher'])
class AccountIsolationTests(TestCase):
    def setUp(self):
        self.a=self.user('Alice');self.b=self.user('Bob');self.c=self.user('Cara')
        self.client.force_login(self.a)
        self.course=Course.objects.create(code='INSY 3304',name='Database Management')
        self.other=Course.objects.create(code='INSY 4325',name='Advanced Databases')
    def user(self,name):
        u=User.objects.create_user(username=name.lower()+'@mavs.uta.edu',email=name.lower()+'@mavs.uta.edu',password='A-test-password-482!',first_name=name,last_name='Student')
        Profile.objects.create(user=u)
        return u
    def call(self,path,body=None,method='post',client=None):
        return getattr(client or self.client,method)('/api/'+path+'/',data=json.dumps(body or {}),content_type='application/json')
    def pair(self):
        for u in [self.a,self.b]:
            Enrollment.objects.create(student=u,course=self.course)
            for h in [10,11]:Availability.objects.create(student=u,day=0,hour=h)
    def conv(self):
        self.pair()
        return self.call('conversations',{'partnerId':self.b.id}).json()['id']
    def test_new_account_never_inherits_courses(self):
        self.call('courses',{'courseId':self.course.id,'studentId':self.b.id})
        self.assertEqual(self.client.get('/api/me/').json()['courses'][0]['id'],self.course.id)
        self.client.force_login(self.b)
        self.assertEqual(self.client.get('/api/me/').json()['courses'],[])
    def test_deleting_course_only_affects_actor(self):
        self.pair()
        self.assertEqual(self.call('courses/'+str(self.course.id),method='delete').status_code,200)
        self.assertFalse(Enrollment.objects.filter(student=self.a).exists())
        self.assertTrue(Enrollment.objects.filter(student=self.b).exists())
    def test_matches_are_current_for_both_students(self):
        self.pair()
        rows=self.client.get('/api/matches/').json()
        self.assertEqual(rows[0]['id'],self.b.id)
        self.assertEqual(rows[0]['overlapHours'],2)
        Enrollment.objects.filter(student=self.b).delete()
        self.assertEqual(self.client.get('/api/matches/').json(),[])
    def test_availability_removal_hides_match(self):
        self.pair();Availability.objects.filter(student=self.b).delete()
        self.assertEqual(self.client.get('/api/matches/').json(),[])
    def test_duplicate_hours_count_once(self):
        self.pair()
        self.call('availability',{'slots':[{'day':0,'hour':10}]*3},'put')
        self.assertEqual(self.client.get('/api/matches/').json()[0]['overlapHours'],1)
    def test_invalid_availability_preserves_previous_schedule(self):
        self.pair()
        r=self.call('availability',{'slots':[{'day':9,'hour':10}]},'put')
        self.assertEqual(r.status_code,400)
        self.assertEqual(self.a.availability.count(),2)
    def test_changed_courses_keep_chats_intact(self):
        cid=self.conv();self.call('messages',{'conversationId':cid,'content':'Our study plan'})
        self.call('courses/'+str(self.course.id),method='delete')
        self.assertEqual(self.client.get('/api/matches/').json(),[])
        self.assertEqual(self.client.get(f'/api/messages/?conversation={cid}').json()[0]['content'],'Our study plan')
    def test_outsider_cannot_read_send_or_report_conversation(self):
        cid=self.conv();mid=self.call('messages',{'conversationId':cid,'content':'Private'}).json()['id']
        self.client.force_login(self.c)
        self.assertEqual(self.client.get(f'/api/messages/?conversation={cid}').status_code,403)
        self.assertEqual(self.call('messages',{'conversationId':cid,'content':'Intrusion'}).status_code,403)
        self.assertEqual(self.call('reports',{'messageId':mid,'reason':'Forged'}).status_code,403)
        self.assertEqual(Message.objects.count(),1)
    def test_sender_identity_cannot_be_spoofed(self):
        cid=self.conv();self.call('messages',{'conversationId':cid,'content':'Hello','senderId':self.c.id})
        self.assertEqual(Message.objects.get().sender,self.a)
    def test_blocking_is_bidirectional_and_preserves_history(self):
        cid=self.conv();self.call('messages',{'conversationId':cid,'content':'History'})
        self.call('blocks',{'studentId':self.b.id})
        self.client.force_login(self.b)
        self.assertEqual(self.client.get('/api/matches/').json(),[])
        self.assertEqual(self.call('messages',{'conversationId':cid,'content':'Blocked'}).status_code,403)
        self.assertEqual(self.client.get(f'/api/messages/?conversation={cid}').status_code,200)
    def test_profile_changes_persist_only_for_actor(self):
        self.call('profile',{'firstName':'Updated','lastName':'Name','major':'Finance','studentId':self.b.id},'patch')
        self.a.refresh_from_db();self.b.refresh_from_db()
        self.assertEqual(self.a.first_name,'Updated');self.assertEqual(self.b.first_name,'Bob')
    def test_message_reads_clear_only_recipient_unread(self):
        cid=self.conv();self.call('messages',{'conversationId':cid,'content':'Hello'})
        self.client.force_login(self.b)
        self.assertEqual(self.client.get('/api/me/').json()['unread'],1)
        self.client.get(f'/api/messages/?conversation={cid}')
        self.assertEqual(self.client.get('/api/me/').json()['unread'],0)
    def test_conversation_identity_is_stable(self):
        cid=self.conv()
        self.client.force_login(self.b)
        self.assertEqual(self.call('conversations',{'partnerId':self.a.id}).json()['id'],cid)
    def test_two_blank_optional_ids_register(self):
        for n in [1,2]:
            r=self.call('register',{'email':f'new{n}@mavs.uta.edu','password':'A-strong-password-673!',
                'firstName':'New','lastName':'Student','utaId':'','major':'Test'},client=Client())
            self.assertEqual(r.status_code,200,r.content)
        self.assertEqual(Profile.objects.filter(uta_id=None).count(),5)
    def test_bad_login_clears_old_session(self):
        self.assertEqual(self.call('login',{'email':self.a.email,'password':'wrong'}).status_code,401)
        self.assertEqual(self.client.get('/api/me/').status_code,401)
    def test_anonymous_and_nonstaff_cannot_access_private_apis(self):
        anon=Client()
        for path in ['me','matches','conversations','meetings','groups','blocks']:
            self.assertEqual(anon.get('/api/'+path+'/').status_code,401)
        self.assertEqual(self.client.get('/admin/').status_code,302)
    def test_csrf_is_required_for_login_and_writes(self):
        c=Client(enforce_csrf_checks=True)
        r=self.call('login',{'email':self.a.email,'password':'A-test-password-482!'},client=c)
        self.assertEqual(r.status_code,403)
        c.get('/login/')
        r=c.post('/api/login/',data=json.dumps({'email':self.a.email,'password':'A-test-password-482!'}),content_type='application/json',HTTP_X_CSRFTOKEN=c.cookies['csrftoken'].value)
        self.assertEqual(r.status_code,200)
    def test_only_recipient_can_accept_meeting_and_outsiders_cannot_cancel(self):
        self.pair()
        data={'recipientId':self.b.id,'courseId':self.course.id,'startsAt':(timezone.now()+timedelta(days=2)).isoformat(),
              'duration':60,'location':'Library'}
        mid=self.call('meetings',data).json()['id']
        self.assertEqual(self.call('meetings/'+str(mid),{'status':'confirmed'},'patch').status_code,403)
        self.client.force_login(self.c)
        self.assertEqual(self.call('meetings/'+str(mid),{'status':'cancelled'},'patch').status_code,403)
        self.assertEqual(self.client.get('/api/meetings/').json(),[])
        self.client.force_login(self.b)
        self.assertEqual(self.call('meetings/'+str(mid),{'status':'confirmed'},'patch').status_code,200)
        self.assertEqual(Meeting.objects.get(pk=mid).status,'confirmed')
    def test_meeting_requires_shared_course(self):
        r=self.call('meetings',{'recipientId':self.b.id,'courseId':self.course.id,'startsAt':(timezone.now()+timedelta(days=2)).isoformat(),'duration':60,'location':'Library'})
        self.assertEqual(r.status_code,400)
    def test_groups_only_visible_and_joinable_for_enrolled_students(self):
        self.pair()
        gid=self.call('groups',{'courseId':self.course.id,'name':'Study circle'}).json()['id']
        self.client.force_login(self.c)
        self.assertEqual(self.client.get('/api/groups/').json(),[])
        self.assertEqual(self.call('groups/'+str(gid),{}).status_code,400)
        self.client.force_login(self.b)
        self.assertEqual(self.call('groups/'+str(gid),{}).status_code,200)
    def test_report_records_actual_received_message(self):
        cid=self.conv();mid=self.call('messages',{'conversationId':cid,'content':'Report this'}).json()['id']
        self.client.force_login(self.b)
        self.assertEqual(self.call('reports',{'messageId':mid,'reason':'Inappropriate'}).status_code,200)
        self.assertEqual(Report.objects.get().reporter,self.b)
    def test_stale_tab_cannot_mutate_after_account_switch(self):
        self.client.force_login(self.b)
        r=self.client.post('/api/courses/',data=json.dumps({'courseId':self.course.id}),content_type='application/json',HTTP_X_MAVMATCH_USER=str(self.a.id))
        self.assertEqual(r.status_code,409)
        self.assertFalse(Enrollment.objects.exists())
    def test_remaining_shared_course_keeps_pair_valid(self):
        self.pair()
        for u in [self.a,self.b]:Enrollment.objects.create(student=u,course=self.other)
        self.call('courses/'+str(self.course.id),method='delete')
        self.assertEqual(self.client.get('/api/matches/').json()[0]['sharedCourses'],[self.other.code])
    def test_new_candidates_are_discovered_after_existing_matches(self):
        self.pair()
        self.assertEqual(len(self.client.get('/api/matches/').json()),1)
        Enrollment.objects.create(student=self.c,course=self.course)
        Availability.objects.create(student=self.c,day=0,hour=10)
        self.assertEqual(len(self.client.get('/api/matches/').json()),2)
    def test_overlapping_confirmed_meetings_are_rejected(self):
        self.pair();Enrollment.objects.create(student=self.c,course=self.course)
        date=timezone.now()+timedelta(days=2)
        Meeting.objects.create(requester=self.c,recipient=self.b,course=self.course,starts_at=date,duration=60,location='Library',status='confirmed')
        mid=self.call('meetings',{'recipientId':self.b.id,'courseId':self.course.id,'startsAt':date.isoformat(),'duration':60,'location':'Library'}).json()['id']
        self.client.force_login(self.b)
        self.assertEqual(self.call('meetings/'+str(mid),{'status':'confirmed'},'patch').status_code,409)
        self.assertEqual(Meeting.objects.get(pk=mid).status,'pending')
    def test_invalid_profile_rolls_back_user_changes(self):
        r=self.call('profile',{'firstName':'Should not persist','lastName':'Changed','major':'New','color':'invalid'},'patch')
        self.assertEqual(r.status_code,400)
        self.a.refresh_from_db();self.assertEqual(self.a.first_name,'Alice')
    def test_registration_cannot_grant_admin_or_demo_privileges(self):
        r=self.call('register',{'email':'attacker@mavs.uta.edu','password':'Uncommon-password-482!',
            'firstName':'New','lastName':'Student','is_staff':True,'is_superuser':True,'isDemo':True})
        self.assertEqual(r.status_code,200)
        created=User.objects.get(username='attacker@mavs.uta.edu')
        self.assertFalse(created.is_staff)
        self.assertFalse(created.is_superuser)
        self.assertFalse(created.profile.is_demo)
    def test_schedule_payload_cannot_overwrite_another_student(self):
        self.pair()
        r=self.call('availability',{'studentId':self.b.id,'slots':[]},'put')
        self.assertEqual(r.status_code,200)
        self.assertEqual(self.a.availability.count(),0)
        self.assertEqual(self.b.availability.count(),2)
    def test_outsider_cannot_delete_group_or_remove_other_members(self):
        self.pair()
        gid=self.call('groups',{'courseId':self.course.id,'name':'Private course group'}).json()['id']
        self.client.force_login(self.c)
        self.call('groups/'+str(gid),{'studentId':self.a.id},'delete')
        from .models import StudyGroup
        group=StudyGroup.objects.get(pk=gid)
        self.assertEqual(list(group.members.all()),[self.a])
    def test_block_prevents_reconnection_and_new_invitations(self):
        self.conv()
        self.call('blocks',{'studentId':self.b.id})
        self.client.force_login(self.b)
        self.assertEqual(self.call('conversations',{'partnerId':self.a.id}).status_code,403)
        self.assertEqual(self.call('meetings',{'recipientId':self.a.id,'courseId':self.course.id,
            'startsAt':(timezone.now()+timedelta(days=2)).isoformat(),'duration':60,'location':'Library'}).status_code,403)
    def test_deactivation_revokes_existing_session(self):
        self.a.is_active=False
        self.a.save(update_fields=['is_active'])
        self.assertEqual(self.client.get('/api/me/').status_code,401)
        self.assertEqual(self.call('profile',{'firstName':'Bypass','lastName':'Ban'},'patch').status_code,401)
    def test_dropping_course_keeps_messages_reports_and_meeting_history(self):
        cid=self.conv()
        mid=self.call('messages',{'conversationId':cid,'content':'Keep this evidence'}).json()['id']
        other=Client();other.force_login(self.b)
        self.call('reports',{'messageId':mid,'reason':'Evidence'},client=other)
        meeting=self.call('meetings',{'recipientId':self.b.id,'courseId':self.course.id,
            'startsAt':(timezone.now()+timedelta(days=2)).isoformat(),'duration':60,'location':'Library'}).json()['id']
        self.call('courses/'+str(self.course.id),method='delete')
        self.assertTrue(Message.objects.filter(pk=mid).exists())
        self.assertTrue(Report.objects.filter(message_id=mid).exists())
        self.assertTrue(Meeting.objects.filter(pk=meeting).exists())
        self.assertEqual(self.client.get('/api/matches/').json(),[])
