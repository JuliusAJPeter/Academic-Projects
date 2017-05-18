# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('gamestore', '0015_auto_20170116_1300'),
    ]

    operations = [
        migrations.AddField(
            model_name='game',
            name='developer',
            field=models.ForeignKey(to='gamestore.UserProfile', null=True),
        ),
    ]
