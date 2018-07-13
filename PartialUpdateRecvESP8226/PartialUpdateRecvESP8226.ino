// PartialUpdateExample : example for Waveshare 1.54", 2.31" and 2.9" e-Paper and the same e-papers from Dalian Good Display Inc.
//
// Created by Jean-Marc Zingg based on demo code from Good Display for GDEP015OC1.
//
// The e-paper displays are available from:
//
// https://www.aliexpress.com/store/product/Wholesale-1-54inch-E-Ink-display-module-with-embedded-controller-200x200-Communicate-via-SPI-interface-Supports/216233_32824535312.html
//
// http://www.buy-lcd.com/index.php?route=product/product&path=2897_8363&product_id=35120
// or https://www.aliexpress.com/store/product/E001-1-54-inch-partial-refresh-Small-size-dot-matrix-e-paper-display/600281_32815089163.html
//

// Supporting Arduino Forum Topics:
// Waveshare e-paper displays with SPI: http://forum.arduino.cc/index.php?topic=487007.0
// Good Dispay ePaper for Arduino : https://forum.arduino.cc/index.php?topic=436411.0

// mapping suggestion from Waveshare 2.9inch e-Paper to Wemos D1 mini
// BUSY -> D2, RST -> D4, DC -> D3, CS -> D8, CLK -> D5, DIN -> D7, GND -> GND, 3.3V -> 3.3V

// mapping suggestion from Waveshare 2.9inch e-Paper to generic ESP8266
// BUSY -> GPIO4, RST -> GPIO2, DC -> GPIO0, CS -> GPIO15, CLK -> GPIO14, DIN -> GPIO13, GND -> GND, 3.3V -> 3.3V

// mapping suggestion for ESP32, e.g. LOLIN32, see .../variants/.../pins_arduino.h for your board
// NOTE: there are variants with different pins for SPI ! CHECK SPI PINS OF YOUR BOARD
// BUSY -> 4, RST -> 16, DC -> 17, CS -> SS(5), CLK -> SCK(18), DIN -> MOSI(23), GND -> GND, 3.3V -> 3.3V

// mapping suggestion for AVR, UNO, NANO etc.
// BUSY -> 7, RST -> 9, DC -> 8, CS-> 10, CLK -> 13, DIN -> 11

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

// generic/common.h
//static const uint8_t SS    = 15; // D8
//static const uint8_t MOSI  = 13; // D7
//static const uint8_t MISO  = 12; // D6
//static const uint8_t SCK   = 14; // D5

GxIO_Class io(SPI, /*CS=D8*/ SS, /*DC=D3*/ 0, /*RST=D4*/ 2); // arbitrary selection of D3(=0), D4(=2), selected for default of GxEPD_Class
GxEPD_Class display(io /*RST=D4*/ /*BUSY=D2*/); // default selection of D4(=2), D2(=4)

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

double speedSlowest = 18.4;
double speedOwn = 20.4;

void setup(void)
{
  Serial.begin(115200);
  Serial.println();
  Serial.println("setup");
  //display.init(115200); // enable diagnostic output on Serial
  display.init(); // disable diagnostic output on Serial
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

  // use GPIO pin 5
  mySwitch.enableReceive(5);  // Receiver on inerrupt 0 => that is pin #2

  showPartialUpdateSlowest();
  showPartialUpdateSpeed();
  showPartialUpdate_AVRDebug();
  showPartialUpdateSlower();

  // D0 on Heltec WiFi Kit 8
  pinMode(16, OUTPUT);

  
  
}

void loop()
{
  
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

      speedOwn = ((double) random(150,280)) / 10;
      speedSlowest = ((double) random(110,220)) / 10;

      showPartialUpdateSlowest();
      showPartialUpdateSpeed();
      showPartialUpdate_AVRDebug();
      showPartialUpdateSlower();

    }
    mySwitch.resetAvailable();
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
  uint16_t box_y = 110;
  uint16_t box_w = 110;
  uint16_t box_h = 70;
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
  //display.print("3 Members");

}

void drawCallbackSlower()
{
  uint16_t box_x = 80;
  uint16_t box_y = 60;
  uint16_t box_w = 110;
  uint16_t box_h = 40;
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
    digitalWrite(16, HIGH);
    delay(200);     
    digitalWrite(16, LOW); 
      
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
  uint16_t box_y = 110;
  uint16_t box_w = 110;
  uint16_t box_h = 70;
  uint16_t cursor_y = box_y + 14;
  display.drawPagedToWindow(drawCallbackSpeed, box_x, box_y, box_w, box_h);
}

void showPartialUpdateSlower()
{
  uint16_t box_x = 80;
  uint16_t box_y = 60;
  uint16_t box_w = 110;
  uint16_t box_h = 40;
  uint16_t cursor_y = box_y + 14;
  display.drawPagedToWindow(drawCallbackSlower, box_x, box_y, box_w, box_h);
}

void showPartialUpdate_AVRDebug()
{
  uint16_t box_x = 80;
  uint16_t box_y = 180;
  uint16_t box_w = 110;
  uint16_t box_h = 15;
  uint16_t cursor_y = box_y + 14;
  display.drawPagedToWindow(drawCallbackDebug, box_x, box_y, box_w, box_h);
}
