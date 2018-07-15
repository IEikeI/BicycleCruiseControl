// include library, include base class, make path known
#include <GxEPD.h>

// select the display class to use, only one
#include <GxGDEP015OC1/GxGDEP015OC1.cpp>

#include <GxIO/GxIO_SPI/GxIO_SPI.cpp>
#include <GxIO/GxIO.cpp>

// FreeFonts from Adafruit_GFX
#include <Fonts/FreeMonoBold9pt7b.h>
#include <Fonts/FreeMonoBold12pt7b.h>

//#include GxEPD_BitmapExamples

static const uint8_t RST   = 0; // D5
static const uint8_t BUSY   = 1; // D5

GxIO_Class io(SPI, /*CS=D8*/ SS, /*DC=D3*/ 13, /*RST=D4*/ RST); // arbitrary selection of D3(=0), D4(=2), selected for default of GxEPD_Class
GxEPD_Class display(io, RST, BUSY); // default selection of D4(=2), D2(=4)

#if defined(_GxGDEP015OC1_H_)
const uint32_t partial_update_period_s = 1;
const uint32_t full_update_period_s = 6 * 60 * 60;
#elif defined(_GxGDE0213B1_H_) || defined(_GxGDEH029A1_H_) || defined(_GxGDEW042T2_H_)
const uint32_t partial_update_period_s = 2;
const uint32_t full_update_period_s = 1 * 60 * 60;
#endif

#include "BitmapGraphics.h"

#include <RCSwitch.h>

RCSwitch mySwitch = RCSwitch();

// heart sensor

#include <Wire.h>
#include "MAX30105.h"
  
#include "heartRate.h"
  
MAX30105 particleSensor;
  
const byte RATE_SIZE = 4; //Increase this for more averaging. 4 is good.
byte rates[RATE_SIZE]; //Array of heart rates
byte rateSpot = 0;
long lastBeat = 0; //Time at which the last beat occurred
  
float beatsPerMinute;
int beatAvg;

int pinVibro = 10;

//measure speed
double speedSlowest = 18.4;
double speedOwn = 20.5;

int speedSensorPin = A0; 
double circumference = 0.00446860139 / 20;

int startTime = 0;
int endTime = 0;
int lastSample = 0;

int cnt = 0;

int cntForSendingSpeed = 0;

void setup() {
  
  pinMode(pinVibro, OUTPUT);
  digitalWrite(pinVibro, LOW); 
  Serial.begin(115200);
  Serial.println();
  Serial.println("setup");
  Serial.println(SS);

  display.init(115200); // enable diagnostic output on Serial
  //display.init(); // disable diagnostic output on Serial
  Serial.println("setup done");
  display.setTextColor(GxEPD_BLACK);
  display.setRotation(0);
  // draw background


  // cope with code size limitation
  display.drawExampleBitmap(BitmapExample1, sizeof(BitmapExample1));
  display.setFont(&FreeMonoBold9pt7b);

  // partial update to full screen to preset for partial update of box window
  // (this avoids strange background effects)
  display.drawExampleBitmap(BitmapExample1, sizeof(BitmapExample1), GxEPD::bm_default | GxEPD::bm_partial_update);
  display.setRotation(1);

  // use GPIO pin 12
  mySwitch.enableReceive(12);  // Receiver on inerrupt 0 => that is pin #2

  // Transmitter is connected to Arduino Pin
  mySwitch.enableTransmit(11);

  showPartialUpdateSlowest();
  showPartialUpdateSpeed();
  showPartialUpdatePulse();
  showPartialUpdateDebug();
  showPartialUpdateSlower();
 
   // Initialize sensor
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) //Use default I2C port, 400kHz speed
  {
    Serial.println("MAX30105 was not found. Please check wiring/power. ");
    while (1);
  }
  Serial.println("Place your index finger on the sensor with steady pressure.");

  particleSensor.setup(); //Configure sensor with default settings
  particleSensor.setPulseAmplitudeRed(0x0A); //Turn Red LED to low to indicate sensor is running
  particleSensor.setPulseAmplitudeGreen(0); //Turn off Green LED
  


}

