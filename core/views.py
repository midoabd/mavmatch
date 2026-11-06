import json
import re
from datetime import timedelta
from functools import wraps
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from django.contrib.auth.models import User
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError, ObjectDoesNotExist
from django.db import IntegrityError, OperationalError, transaction
from django.db.models import Q, Count
from django.http import JsonResponse
from django.shortcuts import render, redirect
from django.utils import timezone
from django.utils.dateparse import parse_datetime
from django.views.decorators.csrf import ensure_csrf_cookie
from django.views.decorators.cache import never_cache
from .models import Profile, Course, Enrollment, Availability, Block, Conversation, Message, Report, Meeting, StudyGroup
from .services import person, recommendations, shared_courses, blocked

class Problem(Exception):
    def __init__(self, message, status=400): self.message, self.status = message, status

def require(condition, message, status=400):
    if not condition: raise Problem(message, status)

def text(data, key, maximum=180, required=True):
    value = data.get(key, '')
    require(isinstance(value, str), f'{key} must be text.')
    value = value.strip()
    require((not required or bool(value)) and len(value) <= maximum, f'Please enter a valid {key} (up to {maximum} characters).')
    return value

def integer(data, key):
    value = data.get(key)
    require(isinstance(value, int) and not isinstance(value, bool), f'{key} must be an integer.')
    return value

def self_profile(user):
    Profile.objects.get_or_create(user=user)
    return {**person(user), 'email': user.email, 'utaId': user.profile.uta_id or '',
            'onboarded': user.profile.onboarded, 'isStaff': user.is_staff}

def conversation(user, cid):
    item = Conversation.objects.select_related('first__profile', 'second__profile').get(pk=cid)
    require(user.id in (item.first_id, item.second_id), 'This conversation is private.', 403)
    return item

@ensure_csrf_cookie
def landing(request):
    if request.user.is_authenticated: return redirect('/app/')
    return render(request, 'landing.html', {'register': request.path == '/register/'})

@login_required
@ensure_csrf_cookie
@never_cache
def app(request, page='dashboard'):
    pages = ['dashboard', 'courses', 'availability', 'matches', 'messages', 'meetings', 'groups', 'profile', 'onboarding']
    if page not in pages: return redirect('/app/')
    return render(request, 'app.html', {'page': page, 'profile': self_profile(request.user)})

