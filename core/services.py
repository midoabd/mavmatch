from django.contrib.auth.models import User
from django.db import transaction
from django.db.models import Q
from .models import Availability, Block, Enrollment

def blocked(a, b):
    return Block.objects.filter(Q(actor=a, target=b) | Q(actor=b, target=a)).exists()

def person(user):
    return {'id': user.id, 'name': user.get_full_name(), 'firstName': user.first_name,
            'lastName': user.last_name, 'major': user.profile.major, 'color': user.profile.color,
            'isDemo': user.profile.is_demo}

@transaction.atomic
def recommendations(user):
    """Score a consistent snapshot of current courses, unique hours, and blocks.

    SQLite IMMEDIATE transactions keep concurrent schedule/enrollment changes
    from being combined into a recommendation that was never valid.
    """
    mine = dict(Enrollment.objects.filter(student=user).values_list('course_id', 'course__code'))
    hours = set(Availability.objects.filter(student=user).values_list('day', 'hour'))
    if not mine or not hours:
        return []
    blocked_ids = set(Block.objects.filter(actor=user).values_list('target_id', flat=True))
    blocked_ids.update(Block.objects.filter(target=user).values_list('actor_id', flat=True))
    enrollments = list(Enrollment.objects.filter(course_id__in=mine).exclude(student=user)
                       .exclude(student_id__in=blocked_ids).filter(student__is_active=True)
                       .values_list('student_id', 'course_id'))
    shared = {}
    for sid, cid in enrollments:
        shared.setdefault(sid, set()).add(cid)
    availability = {}
    for sid, day, hour in Availability.objects.filter(student_id__in=shared).values_list('student_id', 'day', 'hour'):
        availability.setdefault(sid, set()).add((day, hour))
    results = []
    # Admin-created users can have enrollments before their profile is created.
    # One incomplete account must not prevent all valid partners from loading.
    for student in User.objects.filter(id__in=shared, is_active=True, profile__isnull=False).select_related('profile'):
        overlap = sorted(hours & availability.get(student.id, set()))
        if not overlap: continue
        results.append({**person(student), 'sharedCourses': sorted(mine[c] for c in shared[student.id]),
                        'overlapHours': len(overlap), 'overlap': [{'day': d, 'hour': h} for d, h in overlap]})
    return sorted(results, key=lambda m: (-m['overlapHours'], -len(m['sharedCourses']), m['name'], m['id']))

def shared_courses(a, b):
    return Enrollment.objects.filter(student=a, course_id__in=Enrollment.objects.filter(student=b)
                                    .values('course_id')).values_list('course_id', flat=True)
