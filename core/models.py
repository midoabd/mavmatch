from django.conf import settings
from django.db import models
from django.db.models import Q, F

User = settings.AUTH_USER_MODEL

class Profile(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name='profile')
    major = models.CharField(max_length=150, blank=True)
    uta_id = models.CharField(max_length=10, unique=True, null=True, blank=True)
    color = models.CharField(max_length=7, default='#1565C0')
    is_demo = models.BooleanField(default=False)
    onboarded = models.BooleanField(default=False)

class Course(models.Model):
    code = models.CharField(max_length=20, unique=True)
    name = models.CharField(max_length=180)
    def __str__(self): return f'{self.code} — {self.name}'

class Enrollment(models.Model):
    student = models.ForeignKey(User, on_delete=models.CASCADE, related_name='enrollments')
    course = models.ForeignKey(Course, on_delete=models.CASCADE)
    class Meta:
        constraints = [models.UniqueConstraint(fields=['student', 'course'], name='unique_enrollment')]

class Availability(models.Model):
    student = models.ForeignKey(User, on_delete=models.CASCADE, related_name='availability')
    day = models.PositiveSmallIntegerField()
    hour = models.PositiveSmallIntegerField()
    class Meta:
        constraints = [models.UniqueConstraint(fields=['student', 'day', 'hour'], name='unique_available_hour'),
            models.CheckConstraint(condition=Q(day__gte=0, day__lte=6, hour__gte=0, hour__lte=23), name='valid_available_hour')]

class Block(models.Model):
    actor = models.ForeignKey(User, on_delete=models.CASCADE, related_name='blocks')
    target = models.ForeignKey(User, on_delete=models.CASCADE, related_name='blocked_by')
    class Meta:
        constraints = [models.UniqueConstraint(fields=['actor', 'target'], name='unique_block'),
                       models.CheckConstraint(condition=~Q(actor=F('target')), name='no_self_block')]

class Conversation(models.Model):
    first = models.ForeignKey(User, on_delete=models.CASCADE, related_name='conversations_first')
    second = models.ForeignKey(User, on_delete=models.CASCADE, related_name='conversations_second')
    created_at = models.DateTimeField(auto_now_add=True)
    class Meta:
        constraints = [models.UniqueConstraint(fields=['first', 'second'], name='unique_conversation'),
                       models.CheckConstraint(condition=Q(first__lt=F('second')), name='ordered_conversation')]

class Message(models.Model):
    conversation = models.ForeignKey(Conversation, on_delete=models.CASCADE, related_name='messages')
    sender = models.ForeignKey(User, on_delete=models.CASCADE)
    content = models.TextField(max_length=2000)
    created_at = models.DateTimeField(auto_now_add=True)
    read_at = models.DateTimeField(null=True, blank=True)
    class Meta:
        ordering = ['created_at', 'id']

class Report(models.Model):
    message = models.ForeignKey(Message, on_delete=models.CASCADE)
    reporter = models.ForeignKey(User, on_delete=models.CASCADE)
    reason = models.CharField(max_length=500)
    created_at = models.DateTimeField(auto_now_add=True)
    class Meta:
        constraints = [models.UniqueConstraint(fields=['message', 'reporter'], name='unique_message_report')]

class Meeting(models.Model):
    requester = models.ForeignKey(User, on_delete=models.CASCADE, related_name='requested_meetings')
    recipient = models.ForeignKey(User, on_delete=models.CASCADE, related_name='received_meetings')
    course = models.ForeignKey(Course, on_delete=models.PROTECT)
    starts_at = models.DateTimeField()
    duration = models.PositiveSmallIntegerField(default=60)
    location = models.CharField(max_length=180)
    notes = models.CharField(max_length=1000, blank=True)
    status = models.CharField(max_length=12, choices=[(s, s) for s in ['pending', 'confirmed', 'declined', 'cancelled']], default='pending')
    created_at = models.DateTimeField(auto_now_add=True)

class StudyGroup(models.Model):
    owner = models.ForeignKey(User, on_delete=models.CASCADE, related_name='owned_groups')
    course = models.ForeignKey(Course, on_delete=models.CASCADE)
    name = models.CharField(max_length=100)
    description = models.CharField(max_length=500, blank=True)
    members = models.ManyToManyField(User, related_name='study_groups')
    created_at = models.DateTimeField(auto_now_add=True)
