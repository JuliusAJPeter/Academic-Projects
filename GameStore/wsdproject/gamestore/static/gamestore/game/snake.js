
  /* global $, alert */

  $(document).ready( function() {
    "use strict";

    // Sends this game's state to the service.
    // The format of the game state is decided
    // by the game
    $("#save").click( function () {
      var msg = {
        "messageType": "SAVE",
        "gameState": {
          "score": score,
          "grid": {'_grid':grid._grid,
          			'width': grid.width,
          			'height': grid.height},
          "snake": {'direction': snake.direction,
      				'last': snake.last,
      				'_queue': snake._queue}

        }
      };
      window.parent.postMessage(msg, "*");
    });

    // Sends a request to the service for a
    // state to be sent, if there is one.
    $("#load").click( function () {
      var msg = {
        "messageType": "LOAD_REQUEST",
      };
      window.parent.postMessage(msg, "*");
    });

    // Listen incoming messages, if the messageType
    // is LOAD then the game state will be loaded.
    // Note that no checking is done, whether the
    // gameState in the incoming message contains
    // correct information.
    //
    // Also handles any errors that the service
    // wants to send (displays them as an alert).
    window.addEventListener("message", function(evt) {
      if(evt.data.messageType === "LOAD") {
        grid._grid = evt.data.gameState.grid._grid;
        grid.width = evt.data.gameState.grid.width;
        grid.height = evt.data.gameState.grid.height;
        snake.direction = evt.data.gameState.snake.direction;
        snake.last = evt.data.gameState.snake.last;
        snake._queue = evt.data.gameState.snake._queue;
        score = evt.data.gameState.score;
        //stop the loop to prevent multiple loops that make the game crazy fast
        stopLoop();
        loop();
      } else if (evt.data.messageType === "ERROR") {
        alert(evt.data.info);
      }
    });

    // Request the service to set the resolution of the
    // iframe correspondingly
    var message =  {
      messageType: "SETTING",
      options: {
      	"width": COLS*20+15, //Integer
        "height": ROWS*20+70 //Integer
        }
    };
    window.parent.postMessage(message, "*");

});


var
/**
 * Constats
 */
COLS = 26,
ROWS = 26,

EMPTY = 0,
SNAKE = 1,
FRUIT = 2,

LEFT  = 0,
UP    = 1,
RIGHT = 2,
DOWN  = 3,

KEY_LEFT  = 37,
KEY_UP    = 38,
KEY_RIGHT = 39,
KEY_DOWN  = 40,
PAUSE = 112,

/**
 * Game objects
 */
freeze=false,
canvas,	  /* HTMLCanvas */
ctx,	  /* CanvasRenderingContext2d */
keystate, /* Object, used for keyboard inputs */
frames,   /* number, used for animation */
score;  /* number, keep track of the player score */

grid = {

	width: null,  /* number, the number of columns */
	height: null, /* number, the number of rows */
	_grid: null,  /* Array<any>, data representation */

	init: function(d, c, r) {
		this.width = c;
		this.height = r;

		this._grid = [];
		for (var x=0; x < c; x++) {
			this._grid.push([]);
			for (var y=0; y < r; y++) {
				this._grid[x].push(d);
			}
		}
	},

	set: function(val, x, y) {
		this._grid[x][y] = val;
	},

	get: function(x, y) {
		return this._grid[x][y];
	}
}

snake = {

	direction: null, /* number, the direction */
	last: null,		 /* Object, pointer to the last element in the queue */
	_queue: null,	 /* Array<number>, data representation*/

	init: function(d, x, y) {
		this.direction = d;

		this._queue = [];
		this.insert(x, y);
	},

	insert: function(x, y) {
		// unshift prepends an element to an array
		this._queue.unshift({x:x, y:y});
		this.last = this._queue[0];
	},

	remove: function() {
		// pop returns the last element of an array
		return this._queue.pop();
	}
};

function setFood() {
	var empty = [];
	// iterate through the grid and find all empty cells
	for (var x=0; x < grid.width; x++) {
		for (var y=0; y < grid.height; y++) {
			if (grid.get(x, y) === EMPTY) {
				empty.push({x:x, y:y});
			}
		}
	}
	// chooses a random cell
	var randpos = empty[Math.round(Math.random()*(empty.length - 1))];
	grid.set(FRUIT, randpos.x, randpos.y);
}

/**
 * Starts the game
 */
