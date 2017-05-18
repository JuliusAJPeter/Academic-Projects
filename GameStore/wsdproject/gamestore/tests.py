from django.test import TestCase, Client
from django.contrib.auth.models import User
from .models import Game, Transaction, HighScore, UserProfile
from .forms import RegisterForm, GameForm
from django.core.urlresolvers import reverse
from django.template.loader import render_to_string
from datetime import timedelta
from django.utils import timezone
from django.core import mail


class ModelsTestCase(TestCase):
    fixtures = ['test_data.json',]
    dev_profile = None
    play_profile = None

    def setUp(self):
        
        self.client = Client()
        self.dev_profile = UserProfile.objects.get(pk=1)
        self.play_profile = UserProfile.objects.get(pk=2)

    def testUserProfileObject(self):

        #Testing user groups

        dev_groups = []
        play_groups = []
        dev_groups_objects = self.dev_profile.user.groups.all()
        play_groups_objects = self.play_profile.user.groups.all()

        for group in dev_groups_objects:
            dev_groups.append(group.id)

        for group in play_groups_objects:
            play_groups.append(group.id)

        self.assertEqual(dev_groups, [1,2], "Testing that developer has correct groups")
        self.assertEqual(play_groups, [2], "Testing that player has correct groups")

        #Testing UserProfile creation

        user_data = {'user_name': "MrTest", 'email': "test@test.com", 'groups': [2], 'password': 'asdasd'}
        testuser = UserProfile()
        testuser.create(user_data)
        self.assertEqual(testuser.user.username, "MrTest", "Testing created userprofile has correct user linked to it...")
        self.assertEqual(str(testuser), "MrTest", "Testing userprofile str return value is correct...")


        #Testing UserProfile activation

        testuser.activateUser()
        self.assertEqual(testuser.user.is_active, True, "Testing if user activation works...")

        res = testuser.activateUser()
        self.assertEqual(res, "Already activated", "Testing activation of already activated...")

        testuser.user.is_active = False
        testuser.key_expires = timezone.now()+timedelta(days=-3)

        res = testuser.activateUser()
        self.assertEqual(res, "Code Expired", "Testing expired key...")

        #Testing email sending

        testuser.sendActivation()
        self.assertEqual(len(mail.outbox), 1, "Test that one message has been sent.")
        self.assertEqual(mail.outbox[0].subject, 'Activation mail', "Verify that the subject of the first message is correct.")



    def testGameObject(self):

        #Test game object creation

        Game.objects.create(name="Another test game", category="AC", price=88.0, 
            description="Test description", url="http://webcourse.cs.hut.fi/example_game.html",
            developer=self.dev_profile)
        testgame = Game.objects.get(name="Another test game")
        self.assertEqual(testgame.name, "Another test game", "Testing game creation...")
        self.assertEqual(str(testgame), "Another test game (88.0)", "Testing that game has correct str return value...")


        #Test correct developer
        self.assertEqual(testgame.developer.pk, 1, "Testing that game has correct user as developer...")


    def testTransactions(self):

        #Test transaction creation

        testgame = Game.objects.get(pk=1)
        Transaction.objects.create(game=testgame,buyer=self.play_profile)
        transaction = Transaction.objects.get(game=testgame, buyer=self.play_profile)
        user_game = self.play_profile.owned_games.get(game=testgame).game
        self.assertEqual(user_game, testgame, "Testing if transaction was properly created and user has reverse relation to the transaction")

        #Transaction shouldn't be confirmed at this stage (game not literally owned)
        self.assertEqual(transaction.confirmed, False, "Testing if transaction is confirmed")

    #def testHighScores(self):
        #TODO...



