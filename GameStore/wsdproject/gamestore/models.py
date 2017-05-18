from __future__ import unicode_literals
import hashlib, random
from datetime import timedelta
from django.utils import timezone
from django.db import models
from django.contrib.auth.models import User, Group
from django.core.mail import send_mail

class Game(models.Model):
    CATEGORY_CHOICES = (
        ('AC', 'Action'),
        ('AD', 'Adventure'),
        ('AR', 'Arcade'),
        ('PZ', 'Puzzle'),
        ('RC', 'Racing'),
        ('SH', 'Shooting'),
        ('SP', 'Sports'),
        ('OT', 'Other'),
    )

    name = models.CharField(max_length=200, unique=True)
    category = models.CharField(max_length=2, choices=CATEGORY_CHOICES, default='OT')
    price = models.FloatField()
    description = models.CharField(max_length=200)
    url = models.URLField()
    developer = models.ForeignKey('UserProfile', on_delete=models.CASCADE, null=True, related_name='developed_games', unique=False)
    def __str__(self):
        return "{} ({})".format(self.name, self.price)

class Transaction(models.Model):
    game = models.ForeignKey('Game', on_delete=models.CASCADE, related_name='sales')
    buyer = models.ForeignKey('UserProfile', on_delete=models.CASCADE, related_name='owned_games')
    confirmed = models.BooleanField(default=False)
    time_confirmed = models.DateTimeField(null=True)


#http://stackoverflow.com/questions/24935271/django-custom-user-email-account-verification
class HighScore(models.Model):
    game = models.ForeignKey('Game', on_delete=models.CASCADE, related_name='game')
    user = models.ForeignKey('UserProfile', on_delete=models.CASCADE, related_name='player')
    score = models.FloatField()
    data = models.CharField(max_length =300,default='{"score": 0,"playerItems": []}')
    def __str__(self):
        return "{} ({}){}DATA:{}".format(self.game, self.user,self.score,self.data)
#This one is to add the activation key to the default user, can be used also for eg. games
class UserProfile(models.Model):
    user = models.OneToOneField(User, related_name='profile') #1 to 1 link with Django User
    activation_key = models.CharField(max_length=40)
    key_expires = models.DateTimeField()

    def __str__(self):
        return self.user.username



#http://www.b-list.org/weblog/2006/sep/02/django-tips-user-registration/
    def create(self, userData):
        print(userData['user_name'])
        self.user = User.objects.create_user(userData['user_name'], userData['email'], userData['password'])

        #Assign groups to user
        for data in userData['groups']:
            if int(data) == 1:
                group, created = Group.objects.get_or_create(name='Developer')
                group.user_set.add(self.user)
            elif int(data) == 2:
                group, created = Group.objects.get_or_create(name='Player')
                group.user_set.add(self.user)
        self.user.is_active = False
        self.user.save()
        self.key_expires = timezone.now()+timedelta(days=2)
        salt = hashlib.sha1(str(random.random())).hexdigest()[:5]
        self.activation_key = hashlib.sha1(salt+self.user.username).hexdigest()
        self.save()
        print(self.activation_key)
    def activateUser(self):
        user_account = self.user
        if self.key_expires < timezone.now():
            return "Code Expired"
        if user_account.is_active:
            return "Already activated"

        user_account.is_active = True
        user_account.save()
        return "Activation OK"

    def sendActivation(self):
        send_mail('Activation mail','You activation link is http://127.0.0.1:8000/register/confirm/'+self.activation_key+"/",'test@djangoapp.com',[self.user.email],fail_silently=False,)
