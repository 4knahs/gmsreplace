gmsreplace
==========
Since there are a considerable number of devices with no Google Play Market and Google Play Services, this project presents a workaround for enabling Google Play Service dependent applications. 

This project consists of a prototype xposed module to replace Google Play Services by the correspondent web apis. 

Currently supports google drive APIs present in both the Google Android Drive API demo applications:
* Android-quickstart - uses the camera to take a picture and uploads the picture on selected Drive folder.
* Android-quickeditor - a text editor that allows you to create, open and modify text files in your Drive.
