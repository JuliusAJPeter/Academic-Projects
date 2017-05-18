# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('gamestore', '0018_transaction'),
    ]

    operations = [
        migrations.AddField(
            model_name='transaction',
            name='confirmed',
            field=models.BooleanField(default=False),
        ),
    ]
