from django.contrib import admin
from django.urls import path
from core import views

urlpatterns = [path('admin/', admin.site.urls), path('', views.landing),
    path('login/', views.landing), path('register/', views.landing),
    path('app/', views.app), path('app/<str:page>/', views.app),
    path('api/<str:resource>/', views.api), path('api/<str:resource>/<int:pk>/', views.api)]
