import json
from datetime import timedelta
from pathlib import Path
from django.core.management.base import BaseCommand
from django.contrib.auth.models import User
from django.db import transaction
from django.utils import timezone
from core.models import Profile, Course, Enrollment, Availability, Conversation, Message, StudyGroup, Meeting

class Command(BaseCommand):
    help = 'Create an idempotent, explicitly labeled local demonstration dataset.'

    @transaction.atomic
    def handle(self, *args, **options):
        catalog = json.loads((Path(__file__).resolve().parents[3] / 'data/courses.json').read_text())
        for courses in catalog.values():
            for course in courses:
                Course.objects.get_or_create(code=course['code'], defaults={'name': course['name']})
        names = [('Alex','Morgan'),('Sarah','Lee'),('James','Kim'),('Maya','Patel'),('Omar','Hassan'),('Emily','Chen'),
                 ('Carlos','Rivera'),('Aisha','Johnson'),('Noah','Davis'),('Layla','Wilson'),('Ethan','Brown'),('Sofia','Garcia'),
                 ('Daniel','Park'),('Nadia','Ali'),('Grace','Taylor'),('Liam','Nguyen'),('Zara','Thomas'),('Lucas','Martin'),
                 ('Amelia','Clark'),('Rami','Saad'),('Chloe','Lewis'),('Priya','Shah'),('Jack','Walker'),('Nour','Ibrahim')]
        colors = ['#1565c0','#7256bc','#167e76','#c87523','#b04d68','#455ca4']
        majors = list(catalog)
        codes = ['INSY 3303','INSY 3304','INSY 4325','BSTAT 3321','FINA 3313','ACCT 2301']
        courses = [Course.objects.get(code=c) for c in codes]
        people=[]
        for i,(first,last) in enumerate(names):
            email = 'demo@mavs.uta.edu' if i == 0 else f'demo{i:02}@mavs.uta.edu'
            user, created = User.objects.get_or_create(username=email, defaults={'email':email,'first_name':first,'last_name':last})
            if created:
                user.set_password('MavMatchDemo!2026')
                user.save()
                Profile.objects.create(user=user, major='Information Systems (BBA)' if i<6 else majors[i%len(majors)],
                    color=colors[i%len(colors)], is_demo=True, onboarded=True)
                for c in (courses[:3] if i==0 else [courses[i%len(courses)],courses[(i+1)%len(courses)],courses[(i+2)%len(courses)]]):
                    Enrollment.objects.create(student=user,course=c)
                for day in ([0,2,4] if i%3==0 else [0,1,2]):
                    for hour in ([10,11,14,15] if i==0 else [10+i%2,11+i%2,14+i%3,15+i%3]):
                        Availability.objects.get_or_create(student=user,day=day,hour=hour)
            people.append(user)
        demo, sarah = people[:2]
        conv,created = Conversation.objects.get_or_create(first_id=min(demo.id,sarah.id),second_id=max(demo.id,sarah.id))
        if created:
            Message.objects.create(conversation=conv,sender=sarah,content='Hey Alex! We both take INSY 3304 and have Monday morning free. Want to work through the database exercises together?')
        group,created=StudyGroup.objects.get_or_create(owner=sarah,name='Database study circle',defaults={'course':courses[1], 'description':'A small crew working through SQL, ER diagrams, and exam prep. Bring your questions and a curious mind.'})
        if created:group.members.add(sarah,people[2],people[3])
        if not Meeting.objects.filter(requester=sarah,recipient=demo).exists():
            date=(timezone.localtime()+timedelta(days=2)).replace(hour=10,minute=0,second=0,microsecond=0)
            Meeting.objects.create(requester=sarah,recipient=demo,course=courses[1],starts_at=date,location='Central Library · 3rd floor',notes='Let’s review joins and normalization before class.')
        self.stdout.write(self.style.SUCCESS(f'Catalog ready. {len(people)} labeled demo students available. Log in: demo@mavs.uta.edu / MavMatchDemo!2026'))