function main() {
	// create and initiate the canvas element
	canvas = document.createElement("canvas");
	canvas.width = COLS*20;
	canvas.height = ROWS*20;
	ctx = canvas.getContext("2d");
	// add the canvas element to the body of the document
	document.body.appendChild(canvas);

	// sets an base font for bigger score display
	ctx.font = "12px Helvetica";

	frames = 0;
	keystate = {};
	// keeps track of the keybourd input
	document.addEventListener("keydown", function(evt) {
		keystate[evt.keyCode] = true;
	});
	document.addEventListener("keyup", function(evt) {
		delete keystate[evt.keyCode];
	});
	document.addEventListener("keypress", function(evt) {
		if(evt.keyCode == PAUSE){
			toggleFreeze();
		}
	});

	// intatiate game objects and starts the game loop
	init();
	loop();
}

function init() {
	grid.init(EMPTY, COLS, ROWS);

	var sp = {x:Math.floor(COLS/2), y:ROWS-1};
	snake.init(UP, sp.x, sp.y);
	grid.set(SNAKE, sp.x, sp.y);

	setFood();

	score = 0;
}

var request;

function loop() {
	update();
	draw();
	// When ready to redraw the canvas call the loop function
	// first. Runs about 60 frames a second
	request = window.requestAnimationFrame(loop, canvas);
}

function stopLoop(){
	if (request) {
       window.cancelAnimationFrame(request);
       request = undefined;
    }
}

function toggleFreeze(){
	freeze = !freeze;
}

function update() {
	if(freeze){
		return
	}
	frames++;

	// changing direction of the snake depending on which keys that are pressed
	if (keystate[KEY_LEFT] && snake.direction !== RIGHT) {
		snake.direction = LEFT;
	}
	if (keystate[KEY_UP] && snake.direction !== DOWN) {
		snake.direction = UP;
	}
	if (keystate[KEY_RIGHT] && snake.direction !== LEFT) {
		snake.direction = RIGHT;
	}
	if (keystate[KEY_DOWN] && snake.direction !== UP) {
		snake.direction = DOWN;
	}
	if (keystate[PAUSE]) {
		toggleFreeze();
	}

	// each ten frames update the game state.
	if (frames%5 === 0) {
		// pop the last element from the snake queue i.e. the head
		var nx = snake.last.x;
		var ny = snake.last.y;

		// updates the position depending on the snake direction
		switch (snake.direction) {
			case LEFT:
				nx--;
				break;
			case UP:
				ny--;
				break;
			case RIGHT:
				nx++;
				break;
			case DOWN:
				ny++;
				break;
		}

		// checks all gameover conditions
		if (0 > nx || nx > grid.width-1  ||
			0 > ny || ny > grid.height-1 ||
			grid.get(nx, ny) === SNAKE
		) {
			submitScore();
			return init();
		}

		// check wheter the new position are on the fruit item
		if (grid.get(nx, ny) === FRUIT) {
			// increment the score and sets a new fruit position
			score++;
			setFood();
		} else {
			// take out the first item from the snake queue i.e the tail and remove id from grid
			var tail = snake.remove();
			grid.set(EMPTY, tail.x, tail.y);
		}

		// add a snake id at the new position and append it to the snake queue
		grid.set(SNAKE, nx, ny);
		snake.insert(nx, ny);
	}
}

function submitScore(){
	var msg = {
        "messageType": "SCORE",
        "score": score
      };
      window.parent.postMessage(msg, "*");
}

function draw() {
	// calculate tile-width and -height
	var tw = canvas.width/grid.width;
	var th = canvas.height/grid.height;
	// iterate through the grid and draw all cells
	for (var x=0; x < grid.width; x++) {
		for (var y=0; y < grid.height; y++) {
			// sets the fillstyle depending on the id of each cell
			switch (grid.get(x, y)) {
				case EMPTY:
					ctx.fillStyle = "#fff";
					break;
				case SNAKE:
					ctx.fillStyle = "#0ff";
					break;
				case FRUIT:
					ctx.fillStyle = "#f00";
					break;
			}
			ctx.fillRect(x*tw, y*th, tw, th);
		}
	}
	// changes the fillstyle once more and draws the score message to the canvas
	ctx.fillStyle = "#000";
	ctx.fillText("Score: " + score, 5, canvas.height-15);
	ctx.fillText("Press 'p' to pause", 5, canvas.height-3);
}

// start and run the game
main();


