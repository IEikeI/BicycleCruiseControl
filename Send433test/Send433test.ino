/*
  Example for different sending methods
  
  http://code.google.com/p/rc-switch/
  
*/

#include <RCSwitch.h>

RCSwitch mySwitch = RCSwitch();

void setup() {

  Serial.begin(9600);
  
  // Transmitter is connected to Arduino Pin #10  
  mySwitch.enableTransmit(10);

  // Optional set pulse length.
  // mySwitch.setPulseLength(320);
  
  // Optional set protocol (default is 1, will work for most outlets)
  // mySwitch.setProtocol(2);
  
  // Optional set number of transmission repetitions.
  // mySwitch.setRepeatTransmit(15);

  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
  
}

void loop() {

  /* Same switch as above, but using decimal code */
  mySwitch.send(12345, 24);
  digitalWrite(LED_BUILTIN, HIGH);  
  delay(50);
  digitalWrite(LED_BUILTIN, LOW); 
  
  delay(2000);  
  
  mySwitch.send(110, 24);
  digitalWrite(LED_BUILTIN, HIGH);  
  delay(50);
  digitalWrite(LED_BUILTIN, LOW); 
  
  delay(2000);  
  
  mySwitch.send(975, 24);
  digitalWrite(LED_BUILTIN, HIGH);  
  delay(50);
  digitalWrite(LED_BUILTIN, LOW); 
  
  delay(2000);  

}
