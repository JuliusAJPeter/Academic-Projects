from django.contrib.auth.models import Group
from django import template

register = template.Library()

@register.filter
def isDeveloper(user):
    return user.groups.filter(name="Developer").exists()

@register.filter
def multiply(value, arg):
    return value*arg