void loop() {
  //showPartialUpdate_AVRDebug();
  if (mySwitch.available()) {
    
    int value = mySwitch.getReceivedValue();
    
    if (value == 0) {
      Serial.print("Unknown encoding");
    } else {
      Serial.print("Received ");
      Serial.print( mySwitch.getReceivedValue() );
      Serial.print(" / ");
      Serial.print( mySwitch.getReceivedBitlength() );
      Serial.print("bit ");
      Serial.print("Protocol: ");
      Serial.println( mySwitch.getReceivedProtocol() );
      Serial.println ( (long) mySwitch.getReceivedRawdata() );

      //speedOwn = ((double) random(150,280)) / 10;
      //speedSlowest = ((double) random(110,220)) / 10;

      int rec = mySwitch.getReceivedValue();
      // make received int to string
      String recStr = String(rec);
      // use only first 3 chars to get an 3-digit int
      rec = recStr.substring(0,3).toInt();

      speedSlowest = rec;
      speedSlowest = speedSlowest / 10;

      showPartialUpdateSlowest();
      showPartialUpdateSpeed();
      showPartialUpdateDebug();
      showPartialUpdateSlower();

    }

    
    mySwitch.resetAvailable();
  }

  
  long irValue = particleSensor.getIR();

  if (checkForBeat(irValue) == true)
  {
    //We sensed a beat!
    long delta = millis() - lastBeat;
    lastBeat = millis();

    beatsPerMinute = 60 / (delta / 1000.0);

    if (beatsPerMinute < 255 && beatsPerMinute > 20)
    {
      rates[rateSpot++] = (byte)beatsPerMinute; //Store this reading in the array
      rateSpot %= RATE_SIZE; //Wrap variable

      //Take average of readings
      beatAvg = 0;
      for (byte x = 0 ; x < RATE_SIZE ; x++)
        beatAvg += rates[x];
      beatAvg /= RATE_SIZE;

      showPartialUpdatePulse();
    }

  }

  Serial.print("IR=");
  Serial.print(irValue);
  Serial.print(", BPM=");
  Serial.print(beatsPerMinute);
  Serial.print(", Avg BPM=");
  Serial.print(beatAvg);

  if (irValue < 50000)
    Serial.print(" No finger?");

  Serial.println();
  

    // read the input on analog pin 0:
  int sensorValue = analogRead(speedSensorPin);

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
        speedOwn = v;
        Serial.print("V: ");
        Serial.println(v);
        startTime = 0;
        endTime = 0;

        showPartialUpdateSpeed();
        showPartialUpdateSlower();
        
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

  if(cntForSendingSpeed++ > 100){
    Serial.println("Sending on 433mhz my speedOwn:");
    Serial.println(speedOwn);
    mySwitch.send((speedOwn*10), 24);
    cntForSendingSpeed = 0;
  }

}

void drawCallbackSlowest()
{
  uint16_t box_x = 10;
  uint16_t box_y = 0;
  uint16_t box_w = 190;
  uint16_t box_h = 20;
  uint16_t cursor_y = box_y + 16;
  display.fillRect(box_x, box_y, box_w, box_h, GxEPD_WHITE);
  display.setCursor(box_x, cursor_y);
  display.print("Slowest: ");
  display.print(String(speedSlowest, 1));
  display.println("km/h");
}

void drawCallbackSpeed()
{
  uint16_t box_x = 80;
  uint16_t box_y = 80;
  uint16_t box_w = 110;
  uint16_t box_h = 40;
  uint16_t cursor_y = box_y + 16;
  display.fillRect(box_x, box_y, box_w, box_h, GxEPD_WHITE);
  display.setCursor(box_x, cursor_y);
  display.print("Speed: \n");
  display.setCursor(box_x, cursor_y +22);
  display.setFont(&FreeMonoBold12pt7b);
  
  display.print(String(speedOwn, 1));
  display.print("km/h \n");
  display.setFont(&FreeMonoBold9pt7b);
  display.setCursor(box_x, cursor_y +47);
  //display.print(beatAvg);

}