class ViewsTestCase(TestCase):
#This uses more ready-made data from fixtures with ready-made games and ready-made transactions
    fixtures = ['init_data.json',]

    def setUp(self):
        self.client = Client()

    def testViewUrls(self):
        
        #Testing urls
        url = ""
        url = reverse('index')
        self.assertEqual(url, '/')
        url = reverse('games')
        self.assertEqual(url, '/games/')
        url = reverse('my_games')
        self.assertEqual(url, '/games/mygames/')
        url = reverse('game', kwargs={'pk': 1})
        self.assertEqual(url, '/games/1/')
        url = reverse('login')
        self.assertEqual(url, '/login/')
        url = reverse('logout')
        self.assertEqual(url, '/logout/')
        url = reverse('buygame', kwargs={'pk': 1})
        self.assertEqual(url, '/buygame/1/')
        url = reverse('register')
        self.assertEqual(url, '/register/')
        url = reverse('devpage')
        self.assertEqual(url, '/devpage/')
        url = reverse('addgame')
        self.assertEqual(url, '/addgame/')
        url = reverse('editgame', kwargs={'id': 1})
        self.assertEqual(url, '/editgame/1/')
        url = reverse('delgame', kwargs={'id': 1})
        self.assertEqual(url, '/delgame/1/')

    def testLogin(self):
        res = self.client.post(reverse('login'), {'username':'developer1', 'password':'asdasd'}, follow=True)
        self.assertTrue(res.context['user'].is_active)

        self.client.logout()

        #wrong credentials
        res = self.client.post(reverse('login'), {'username':'developer1', 'password':'sdfsdf'}, follow=True)
        self.assertFalse(res.context['user'].is_active)

    def testInvalidUrls(self):
        self.client.login(username='player1', password='asdasd')
        res = self.client.get(reverse('game', kwargs={'pk': 00}))
        self.assertEqual(res.status_code, 404, "Testing non-existing game code")
        self.client.logout()
        
        #login as developer to check invalid devpage
        self.client.login(username='developer1', password='asdasd')
        res = self.client.get(reverse('editgame', kwargs={'id': 00}))
        self.assertEqual(res.status_code, 404, "Testing non-existing game code on editgame")
        res = self.client.get(reverse('delgame', kwargs={'id': 00}))
        self.assertEqual(res.status_code, 404, "Testing non-existing game code on delgame")
        self.client.logout()

    def testDevOnlyViews(self):
        #login as developer
        self.client.login(username='developer1', password='asdasd')
        res = self.client.get(reverse('devpage'))
        self.assertEqual(res.status_code, 200, "Testing that developer gets to devpage")
        res = self.client.get(reverse('editgame', kwargs={'id': 1}))
        self.assertEqual(res.status_code, 200, "Testing that developer gets to editpage of his own game")
        res = self.client.get(reverse('delgame', kwargs={'id': 1}))
        self.assertEqual(res.status_code, 200, "Testing that developer gets to deletepage of his own game")
        res = self.client.get(reverse('addgame'))
        self.assertEqual(res.status_code, 200, "Testing that developer gets to add new game")

        #testing editing/deleting someone elses games
        res = self.client.get(reverse('editgame', kwargs={'id': 3}))
        self.assertEqual(res.status_code, 403, "Testing editing someone elses game")
        res = self.client.get(reverse('delgame', kwargs={'id': 3}))
        self.assertEqual(res.status_code, 403, "Testing deleting someone elses game")
        self.client.logout()

        #testing that player doesnt have access to developer specific pages
        self.client.login(username='player1', password='asdasd')
        res = self.client.get(reverse('devpage'))
        self.assertEqual(res.status_code, 403, "Testing that player doesn't get to devpage")
        res = self.client.get(reverse('addgame'))
        self.assertEqual(res.status_code, 403, "Testing that player doesn't get to add game")
        #(no need to test editgame and delgame if the user cannot even add game)
        self.client.logout()

    def testGameViews(self):
        #test everyone has access to games
        res = self.client.get(reverse('games'))
        self.assertEqual(res.status_code, 200, "Testing that games browsing is accessed by everyone")
        #for anonymous users game-pages are restricted and redirected to login
        res = self.client.get(reverse('game', kwargs={'pk': 1}))
        self.assertRedirects(res, '/login/?next=/games/1/')

        #testing that player can get to game page of owned game
        #In the init_data  player1 (pk=3) has bought game (pk=1) 
        self.client.login(username='player1', password='asdasd')
        res = self.client.get(reverse('game', kwargs={'pk': 1}))
        self.assertEqual(res.status_code, 200, "Testing that player has access to owned game")

        #testing that player cant get to game page of not owned games
        res = self.client.get(reverse('game', kwargs={'pk': 7}))
        self.assertEqual(res.status_code, 403, "Testing that player cannot access not owned game")
        self.client.logout()

        #testing that developer can play self developed games
        self.client.login(username='developer1', password='asdasd')
        res = self.client.get(reverse('game', kwargs={'pk': 1}))
        self.assertEqual(res.status_code, 200, "Testing that developer can play self developed games")

        #but not others...
        res = self.client.get(reverse('game', kwargs={'pk': 3}))
        self.assertEqual(res.status_code, 403, "Testing that developer cant play other developers' games")
        self.client.logout()

    def testBuyingFeatures(self):
        #only logged in users can buy games
        res = self.client.get(reverse('buygame', kwargs={'pk': 1}))
        self.assertRedirects(res, '/login/?next=/buygame/1/')

        self.client.login(username='player1', password='asdasd')
        res = self.client.get(reverse('buygame', kwargs={'pk': 7}))
        self.assertEqual(res.status_code, 200, "Testing that player can get to buying page")

        #already owned game
        res = self.client.get(reverse('buygame', kwargs={'pk': 1}))
        self.assertRedirects(res, '/games/1/')

        #test success, cancel and error pages
        res = self.client.get("buygame/success/?&pid=14&ref=11162&result=success&checksum=3490bbb3b01a75aee2384016c651aa70")
        self.assertEqual(res.status_code, 404, "Testing trying to fake transaction")
        res = self.client.get("buygame/cancel/?&pid=1")
        self.assertEqual(res.status_code, 404, "Testing trying to cancel already confirmed transactions")
        res = self.client.get("buygame/error/?&pid=1")
        self.assertEqual(res.status_code, 404, "Testing trying to 'error' already confirmed transactions")

        self.client.logout()

    #FORMS

    def testRegisterForm(self):
        #correct data
        user_data = {'user_name': "MrTest", 'email': "test@test.com", 'groups': [2], 'password': 'asdasd'}
        form = RegisterForm(user_data)
        self.assertTrue(form.is_valid())

        res = self.client.post(reverse('register'), user_data)
        self.assertEqual(res.status_code, 200, "Testing that registration form works correctly")


        #wrong data
        user_data = {'user_name': "", 'email': "test@test.com", 'groups': [2], 'password': ''}
        form = RegisterForm(user_data)
        self.assertFalse(form.is_valid())
        user_data = {'user_name': "MrTest", 'email': "", 'groups': [2], 'password': 'asdasd'}
        form = RegisterForm(user_data)
        self.assertFalse(form.is_valid())
        user_data = {'user_name': "MrTest", 'email': "test@test.com", 'groups': [], 'password': 'asdasd'}
        form = RegisterForm(user_data)
        self.assertFalse(form.is_valid())
        user_data = {'user_name': "MrTest", 'email': "testtest.com", 'groups': [2], 'password': 'asdasd'}
        form = RegisterForm(user_data)
        self.assertFalse(form.is_valid())
        user_data = {'user_name': "MrTest", 'email': "test@test.com", 'groups': [3], 'password': 'asdasd'}
        form = RegisterForm(user_data)
        self.assertFalse(form.is_valid())

    def testAddGameForm(self):
        #correct data
        game_data = {'name': "test game",'category': "OT",'price': 13,'description': "This is a test description",'url': "http://webcourse.cs.hut.fi/example_game.html"}
        form = GameForm(game_data)
        self.assertTrue(form.is_valid())

        self.client.login(username='developer1', password='asdasd')
        res = self.client.post(reverse('addgame'), game_data)
        self.assertRedirects(res, '/devpage/')

        #wrong data
        game_data = {'name': "",'category': "OT",'price': 13,'description': "This is a test description",'url': "http://webcourse.cs.hut.fi/example_game.html"}
        form = GameForm(game_data)
        self.assertFalse(form.is_valid())
        game_data = {'name': "test game",'category': "OTTHER",'price': 13,'description': "This is a test description",'url': "http://webcourse.cs.hut.fi/example_game.html"}
        form = GameForm(game_data)
        self.assertFalse(form.is_valid())
        game_data = {'name': "test game",'category': "OT",'price': "",'description': "This is a test description",'url': "http://webcourse.cs.hut.fi/example_game.html"}
        form = GameForm(game_data)
        self.assertFalse(form.is_valid())
        game_data = {'name': "test game",'category': "OT",'price': 13,'description': "",'url': "http://webcourse.cs.hut.fi/example_game.html"}
        form = GameForm(game_data)
        self.assertFalse(form.is_valid())
        game_data = {'name': "test game",'category': "OT",'price': 13,'description': "This is a test description",'url': "http/webcourse.cs.hut.fi/example_game.html"}
        form = GameForm(game_data)
        self.assertFalse(form.is_valid())