@never_cache
def api(request, resource, pk=None):
    try:
        require(request.method in ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'], 'Method not allowed.', 405)
        data = {}
        if request.body:
            try: data = json.loads(request.body)
            except (ValueError, UnicodeDecodeError): raise Problem('Please send valid JSON.')
            require(isinstance(data, dict), 'Please send a JSON object.')
        if resource in ['login', 'register', 'logout']:
            require(request.method == 'POST', 'Method not allowed.', 405)
            return JsonResponse(auth(request, resource, data))
        require(request.user.is_authenticated, 'Please log in to continue.', 401)
        require(request.user.is_active, 'This account is unavailable.', 403)
        actor_hint = request.headers.get('X-MavMatch-User')
        require(actor_hint is None or actor_hint == str(request.user.id),
                'Your account changed in another tab. Refresh this page before continuing.', 409)
        if request.method == 'GET':
            result = dispatch(request, resource, pk, data)
        else:
            # Keep permission checks and their writes in the same transaction.
            # With SQLite's IMMEDIATE mode, a block/enrollment change cannot
            # commit between authorization and message/invitation creation.
            with transaction.atomic():
                result = dispatch(request, resource, pk, data)
        return JsonResponse(result, safe=False)
    except Problem as e: return JsonResponse({'error': e.message}, status=e.status)
    except ObjectDoesNotExist: return JsonResponse({'error': 'That item was not found.'}, status=404)
    except ValidationError as e: return JsonResponse({'error': ' '.join(e.messages)}, status=400)
    except IntegrityError: return JsonResponse({'error': 'That item already exists or conflicts with another update. Please refresh.'}, status=409)
    except OperationalError as e:
        code = getattr(e.__cause__, 'sqlite_errorcode', 0)
        if code & 0xff in (5, 6):  # SQLITE_BUSY / SQLITE_LOCKED, including extended codes.
            return JsonResponse({'error': 'Another update is in progress. Please try again.'}, status=409)
        raise

def auth(request, action, data):
    if action == 'logout':
        logout(request)
        return {'ok': True}
    email = text(data, 'email', 150).lower()
    password = data.get('password', '')
    require(isinstance(password, str) and 0 < len(password) <= 128, 'Please enter your password (up to 128 characters).')
    if action == 'login':
        user = authenticate(request, username=email, password=password)
        if user is None:
            logout(request)
            raise Problem('Email or password is incorrect.', 401)
    else:
        require(email.endswith('@mavs.uta.edu') and re.fullmatch(r'[^\s@]+@mavs\.uta\.edu', email), 'Please use your @mavs.uta.edu email.')
        first, last = text(data, 'firstName', 60), text(data, 'lastName', 60)
        uta = text(data, 'utaId', 10, False) or None
        require(uta is None or (len(uta) == 10 and uta.isdigit()), 'UTA ID must contain 10 digits, or be left blank.')
        require(not User.objects.filter(username=email).exists(), 'This email is already registered.', 409)
        require(not uta or not Profile.objects.filter(uta_id=uta).exists(), 'This UTA ID is already registered.', 409)
        user = User(username=email, email=email, first_name=first, last_name=last)
        validate_password(password, user)
        with transaction.atomic():
            user.set_password(password)
            user.save()
            Profile.objects.create(user=user, uta_id=uta, major=text(data, 'major', 150, False))
    login(request, user)
    return {'user': self_profile(user), 'next': '/app/' if user.profile.onboarded else '/app/onboarding/'}

def dispatch(request, resource, pk, data):
    user, method = request.user, request.method
    if resource == 'me' and method == 'GET':
        courses = list(Enrollment.objects.filter(student=user).values('course_id', 'course__code', 'course__name'))
        slots = list(user.availability.order_by('day', 'hour').values('day', 'hour'))
        return {'user': self_profile(user), 'courses': [{'id': c['course_id'], 'code': c['course__code'], 'name': c['course__name']} for c in courses],
            'availability': slots, 'unread': Message.objects.filter(Q(conversation__first=user) | Q(conversation__second=user), read_at=None).exclude(sender=user).count(),
            'pendingMeetings': Meeting.objects.filter(recipient=user, status='pending').count()}
    if resource == 'catalog' and method == 'GET':
        q = request.GET.get('q', '')[:100]
        return list(Course.objects.filter(Q(code__icontains=q) | Q(name__icontains=q)).order_by('code').values('id', 'code', 'name')[:200])
    if resource == 'courses':
        if method == 'POST':
            course = Course.objects.get(pk=integer(data, 'courseId'))
            Enrollment.objects.get_or_create(student=user, course=course)
            return {'ok': True}
        if method == 'DELETE' and pk:
            Enrollment.objects.filter(student=user, course_id=pk).delete()
            return {'ok': True}
    if resource == 'availability' and method == 'PUT':
        slots = data.get('slots')
        require(isinstance(slots, list) and len(slots) <= 168, 'Choose up to 168 weekly hours.')
        unique = set()
        for s in slots:
            require(isinstance(s, dict), 'Invalid availability slot.')
            day, hour = integer(s, 'day'), integer(s, 'hour')
            require(0 <= day <= 6 and 0 <= hour <= 23, 'Availability must use valid days and hours.')
            unique.add((day, hour))
        with transaction.atomic():
            # Serialize replacements from concurrent tabs for this same student.
            User.objects.select_for_update().get(pk=user.pk)
            user.availability.all().delete()
            Availability.objects.bulk_create([Availability(student=user, day=d, hour=h) for d, h in sorted(unique)])
        return {'ok': True, 'count': len(unique)}
    if resource == 'profile' and method == 'PATCH':
        with transaction.atomic():
            user.first_name = text(data, 'firstName', 60)
            user.last_name = text(data, 'lastName', 60)
            user.save(update_fields=['first_name', 'last_name'])
            profile = user.profile
            profile.major = text(data, 'major', 150, False)
            color = data.get('color', profile.color)
            require(isinstance(color, str) and re.fullmatch(r'#[0-9a-fA-F]{6}', color), 'Invalid avatar color.')
            profile.color = color
            profile.save()
        return {'user': self_profile(user)}
    if resource == 'onboarding' and method == 'POST':
        require(user.enrollments.exists() and user.availability.exists(), 'Add a course and at least one available hour first.')
        user.profile.onboarded = True
        user.profile.save(update_fields=['onboarded'])
        return {'ok': True}
    if resource == 'matches' and method == 'GET': return recommendations(user)
    if resource == 'conversations':
        if method == 'POST':
            partner = User.objects.get(pk=integer(data, 'partnerId'), is_active=True)
            require(partner != user and not blocked(user, partner), 'You cannot connect with this student.', 403)
            existing = Conversation.objects.filter(first_id=min(user.id, partner.id), second_id=max(user.id, partner.id)).first()
            if existing: return {'id': existing.id}
            require(any(m['id'] == partner.id for m in recommendations(user)), 'Your courses or availability no longer overlap. Refresh your matches.', 409)
            conv, _ = Conversation.objects.get_or_create(first_id=min(user.id, partner.id), second_id=max(user.id, partner.id))
            return {'id': conv.id}
        if method == 'GET':
            result = []
            for conv in Conversation.objects.filter(Q(first=user) | Q(second=user)).select_related('first__profile', 'second__profile').order_by('-id'):
                partner = conv.second if conv.first_id == user.id else conv.first
                last = conv.messages.last()
                result.append({'id': conv.id, 'partner': person(partner), 'blocked': blocked(user, partner),
                    'sharedCourses': list(Course.objects.filter(id__in=shared_courses(user, partner)).values('id', 'code', 'name')),
                    'unread': conv.messages.filter(read_at=None).exclude(sender=user).count(),
                    'lastMessage': last.content if last else 'Say hello and plan a study session.'})
            return result
    if resource == 'messages':
        if method == 'GET':
            try: cid = int(request.GET.get('conversation', ''))
            except ValueError: raise Problem('Choose a conversation.')
            conv = conversation(user, cid)
            conv.messages.filter(read_at=None).exclude(sender=user).update(read_at=timezone.now())
            rows = list(conv.messages.select_related('sender').order_by('-id')[:200])
            return [{'id': m.id, 'content': m.content, 'mine': m.sender_id == user.id,
                     'sender': m.sender.get_full_name(), 'createdAt': m.created_at.isoformat()} for m in reversed(rows)]
        if method == 'POST':
            conv = conversation(user, integer(data, 'conversationId'))
            partner = conv.second if conv.first_id == user.id else conv.first
            require(partner.is_active and not blocked(user, partner), 'Messaging is unavailable for this conversation.', 403)
            message = Message.objects.create(conversation=conv, sender=user, content=text(data, 'content', 2000))
            return {'id': message.id}
    if resource == 'reports' and method == 'POST':
        message = Message.objects.select_related('conversation').get(pk=integer(data, 'messageId'))
        conversation(user, message.conversation_id)
        require(message.sender_id != user.id, 'You can report messages you received.')
        Report.objects.update_or_create(message=message, reporter=user, defaults={'reason': text(data, 'reason', 500)})
        return {'ok': True}
    if resource == 'blocks':
        if method == 'GET':
            return [person(b.target) for b in Block.objects.filter(actor=user).select_related('target__profile')]
        if method == 'POST':
            target = User.objects.get(pk=integer(data, 'studentId'))
            require(target != user, 'You cannot block yourself.')
            Block.objects.get_or_create(actor=user, target=target)
            return {'ok': True}
        if method == 'DELETE' and pk:
            Block.objects.filter(actor=user, target_id=pk).delete()
            return {'ok': True}
    if resource == 'meetings': return meetings(request, pk, data)
    if resource == 'groups': return groups(request, pk, data)
    raise Problem('Endpoint or method not found.', 404)

def meetings(request, pk, data):
    user, method = request.user, request.method
    if method == 'GET':
        rows = Meeting.objects.filter(Q(requester=user) | Q(recipient=user)).select_related('requester__profile', 'recipient__profile', 'course').order_by('starts_at')
        return [{'id': m.id, 'partner': person(m.recipient if m.requester_id == user.id else m.requester),
            'course': {'id': m.course_id, 'code': m.course.code}, 'startsAt': m.starts_at.isoformat(),
            'duration': m.duration, 'location': m.location, 'notes': m.notes, 'status': m.status,
            'isRequester': m.requester_id == user.id} for m in rows]
    if method == 'POST':
        recipient = User.objects.get(pk=integer(data, 'recipientId'), is_active=True)
        cid = integer(data, 'courseId')
        require(recipient != user and not blocked(user, recipient), 'You cannot invite this student.', 403)
        require(cid in shared_courses(user, recipient), 'Choose a course you both currently take.')
        date = parse_datetime(text(data, 'startsAt', 40))
        require(date is not None and timezone.is_aware(date) and date > timezone.now(), 'Choose a future date and time with a timezone.')
        duration = integer(data, 'duration')
        require(duration in [30, 60, 90, 120], 'Choose a duration between 30 minutes and 2 hours.')
        m = Meeting.objects.create(requester=user, recipient=recipient, course_id=cid, starts_at=date, duration=duration,
            location=text(data, 'location', 180), notes=text(data, 'notes', 1000, False))
        return {'id': m.id}
    if method == 'PATCH' and pk:
        with transaction.atomic():
            m = Meeting.objects.select_for_update().get(pk=pk)
            require(user.id in (m.requester_id, m.recipient_id), 'This meeting is private.', 403)
            status = data.get('status')
            if status == 'cancelled': require(m.status in ['pending', 'confirmed'], 'This meeting has already ended.')
            else:
                require(user.id == m.recipient_id, 'Only the invited student can respond.', 403)
                require(m.status == 'pending' and status in ['confirmed', 'declined'], 'This request has already been answered.')
                if status == 'confirmed':
                    list(User.objects.select_for_update().filter(pk__in=[m.requester_id, m.recipient_id]).order_by('pk'))
                    require(m.starts_at > timezone.now(), 'This meeting is in the past.')
                    require(not blocked(m.requester, m.recipient), 'This invitation is unavailable.', 403)
                    require(m.course_id in shared_courses(m.requester, m.recipient), 'You no longer share the course for this invitation.', 409)
                    end = m.starts_at + timedelta(minutes=m.duration)
                    conflict = Meeting.objects.filter(status='confirmed', starts_at__lt=end).filter(
                        Q(requester_id__in=[m.requester_id, m.recipient_id]) | Q(recipient_id__in=[m.requester_id, m.recipient_id])).exclude(pk=m.pk)
                    require(not any(other.starts_at + timedelta(minutes=other.duration) > m.starts_at for other in conflict),
                            'One of you already has a confirmed meeting at this time.', 409)
            m.status = status
            m.save(update_fields=['status'])
        return {'ok': True}
    raise Problem('Method not allowed.', 405)

def groups(request, pk, data):
    user, method = request.user, request.method
    if method == 'GET':
        rows = StudyGroup.objects.filter(course_id__in=user.enrollments.values('course_id')).select_related('course', 'owner').prefetch_related('members').order_by('-id')
        return [{'id': g.id, 'name': g.name, 'description': g.description, 'course': g.course.code,
                 'owner': g.owner.get_full_name(), 'isOwner': g.owner_id == user.id,
                 'joined': user in g.members.all(), 'members': g.members.count()} for g in rows]
    if method == 'POST' and pk is None:
        cid = integer(data, 'courseId')
        require(user.enrollments.filter(course_id=cid).exists(), 'Choose one of your courses.')
        with transaction.atomic():
            g = StudyGroup.objects.create(owner=user, course_id=cid, name=text(data, 'name', 100), description=text(data, 'description', 500, False))
            g.members.add(user)
        return {'id': g.id}
    if pk:
        group = StudyGroup.objects.get(pk=pk)
        if method == 'POST':
            require(user.enrollments.filter(course=group.course).exists(), 'You must take this course to join.')
            group.members.add(user)
            return {'ok': True}
        if method == 'DELETE':
            if group.owner_id == user.id: group.delete()
            else: group.members.remove(user)
            return {'ok': True}
    raise Problem('Method not allowed.', 405)
