import datetime, json
from django.shortcuts import render, redirect, get_object_or_404
from .models import Game, UserProfile, Transaction, HighScore
from .forms import RegisterForm
from django.conf import settings
from django.contrib.auth.models import User
from gamestore.forms import GameForm
from django.contrib.messages import get_messages
from django.contrib import messages
from django.core.exceptions import PermissionDenied
from django.core import serializers
from django.contrib.auth.views import login as contrib_login
from hashlib import md5

from django.http import Http404, JsonResponse, HttpResponse

from django.contrib.auth.decorators import login_required

def index(request):
	context = {}
	return render(request, 'gamestore/index.html', context)
	

def games(request, my=False):
	all_games = Game.objects.all()
	owned_games = []
	categories = Game._meta.get_field('category').choices
	current_category = None
	category = request.GET.get('cat', None)
	if request.user.is_authenticated():
		userprofile = request.user.profile
		userTransactions = Transaction.objects.filter(buyer=userprofile, confirmed=True)
		for transaction in userTransactions:
			owned_games.append(transaction.game)
		#If user is the developer of the game also add it to the games list
		if isDeveloper(request.user):
			developed_games = userprofile.developed_games.all()
			for game in developed_games:
				owned_games.append(game)
	if category is None:
		games = all_games
	else:
		games = all_games.filter(category=category)
		for cat in categories:
			if cat[0] == category:
				current_category = cat

	context = {'games': games,
				'all_games': all_games,
				'owned_games': owned_games,
				'categories': categories,
				'current_category': current_category}
	if request.is_ajax():
		return render(request, 'gamestore/gameslist.html', context)
	if my == True:
		context['games'] = owned_games
		context['current_category'] = ("", "my")
		return render(request, 'gamestore/mygames.html', context)
	return render(request, 'gamestore/games.html', context)

@login_required
def mygames(request):
	return games(request, my=True)

@login_required
def game(request, pk, command):
	game = get_object_or_404(Game, pk=pk)
	userprofile = request.user.profile
	highscores = HighScore.objects.filter(game= game).order_by('-score')
	#For highscore
	hindex = 0
	context = {'game': game, 'highscore' : highscores, 'hindex': hindex}
	if request.method == 'POST' and request.is_ajax():
		#Get request to json format and chech score and gamestate
		print(request.body)
		jsonData = json.loads(request.body.decode('utf-8'))
		print(request.body.decode('utf-8'))
		#Check if highscore object founds, create empty if not
		highscore = HighScore.objects.filter(user = userprofile, game=game)

		if highscore.count() == 0:
			newHighScore = HighScore()
			newHighScore.user=userprofile
			newHighScore.game=Game.objects.get(pk=pk)
			newHighScore.score=0
			newHighScore.save()
		#Get the highscore object, which is created just or earlier
		#Do the response accordinly
		highscoreObject = get_object_or_404(HighScore,user= userprofile, game = game)
		if command =="SETTING":
			print("SETTING")
			return HttpResponse(json.dumps(jsonData), content_type='application/json')
		#Score saves the highscore if new
		elif command == "SCORE":
			print("SCORE")
			currentScore = jsonData['score']
			if highscoreObject.score < currentScore:
				highscoreObject.score = currentScore
			highscoreObject.save()
		#Save takes the entire gamestate and saves to data
		elif command == "SAVE":
			print("SAVE")
			highscoreObject.data =json.dumps(jsonData['gameState'])
			highscoreObject.save()
		#Load request tries to load the data and if not possible raises the error. Data is saved in string and converted with json lib
		elif command == "LOAD_REQUEST":
			print("LOAD_REQUEST")
			print(highscore.count())
			if highscore.count() != 1:
				jsonData =""
				jsonData['info']= "Gamestate could not be loaded"
				jsonData['messageType']="ERROR"
				return HttpResponse(json.dumps(jsonData), content_type='application/json')
			else:
				jsonData['gameState']=json.loads(highscoreObject.data)
				jsonData['messageType']="LOAD"
				print()
				return HttpResponse(json.dumps(jsonData), content_type='application/json')
				
	if Transaction.objects.filter(game = game, buyer = userprofile).exists() or userprofile == game.developer:
		return render(request, 'gamestore/game.html', context)
	else:
		raise PermissionDenied
	return redirect('login')



