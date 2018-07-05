/*
  AnalogReadSerial

  Reads an analog input on pin 0, prints the result to the Serial Monitor.
  Graphical representation is available using Serial Plotter (Tools > Serial Plotter menu).
  Attach the center pin of a potentiometer to pin A0, and the outside pins to +5V and ground.

  This example code is in the public domain.

  http://www.arduino.cc/en/Tutorial/AnalogReadSerial
*/

// the setup routine runs once when you press reset:
void setup() {
  // initialize serial communication at 9600 bits per second:
  Serial.begin(9600);
}

double circumference = 0.00446860139 / 20;

int startTime = 0;
int endTime = 0;
int lastSample = 0;

int cnt = 0;

// the loop routine runs over and over again forever:
void loop() {
  // read the input on analog pin 0:
  int sensorValue = analogRead(A0);

  if (sensorValue >= 1020) {
    if (abs(sensorValue - lastSample) >= 1000) {
      
      if (startTime != 0) {
        Serial.print("LAST: ");
        Serial.print(lastSample);
        Serial.print(", CURRENT: ");
        Serial.println(sensorValue);
        endTime = millis();
        int diff = endTime - startTime;
        Serial.print("DIFF: ");
        Serial.println(diff);
        double hours = (diff * 1.0) / 3600000;
        Serial.print("HOURS: ");
        Serial.println(hours);
        double km = circumference;
        Serial.print("KM: ");
        Serial.println(km);
        double v = km / hours;
        Serial.print("V: ");
        Serial.println(v);
        startTime = 0;
        endTime = 0;
      } else {
        startTime = millis();
      }
    }
  }
  if (sensorValue > 55500) {
    if (abs(sensorValue - lastSample) >= 500) {
      cnt++;
      Serial.println(cnt);
    }
  }
  
  lastSample = sensorValue;
  // print out the value you read:
  //Serial.println(sensorValue);
  if (sensorValue >= 0) {
    //Serial.println(sensorValue);
  }
  delay(1);        // delay in between reads for stability
}
