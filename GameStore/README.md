# GameStore
Web Software development - Academic project

## 1. Collaboration
* 3 member team (spring 2017)

## 2. Goal

The goal in this project is to create an online game store for JavaScript games. The service has two types of users: players and developers. Developers can add their games to the service and set a price for it. Players can buy games on the platform and then play purchased games online. The online service holds information on game's high scores and one of our major goals is to enable fluent communication between the service and the games.

The service will be published as a Heroku app and it shall be implemented using Django web framework. 

Our group has the target grade of +2 or +3. We will adjust this target during the project and match it to the effort we are able to put without risking the other courses. 

## 3. Plans

### 3.1. Types of Users and Authentication
In the service there will be two types of users: developers and players. Developers can add their own games for sale in the service and players can browse and buy these games. In the first step both types of users shall have similar capabilites (developer functionalities mainly as a feature that any user can use) and if deemed necessary, they shall be divided into different users with different authorization in the back-end.

For authentication of the user we will be using Django's built-in authentication system with E-mail authentication with Django Console Backend.

#### 3.1.1. Player Functionalities
The player users shall have the following functionalities:

* Browse and buy games, payment is handled by the course’s [mockup payment service](http://payments.webcourse.niksula.hut.fi/)
	* Open text search, which can be targeted to categories. The categories will be set during the projects implementation.
* Play games
* Player is only allowed to play the games they’ve purchased

There shall be a browsing view where available games are listed with action buttons to buy the game. The player shall have also a view for the games he/she has bought and from that list the player can open the playing view of the game. In the playing view there shall be global and personal high scores of the game presented. Most of these functionalities shall be implemented with built-in Django features and the game view shall have the game running in an iframe element. JQuery will be used in the communication between the game and the service (submit score, load/save game state). 


#### 3.1.2. Developer Functionalities
The developer users shall have the following functionalities:

* Add a game (URL) and set price for that game and manage that game (remove, modify)
* Basic game inventory and sales statistics (how many of the developers' games have been bought and when)
* Developers are only allowed to modify/add/etc. their own games

For developer to manage his/her games there shall be own view created. In the view there will be inventory of developer's games listed with action buttons to modify or delete games from the service. When adding a new game we'll use Django forms to input the URL and set the price.

### 3.2. Payment Service
We will use the provided [mockup payment service](http://payments.webcourse.niksula.hut.fi/) for the purchasing of the games in the service. For the payment system we need to create necessary models to the back-end of the service. The payment system isn't on the top of the list in the feature priorities.

### 3.3. Server Side Implementation
As the service will be implemented using Django web framework, server side implementation will be mostly utilizing Django's built-in features. The service shall thus be built with Djangos MTV-pattern. For the minimum viable product there should be views for login, browsing/buying games, uploading a game and playing a game.

The models created for the service play an important role of the server side implementation; it can make things a lot harder in views if the data structure is confusing. It is one of the first priorities to create simple and easily utilizable models for the service. The basic models shall be Users and Games. With these models it should be possible to construct such relations that the basic features can be implemented in the service. Attributes in these models will be versatile and further in development it might be needed to create additional models from the attributes if they are more feasible that way, but these models are the starting point.

The steps in server side implementation shall be the following:

1. Define models
2. Define views
	1. Login and authentication
	2. Browse games
	3. Play game (+ communication between game and game service)
	4. Upload game
3. Additional features to the views and models (e.g. payment system, game developer tools for managing inventory of "own games" etc.)

On top of these steps additional features will be implemented depending on the schedule and deadline.

As the service will be a Heroku app, there are limitations regarding the database that can be used. Because Heroku doesn't support using of SQLite, our service will be using PostgreSQL database instead and the project needs to be configured accordingly. 

### 3.4. Client Side Implementation
On the client side implementation we will be using [Materialize](http://materializecss.com/) front-end framework to bring our Django templates into life. With Materialize we will be able to create quickly beautiful layouts and animations that will enhance user experience. The framework also provides responsivity to the system which is essential for seamless user experience across different platforms. By utilizing static files `materialize.css` and `materialize.js` we will take care of most of the visual layout and interaction in the service. The client side functionalities will be implemented after the server side of the service is proven functional.

Materialize utilizes JQuery in it's functionalities and thus JQuery will also be a needed JavaScript library. JQuery will also be playing an essential role in communication between user actions in games and the game service. Thus JQuery will be utilized widely in our client side implementation.

### 3.5. Priorities
Our first priority is to get the site up and running and then iterate the additional features. We think that the minimum product for this project is the possibility to upload games and then play them. For this to work according to the mandatory criteria we need to have user authentication working. These features are then filled to match the mandatory criteria and finally added with additional features. 

## 4. Process and Time Schedule
We have scheduled the start of project in the week starting at 26.12. We will be using the provided Git repository and the communication will be done by using different social medias. If needed, we can arrange live meetings, but currently we are not concerned about them. We have done many projects online previously and because of our schedule differences, it is more convenient to not force face to face meetings. The primary division of labor is done by communication and we are not using any specific project management tools. This plan will be the major source for our project and we will also update this according the processing.
Although we have experience in agile and Scrum methodology, we are not going to conduct any weekly meetings or tight goals. The project will have changes through it, so the too massive management structure would cause unnecessary labor. Our experience is that the doing is more important than tight plans in university group works.

## 5. Testing
The testing will be conducted with cases that are defined during the development. We will emphasize the testing throughout the entire development process. The bugs and mistakes are very much easier to fix near the major development than afterwards during the implementation. No feature should be added before there are the plan for how to test it. The testing in this project will be conducted using Django's built-in [unit testing tools](https://docs.djangoproject.com/en/1.10/topics/testing/overview/).

## 6. Risk Analysis
There are a multitude of different possible risks in our project. Illness of group members, schedule changes and unforeseen situations can happen during the process, but we are not expecting any major obstacles. Mainly the risks are related to the schedule or getting the different technical aspects running. We are pretty motivated group so the managerial tasks are not big in the current risk scope.
Our biggest concern is the schedule after the 3rd period will start. There will be other courses, which will fill the schedule. This problem is tackled by scheduling most of the doing into the period before 9.1.2017. We will try to add buffer to the time around the deadline. The early release is clearly an option.
It is very probable that there are some unforeseen problems during our development. It is hard to say how these impediments are handled, but in our experience the best way is tight communication and leeway in schedule. Any problem is possible to overcome and very rarely there are problems, which would need external help.
 

