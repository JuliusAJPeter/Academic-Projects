# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('gamestore', '0016_game_developer'),
    ]

    operations = [
        migrations.AlterField(
            model_name='game',
            name='developer',
            field=models.ForeignKey(related_name='developed_games', to='gamestore.UserProfile', null=True),
        ),
    ]
