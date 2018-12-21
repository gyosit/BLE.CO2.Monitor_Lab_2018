/*
 * Copyright (c) 2018 JKsoft
 * Released under the MIT license
 * https://github.com/junichikatsu/CDM7160/blob/master/library.properties
 */

/*
    Video: https://www.youtube.com/watch?v=oCMOYS71NIU
    Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleNotify.cpp
    Ported to Arduino ESP32 by Evandro Copercini

   Create a BLE server that, once we receive a connection, will send periodic notifications.
   The service advertises itself as: 4fafc201-1fb5-459e-8fcc-c5c9c331914b
   And has a characteristic of: beb5483e-36e1-4688-b7f5-ea07361b26a8

   The design of creating the BLE server is:
   1. Create a BLE Server
   2. Create a BLE Service
   3. Create a BLE Characteristic on the Service
   4. Create a BLE Descriptor on the characteristic
   5. Start the service.
   6. Start advertising.

   A connect hander associated with the server starts a background task that performs notification
   every couple of seconds.
*/

// PIN:12,13,14,15,16,17,PIN_N,19,21,22,23,25,26,27,32,33,34,35

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Wire.h>
//#include <WioLTEforArduino.h>
#include <CDM7160.h>
#define PIN_N 1

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;
uint8_t value = 0;
double Duty;
double partDuty;
volatile float UpNew[PIN_N];
volatile float UpOld[PIN_N];
volatile float DownNew[PIN_N];
volatile float DownOld[PIN_N];
bool flag = false; //F:LOW T:HIGH
double posi_count = 0;
double nega_count = 0;
int inpins[] = {35};

/* CDM7160 */
CDM7160 co2;

// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914c"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a9"

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
    };

    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
    }
};

void getDuty();

void setup() {
  Serial.begin(115200);
  Serial.println("START");

  // Create the BLE Device
  BLEDevice::init("MyESP32C9");

  // Create the BLE Server
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  // Create the BLE Service
  BLEService *pService = pServer->createService(SERVICE_UUID);

  // Create a BLE Characteristic
  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ   |
                      BLECharacteristic::PROPERTY_WRITE  |
                      BLECharacteristic::PROPERTY_NOTIFY |
                      BLECharacteristic::PROPERTY_INDICATE
                    );

  // https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
  // Create a BLE Descriptor
  pCharacteristic->addDescriptor(new BLE2902());

  // Start the service
  pService->start();

  // Start advertising
  pServer->getAdvertising()->start();
  Serial.println("Waiting a client connection to notify...");

  for(int i:inpins)
    pinMode(i, INPUT);
  pinMode(21, INPUT_PULLUP);
  pinMode(22, INPUT_PULLUP);
  //xTaskCreate(getDuty, "getDuty", 1024, NULL, 1, NULL); 

  //pinMode(34, INPUT);

  Wire.begin(21, 22);
  Wire.begin();
  co2.begin();
}

void loop() {
    /* I2C */
    Serial.println("SCANNING");
    byte address, error;
    int nDevices = 0;
    for(address = 1; address < 127; address++){
      Wire.beginTransmission(address);
      error = Wire.endTransmission();
      if(error == 0){
        Serial.print("0");
        Serial.print(address,HEX);
        Serial.println("  !");

        nDevices++;
      }
    }
  
    // notify changed value
    //Serial.println(String(Duty[16]));
    getDuty();
    if (deviceConnected) {
        char buffer[200];
        Serial.println(String(Duty));
        sprintf(buffer, "%f", Duty);
        pCharacteristic->setValue(buffer);
        pCharacteristic->notify();
        delay(500); // bluetooth stack will go into congestion, if too many packets are sent
    }
    // disconnecting
    if (!deviceConnected && oldDeviceConnected) {
        delay(500); // give the bluetooth stack the chance to get things ready
        pServer->startAdvertising(); // restart advertising
        Serial.println("start advertising");
        oldDeviceConnected = deviceConnected;
    }
    // connecting
    if (deviceConnected && !oldDeviceConnected) {
        // do stuff here on connecting
        oldDeviceConnected = deviceConnected;
    }
    delay(1000);
}

void getDuty(){
  if(!co2.readData()) return;
  Serial.println(String(co2.getCo2()));
  Duty = co2.getCo2();
}




