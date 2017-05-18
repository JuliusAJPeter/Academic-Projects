const express = require('express');
const app = express();
const bodyParser= require('body-parser')
const MongoClient = require('mongodb').MongoClient

app.use(bodyParser.urlencoded({extended: true}))

var db
MongoClient.connect('mongodb://mccg09:mccg09@ds159217.mlab.com:59217/mccg09', (err, database) => {
   if (err) return console.log(err)
  db = database
  app.listen(3000, () => {
    console.log('listening on 3000')
  })
})

/*app.get('/', (req, res) => {
   res.sendFile(__dirname + '/index.html')
})*/

app.post('/store', (req, res) => {
  db.collection('quotes').save(req.body, (err, result) => {
    if (err) return console.log(err)

    console.log('saved to database')
    res.redirect('/')
  })
})


app.get('/store', (req, res) => {
	var appToken = req.body.appToken
  db.collection('quotes').find().toArray(function(err, results) {
  
  var intCount = results.length;
  for (var i=0; i<intCount; i++){
  	if (results[i]['appToken']==appToken){
  		console.log(results[i])

  	}
  }
})
})
