# IdentifyBookApp

An app which 1) finds identifies a book using the camera or NFC and 2) allows the creation of book lists/displays. The project is based on my Bachelor's Thesis.


Books are identified by finding their ISBN. There are 4 ways implemented (so far):

- Barcode Scanning (mainly EAN_13 and CODE_39)
- RFID chip scanning (with NFC)
- OCR
- Reverse Image Search

The last 2 methods are less reliable but provide more flexibility when the other methods do not work.


Through book scanning with this app on a larger scale, one may accomplish 2 things: 

1. Store these interactions for future data analysis. This is accomplished by sending data to a server with each successful interaction. The data comprises of details such as book ISBN, location, android/phone details, scanning type, and time.
2. Create lists/displays for better book management. The book lists are stored in a DB, both locally and on a server, with an append-only scheme which allows easy querying.