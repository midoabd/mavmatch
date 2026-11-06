"""Use the same file-backed SQLite locking behavior as the local app."""
import json
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import timedelta
from django.contrib.auth.models import User
from django.db import connection, connections, close_old_connections
from django.test import TransactionTestCase, Client, override_settings
from django.utils import timezone
from .models import Profile, Course, Enrollment, Availability, Meeting

@override_settings(PASSWORD_HASHERS=['django.contrib.auth.hashers.MD5PasswordHasher'])
class ConcurrentWritesTests(TransactionTestCase):
    def setUp(self):
        self.users=[]
        for name in ['Alice', 'Bob', 'Cara']:
            user=User.objects.create_user(username=name,first_name=name,password='Test-password-482!')
            Profile.objects.create(user=user)
            self.users.append(user)
        self.course=Course.objects.create(code='INSY 3304',name='Databases')
        for user in self.users: Enrollment.objects.create(student=user,course=self.course)

    def client_for(self,user):
        client=Client(raise_request_exception=False)
        client.force_login(user)
        return client

    def delayed_write(self,client,path,body,first_read,table,method,atomic_only=True):
        close_old_connections()
        delayed=False
        def pause_after_read(execute,sql,params,many,context):
            nonlocal delayed
            result=execute(sql,params,many,context)
            if not delayed and (not atomic_only or connection.in_atomic_block) and sql.lstrip().upper().startswith('SELECT') and table in sql:
                delayed=True
                first_read.set()
                time.sleep(0.25)
            return result
        try:
            with connection.execute_wrapper(pause_after_read):
                return getattr(client,method)(path,data=json.dumps(body),content_type='application/json').status_code
        finally: connections.close_all()

    def test_two_simultaneous_acceptances_cannot_crash_or_double_book(self):
        alice,bob,cara=self.users
        date=timezone.now()+timedelta(days=2)
        invitations=[Meeting.objects.create(requester=requester,recipient=bob,course=self.course,
            starts_at=date,duration=60,location='Library') for requester in [alice,cara]]
        first,second=self.client_for(bob),self.client_for(bob)
        ready=threading.Event()
        with ThreadPoolExecutor(max_workers=2) as pool:
            a=pool.submit(self.delayed_write,first,f'/api/meetings/{invitations[0].id}/',
                          {'status':'confirmed'},ready,'core_meeting','patch')
            self.assertTrue(ready.wait(5),'First request never reached the transaction.')
            b=pool.submit(self.delayed_write,second,f'/api/meetings/{invitations[1].id}/',
                          {'status':'confirmed'},threading.Event(),'core_meeting','patch')
            self.assertEqual(sorted([a.result(timeout=10),b.result(timeout=10)]),[200,409])
        self.assertEqual(Meeting.objects.filter(status='confirmed').count(),1)

    def test_simultaneous_schedule_replacements_save_complete_schedules(self):
        alice=self.users[0]
        first,second=self.client_for(alice),self.client_for(alice)
        sets=[[{'day':0,'hour':10},{'day':0,'hour':11}],[{'day':2,'hour':14}]]
        ready=threading.Event()
        with ThreadPoolExecutor(max_workers=2) as pool:
            a=pool.submit(self.delayed_write,first,'/api/availability/',{'slots':sets[0]},ready,'auth_user','put')
            self.assertTrue(ready.wait(5),'First request never reached the transaction.')
            b=pool.submit(self.delayed_write,second,'/api/availability/',{'slots':sets[1]},threading.Event(),'auth_user','put')
            self.assertEqual([a.result(timeout=10),b.result(timeout=10)],[200,200])
        saved=set(Availability.objects.filter(student=alice).values_list('day','hour'))
        self.assertIn(saved,[{(0,10),(0,11)},{(2,14)}])

    def test_message_cannot_arrive_after_a_completed_block(self):
        from .models import Conversation, Message
        alice,bob,_=self.users
        conv=Conversation.objects.create(first=alice,second=bob)
        sender,blocker=self.client_for(alice),self.client_for(bob)
        ready=threading.Event()
        block_confirmed=[]
        def send():
            status=self.delayed_write(sender,'/api/messages/',{'conversationId':conv.id,'content':'Already blocked?'},
                                      ready,'core_block','post',False)
            return status
        def block():
            status=self.delayed_write(blocker,'/api/blocks/',{'studentId':alice.id},
                                      threading.Event(),'never_pause','post',False)
            block_confirmed.append(timezone.now())
            return status
        with ThreadPoolExecutor(max_workers=2) as pool:
            a=pool.submit(send)
            self.assertTrue(ready.wait(5),'First request never checked blocking.')
            b=pool.submit(block)
            status_a,status_b=a.result(timeout=10),b.result(timeout=10)
        self.assertEqual(status_b,200)
        self.assertIn(status_a,[200,403])
        if status_a==200:
            self.assertLessEqual(Message.objects.get(conversation=conv).created_at,block_confirmed[0],
                'A message was admitted after the recipient had already received block confirmation.')
        else:
            self.assertFalse(Message.objects.filter(conversation=conv).exists())