@login_required
def buygame(request, pk):
	context = {}
	game = get_object_or_404(Game, pk=pk)
	userprofile = request.user.profile

	#make sure the game hasnt already been bought by the user
	if Transaction.objects.filter(game = game, buyer = userprofile).exists():
		existing = Transaction.objects.get(game = game, buyer = userprofile)
		if existing.confirmed:				
			return redirect('game', pk)	
		else:
			#Delete the 'unconfirmed' transaction to prevent double payments
			existing.delete()
		
	
	transaction = Transaction(game=game, buyer=userprofile)
	transaction.save()

	pid = transaction.pk
	sid = settings.TRANSACTION_SID
	amount = game.price
	secret_key = settings.TRANSACTION_KEY
	
	checksumstr = "pid={}&sid={}&amount={}&token={}".format(pid, sid, amount, secret_key)
	# checksumstr is the string concatenated above
	m = md5(checksumstr.encode("ascii"))
	checksum = m.hexdigest()
	# checksum is the value that should be used in the payment request
	urls = {
		'payment_service_url': "http://payments.webcourse.niksula.hut.fi/pay/",
		'success_url': "http://localhost:8000/buygame/success/",
		'cancel_url': "http://localhost:8000/buygame/cancel/",
		'error_url': "http://localhost:8000/buygame/error/",
	}

	context = {'game': game,
				'pid': pid, 
				'sid': sid, 
				'amount': amount, 
				'secret_key': secret_key, 
				'checksum': checksum,
				'urls': urls,
				}

	return render(request, 'gamestore/buygame.html', context)

def buygame_success(request):
	context={}
	pid = request.GET.get('pid', '-1')
	ref = request.GET.get('ref', '-1')
	result = request.GET.get('result', 'error')
	checksum = request.GET.get('checksum', '-1')

	#Security check of the payment to prevent forged payments
	service_checksumstr = "pid={}&ref={}&result={}&token={}".format(pid, ref, result, settings.TRANSACTION_KEY)
	m = md5(service_checksumstr.encode("ascii"))
	service_checksum = m.hexdigest()
	
	if checksum == service_checksum and result == 'success':	
		transaction = get_object_or_404(Transaction, pk=pid)
		#check already confirmed transactions
		if transaction.confirmed == True:
			raise Http404("Requested transaction already exists and is confirmed")
		transaction.confirmed = True
		transaction.time_confirmed = datetime.datetime.now()
		context['game'] = transaction.game
		transaction.save()
	else:
		raise Http404("Not found")
	return render(request, 'gamestore/buygame_success.html', context)

def buygame_cancel(request):
	context={}
	pid = request.GET.get('pid', '-1')
	if int(pid) >= 0:
		transaction = get_object_or_404(Transaction, pk=pid)
		#cannot cancel already confirmed payments
		if transaction.confirmed is False:
			transaction.delete()
			return render(request, 'gamestore/buygame_cancel.html', context)			
		else:
			raise Http404("Not found")
	else:
		return render(request, 'gamestore/buygame_error.html', context)
	

def buygame_error(request):
	context={}
	pid = request.GET.get('pid', '-1')
	if int(pid) >= 0:
		transaction = get_object_or_404(Transaction, pk=pid)
		#cannot cancel already confirmed payments
		if transaction.confirmed is False:
			transaction.delete()	
		else:
			raise Http404("Not found")
	return render(request, 'gamestore/buygame_error.html', context)

def login(request):
	if request.user.is_authenticated():
		return redirect(settings.LOGIN_REDIRECT_URL)
	return contrib_login(request)


