

// the setup routine runs once when you press reset:
void setup() {
  // initialize serial communication at 9600 bits per second:
  Serial.begin(9600);
}

double circumference = 0.00446860139 / 20;
double speedStep = 10.0; //Kmh
double difSpeed = 0.0; //Kmh
double heartRateThreshold = 120.0; //bpm
double heartRate = 0.0; //bpm

//we ne to get the heartrate live monitored
double minhr = 90.0; //bpm
double maxHr = 140.0; //bpm

int startTime = 0;
int endTime = 0;
int lastSample = 0;

int cnt = 0;

// the loop routine runs over and over again forever:
void loop() {
  // read the input on analog pin 0:
  int sensorValue = analogRead(A0);

  heartRate = random(minHr, maxHr)

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

         if (heartRate >= heartRateThreshold){
              //analogWrite();
              digitalWrite(A1, HIGH)//vibromotor-output
              Serial.print("WARNING - HIGH HEARTRATE");
              Serial.print("recommended Speed: ");
              difSpeed = v-speedStep;
              if (difSpeed > 0){
                   Serial.println(difSpeed);
              } else {
                  Serial.println(0.00);
              }
         } else {
             digitalWrite(A1, LOW)//vibromotor-output
         }
        
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
