from django.contrib import admin
from .models import Profile, Course, Enrollment, Availability, Conversation, Message, Report, Meeting, Block, StudyGroup

admin.site.site_header = 'MavMatch moderation'
admin.site.site_title = 'MavMatch'
@admin.register(Report)
class ReportAdmin(admin.ModelAdmin):
    list_display = ['id', 'reporter', 'message', 'reason', 'created_at']
    search_fields = ['reason', 'message__content', 'reporter__email']
    readonly_fields = ['created_at']

@admin.register(Message)
class MessageAdmin(admin.ModelAdmin):
    list_display = ['id', 'sender', 'conversation', 'content', 'created_at']
    search_fields = ['content', 'sender__email']
    list_filter = ['created_at']

for model in [Profile, Course, Enrollment, Availability, Conversation, Meeting, Block, StudyGroup]:
    admin.site.register(model)
