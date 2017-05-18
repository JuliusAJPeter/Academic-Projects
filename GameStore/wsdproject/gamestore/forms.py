from django import forms
from gamestore.models import Game
from django.forms import Select, RadioSelect

GROUP = ((1,'Developer'),
               (2,'Player'),)

class RegisterForm(forms.Form):
    user_name = forms.CharField(label='Username', max_length=100)
    email = forms.EmailField(label="Email")
    groups = forms.MultipleChoiceField(choices=GROUP, widget=forms.CheckboxSelectMultiple(), required = True)
    password = forms.CharField(widget=forms.PasswordInput)


class GameForm(forms.ModelForm):

	#category = forms.ChoiceField(widget=forms.Select(), choices=CHOICES, required=True)

	class Meta:
		model = Game
		fields = ('name','category','price','description','url')
		widgets = {
            'category': forms.Select(attrs={'class': 'browser-default'}),
        }

