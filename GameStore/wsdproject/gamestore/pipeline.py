from django.db import transaction
from django.core.urlresolvers import reverse
from django.http import HttpResponseRedirect
from .models import UserProfile
from django.contrib import messages
from django.contrib.auth.models import Group, User
from datetime import datetime, timedelta
from django.utils import timezone

# Inject social-user into db
def map_social_user(backend, user, response, *args, **kwargs):
    if backend.name == 'facebook':
        if not UserProfile.objects.filter(user=user).exists():
            # Create the profile
            p=UserProfile.objects.create(user=user, activation_key="NOT_NEEDED_FACEBOOK", key_expires=timezone.now()+timedelta(days=2))
            p.save()

            # Assign groups to the user
            #user = Group.objects.get(name='players') --??
            #user.user_set.add(p.user) --??

    return {}
