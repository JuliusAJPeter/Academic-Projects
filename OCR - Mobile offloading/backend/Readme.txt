/login method=post
	- requires JSON{username, password (in hash)}
	- returns JSON{username, token, login(success/failure)}

/image method=post enctype=multipart/form-data
	- requires JSON{appToken, ocrPhoto <multiple>} ** currently supports 2 image processing together and this can be altered.
	- returns JSON[{thumbnail, text}] //JSON array
		thumbnail - thumbnail image encoded as base64 string
		text - text extracted from source file

/store method=get
	- requires JSON{appToken}
	- returns JSON[{thumbnail, text}] //JSON array

/store method=post
	- requires JSON{appToken, [{thumbnail}], [{text}]} //appToken, array(thumbnail), array(text)
	- returns JSON[{thumbnail, text}] //JSON array

As of now the supported file formats are jpg, jpeg, png. Tesseract dumps When tried with PDF file, so check on allowed formats is in place. Also quality of extract depends on quality of image.

Tesseract OCR needs to be installed before starting the server.
https://github.com/tesseract-ocr/tesseract/wiki

For thumbnails, Imagemagick needs to be installed as well.