void drawCallbackPulse()
{
  uint16_t box_x = 80;
  uint16_t box_y = 130;
  uint16_t box_w = 110;
  uint16_t box_h = 30;
  uint16_t cursor_y = box_y + 16;
  display.fillRect(box_x, box_y, box_w, box_h, GxEPD_WHITE);
  display.setCursor(box_x, cursor_y);
  display.print("Pulse: \n");
  display.setCursor(box_x, cursor_y +16);
  //display.setFont(&FreeMonoBold12pt7b);
  
  display.print(beatAvg);
  display.print("bpm \n");
  //display.setFont(&FreeMonoBold9pt7b);
  //display.setCursor(box_x, cursor_y +47);
  //display.print(beatAvg);

}

void drawCallbackSlower()
{
  uint16_t box_x = 80;
  uint16_t box_y = 50;
  uint16_t box_w = 110;
  uint16_t box_h = 20;
  uint16_t cursor_y = box_y + 16;
  display.fillRect(box_x, box_y, box_w, box_h, GxEPD_WHITE);
  display.setCursor(box_x, cursor_y);

  Serial.print("speedSlowest: ");
  Serial.println(speedSlowest);
  Serial.print("speedOwn: ");
  Serial.println(speedOwn);

  if((speedSlowest + 1) < speedOwn){
    display.print("! Slower !\n");

    // turn on vibro
    digitalWrite(pinVibro, HIGH);
    delay(50);
    digitalWrite(pinVibro, LOW); 
    delay(50); 
    digitalWrite(pinVibro, HIGH);    
    delay(80);
    digitalWrite(pinVibro, LOW); 
      
  }else{
    display.print("\n");
   }
  
}

void drawCallbackDebug()
{
  uint16_t box_x = 80;
  uint16_t box_y = 180;
  uint16_t box_w = 110;
  uint16_t box_h = 15;
  uint16_t cursor_y = box_y + 16;
  display.fillRect(box_x, box_y, box_w, box_h, GxEPD_WHITE);
  display.setCursor(box_x, cursor_y);
  display.print(mySwitch.getReceivedValue());
}

void showPartialUpdateSlowest()
{
  uint16_t box_x = 10;
  uint16_t box_y = 0;
  uint16_t box_w = 190;
  uint16_t box_h = 20;
  uint16_t cursor_y = box_y + 14;
  display.drawPagedToWindow(drawCallbackSlowest, box_x, box_y, box_w, box_h);
}

void showPartialUpdateSpeed()
{
  uint16_t box_x = 80;
  uint16_t box_y = 80;
  uint16_t box_w = 110;
  uint16_t box_h = 40;
  uint16_t cursor_y = box_y + 14;
  display.drawPagedToWindow(drawCallbackSpeed, box_x, box_y, box_w, box_h);
}

void showPartialUpdatePulse()
{
  uint16_t box_x = 80;
  uint16_t box_y = 130;
  uint16_t box_w = 110;
  uint16_t box_h = 30;
  uint16_t cursor_y = box_y + 14;
  display.drawPagedToWindow(drawCallbackPulse, box_x, box_y, box_w, box_h);
}

void showPartialUpdateSlower()
{
  uint16_t box_x = 80;
  uint16_t box_y = 50;
  uint16_t box_w = 110;
  uint16_t box_h = 20;
  uint16_t cursor_y = box_y + 14;
  display.drawPagedToWindow(drawCallbackSlower, box_x, box_y, box_w, box_h);
}

void showPartialUpdateDebug()
{
  uint16_t box_x = 80;
  uint16_t box_y = 180;
  uint16_t box_w = 110;
  uint16_t box_h = 15;
  uint16_t cursor_y = box_y + 14;
  display.drawPagedToWindow(drawCallbackDebug, box_x, box_y, box_w, box_h);
}