def register(request):
	if request.user.is_authenticated():
		return redirect('index')
	if request.method == 'POST':
		# create a form instance and populate it with data from the request:
		form = RegisterForm(request.POST)
		# check whether it's valid:
		if form.is_valid() and User.objects.filter(username = form.cleaned_data['user_name']).count() == 0:
			newProfile = UserProfile()
			newProfile.create(form.cleaned_data)
			newProfile.sendActivation()
			return render(request,'registration/registerok.html', {'activation': 'Activation needed'})
	else:
		form = RegisterForm()
	return render(request, 'registration/register.html', {'form': form})
#Used as a help: http://www.b-list.org/weblog/2006/sep/02/django-tips-user-registration/

def confirm(request, activation):
    up = get_object_or_404(UserProfile,activation_key=activation)
    up.sendActivation() #<---- is this needed? the activation has been sent in register already.
    success = up.activateUser()
    if success=="Activation OK":
    	return render(request, 'registration/registerok.html', {'activation': 'Activation ok'})
    if success=="Already activated":
    	return render(request, 'registration/registerok.html', {'activation': 'Already Activated'})
    return render(request,'registration/registerok.html', {'activation': success})

@login_required
def devpage(request):
	userprofile = request.user.profile
	if isDeveloper(request.user):	
		try:
			list_of_games = Game.objects.filter(developer = userprofile)
			my_transactions=[]
			for game in list_of_games:
				transactions = Transaction.objects.filter(game=game)
				for t in transactions:
					my_transactions.append(t)
		except Game.DoesNotExist:
			list_of_games = None
		context = {'list_of_games': list_of_games,
					'transactions': my_transactions}
		return render(request, 'developer/devpage.html', context)
	else:
		raise PermissionDenied

def isDeveloper(user):
	if user.groups.filter(name='Developer').exists():
		return True
	else:
		return False

	
@login_required
def addgame(request):
	context = {}

	if isDeveloper(request.user):

		if request.method == 'POST':
			addgame_form = GameForm(data=request.POST)
			if addgame_form.is_valid():
				newgame = addgame_form.save()
				newgame.developer = request.user.profile
				newgame.save()
				# transaction = Transaction(game=newgame, buyer=newgame.developer)
				# transaction.confirmed = True
				# transaction.save()
			list_of_games = Game.objects.all()
			return redirect('devpage')
		return render(request, 'developer/addgame.html', {'addgame_form': GameForm()})
	else:
		raise PermissionDenied

def checkDevAuth(user, game):
	userprofile = get_object_or_404(UserProfile,user=user)
	games = userprofile.developed_games.all()

	#Check if the user is the developer of the game under editing
	if game in games:
		return True
	else:
		return False

@login_required
def editgame(request, id):
	game = get_object_or_404(Game,pk=id)
	isGameDev = checkDevAuth(request.user,game)
	if isGameDev:
		if request.method == 'POST':
			game_form = GameForm(data=request.POST)
			game.name = request.POST.get('name','')
			game.category = request.POST.get('category','')
			game.price = request.POST.get('price','')
			game.description = request.POST.get('description','')
			game.url = request.POST.get('url','')
			game.save()

			# list_of_games = Game.objects.all()
			return redirect('devpage')
		else:
			form = GameForm(
				initial = { 'name':game.name, 'category':game.category, 'price':game.price, 'description':game.description, 'url':game.url }
			)
		return render(request, 'developer/editgame.html', {'game':game, 'game_form':form})
	else:
		raise PermissionDenied

@login_required
def delgame(request, id):
	game = get_object_or_404(Game,pk=id)
	isGameDev = checkDevAuth(request.user,game)
	if isGameDev:
		if request.method == 'POST':
			game.delete()

			# list_of_games = Game.objects.all()
			return redirect('devpage')
		else:
			form = GameForm(
				initial = { 'name':game.name, 'category':game.category, 'price':game.price, 'description':game.description, 'url':game.url }
			)
		return render(request, 'developer/delgame.html', {'game':game, 'game_form':form})
	else:
		raise PermissionDenied


