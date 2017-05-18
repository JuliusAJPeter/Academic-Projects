from tastypie.resources import ModelResource, ALL, ALL_WITH_RELATIONS
from tastypie import fields
from .models import Game, HighScore, UserProfile
from django.contrib.auth.models import User


class GameResource(ModelResource):
    class Meta:
        queryset = Game.objects.all()
        resource_name = 'game'
        include_resource_uri = False
        allowed_methods = ['get']

class UserResource(ModelResource):
    class Meta:
        queryset = User.objects.all()
        resource_name = 'user'
        include_resource_uri = False
        fields = ['username']

class UserProfileResource(ModelResource):
    class Meta:
        queryset = UserProfile.objects.all()
        resource_name = 'userprofile'
        allowed_methods = ['get']
        include_resource_uri = False
        fields = ['id']
    def dehydrate(self, bundle):
        bundle.data['username'] = bundle.obj.user.username
        return bundle

class HighScoreResource(ModelResource):
    # game = fields.ForeignKey(GameResource, 'game',full=True)
    # user = fields.ForeignKey(UserProfileResource, 'user', full=True)

    class Meta:
        queryset = HighScore.objects.all()
        resource_name = 'highscore'
        include_resource_uri = False
        excludes = ['data']
        allowed_methods = ['get']


    def dehydrate(self, bundle):
        bundle.data['user'] = bundle.obj.user
        bundle.data['game'] = bundle.obj.game
        return bundle