class TemplateTestCase(TestCase):
    fixtures = ['init_data.json',]

    def setUp(self):
        self.client = Client()


    def testBrowseGameContents(self):
        games = Game.objects.all()
        res = self.client.get(reverse('games'))
        self.assertTemplateUsed(res, "gamestore/games.html", "Testing that the right template was rendered")
        #all games
        for game in games:
            self.assertContains(res, game.name)
            self.assertContains(res, game.description)
            self.assertContains(res, game.price)
        #AJAX get action games
        res = self.client.get(reverse('games')+"?cat=AC", HTTP_X_REQUESTED_WITH='XMLHttpRequest')
        self.assertTemplateUsed(res, "gamestore/gameslist.html", "Testing that the right template was rendered")
        self.assertNotContains(res, "<html>")
        self.assertNotContains(res, "<body>")
        self.assertTemplateNotUsed(res, "gamestore/games.html", "Testing that games.html was not rendered in ajax")
        action_games = Game.objects.filter(category="AC")
        for game in action_games:
            self.assertContains(res, game.name)
            self.assertContains(res, game.description)
            self.assertContains(res, game.price)
        #Make sure only action games are rendered
        other_games = Game.objects.exclude(category="AC")
        for game in other_games:
            self.assertNotContains(res, game.name)
            self.assertNotContains(res, game.description)
            self.assertNotContains(res, game.price)

    def testMyGamesContents(self):
        games = Game.objects.all()
        self.client.login(username='player1', password='asdasd')
        res = self.client.get(reverse('my_games'))
        self.assertTemplateUsed(res, "gamestore/gameslist.html", "Testing that the right template was rendered")
        owned_games = []
        transactions = Transaction.objects.filter(buyer=res.context['user'].profile, confirmed=True)

        for transaction in transactions:
            owned_games.append(transaction.game)
        for game in owned_games:
            self.assertContains(res, game.name)
            self.assertContains(res, game.description)
            self.assertContains(res, game.price)

    def testDevPageContents(self):
        self.client.login(username='developer1', password='asdasd')
        res = self.client.get(reverse('devpage'))
        self.assertTemplateUsed(res, "developer/devpage.html", "Testing that the right template was rendered")
        
        dev2 = User.objects.get(pk=2)
        dev_games = Game.objects.filter(developer=res.context['user'].profile)
        dev2_games = Game.objects.filter(developer=dev2.profile)

        #make sure own games are listed
        for game in dev_games:
            self.assertContains(res, game.name)
            self.assertContains(res, game.description)
            self.assertContains(res, game.price)

        #make sure only own games are listed
        for game in dev2_games:
            self.assertNotContains(res, game.name)
            self.assertNotContains(res, game.description)
            self.assertNotContains(res, game.price)

        #Make sure added game is shown
        game_data = {'name': "test game",'category': "OT",'price': 13,'description': "This is a test description",'url': "http://webcourse.cs.hut.fi/example_game.html"}
        self.client.post(reverse('addgame'), game_data)
        res = self.client.get(reverse('devpage'))
        self.assertContains(res, "test game")
