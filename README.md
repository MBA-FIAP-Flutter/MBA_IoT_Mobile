# MBA_IoT_Mobile
# Aula de IoT com foco em Mobile

# Código Node JS ->>>

var five = require("johnny-five");
var board = new five.Board();

const PubNub = require('pubnub');
const pubnub = new PubNub({
    publishKey: "pub-c-2c9d5de0-71d1-4169-b64b-4f158b701211",
    subscribeKey: "sub-c-5c029dc6-1347-11eb-ae19-92aa6521e721",
    uuid: "mbaFiapMobile",  
});

var firebase = require("firebase/app");
require("firebase/database");
var guardian = false;

var firebaseConfig = {
    apiKey: "AIzaSyB8DVvY7_gnxcIoybwNUroRSdUGAjytcNk",
    authDomain: "mba-fiap-79e61.firebaseapp.com",
    databaseURL: "https://mba-fiap-79e61.firebaseio.com",
    projectId: "mba-fiap-79e61",
    storageBucket: "mba-fiap-79e61.appspot.com",
    messagingSenderId: "299898544068",
    appId: "1:299898544068:web:b0adbd1077351d23a50e91"
};

firebase.initializeApp(firebaseConfig);
var database = firebase.database();

board.on("ready", function() {
    var led = new five.Led(13);
    var button = new five.Button(2);

    button.on("press", function() {
        console.log("press - alguém abriu a porta");
        if (guardian){
            led.blink(1500);
        }
    });

    button.on("release", function() {
        console.log("release - alguém fechou a porta");
    });

    database.ref("guardian").on('value', function(snapshot) {
        guardian = snapshot.val();

        if (!guardian){
            led.stop().off();
        }
    });


    pubnub.addListener({
        status: function(statusEvent) {},
        message: function(messageEvent) {
            if (messageEvent.message.action == 1){
                led.off();
            } else {
                led.on();
            }
        },
        presence: function(presenceEvent) {}
    })
    pubnub.subscribe({ channels: ["mbaFiapMobile"], });

});
