from django.conf.urls import url
from django.conf.urls import include
from . import views
from django.contrib import admin
from django.contrib.auth import views as auth_views

urlpatterns = [
    url(r'^$', views.index, name='index'),
    url(r'^games/$', views.games, name='games'),
    url(r'^games/mygames/$', views.mygames,
    	name='my_games'),
    url(r'^games/(?P<pk>\d+)/$', views.game,
    	 {'command':'GET'},
    	name='game',),
    url(r'^games/(?P<pk>\d+)/(?P<command>[\w\-]*)/$' ,views.game,
        name='gamedata'),
    
    # url(r'^games/(?P<pk>\d+)/(?P<action>.*)$', views.game, name='game',),

    url(r'^login/$', views.login, name='login'),
    url(r'^logout/$', auth_views.logout, {'next_page': 'index'}, name='logout'),
    url(r'^admin/', admin.site.urls),

    # url(r'^testgame/$', views.testgame,{'test':"empty"}),
    # url(r'^testgame/(?P<test>.*)$', views.testgame, name='testgame',),

    url(r'^buygame/(?P<pk>\d+)/$', views.buygame, name='buygame'),
    url(r'^buygame/success/', views.buygame_success),
    url(r'^buygame/cancel/', views.buygame_cancel),
    url(r'^buygame/error/', views.buygame_error),

    url(r'^register/$', views.register, name='register'),
    url(r'^register/confirm/(?P<activation>.*)/$', views.confirm),

    url(r'^devpage/', views.devpage, name='devpage'),
    url(r'^addgame/', views.addgame, name='addgame'),
    url(r'^editgame/(?P<id>[0-9]+)/$', views.editgame, name='editgame'),
    url(r'^delgame/(?P<id>[0-9]+)/$', views.delgame, name='delgame'),
    #https://github.com/yudazilian/SocialAuthDjangoTutorial
    url(r'^oauth/', include('social_django.urls', namespace='social')), 
    # url(r'^oauth/', include('social.apps.django_app.urls', namespace='social')),  # fb login
]
