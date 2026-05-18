#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>

// WiFi AP credentials
const char* ssid = "ESP8266_LED";
const char* password = "12345678";

// Built-in LED pin (GPIO2 on most ESP8266 boards)
#define LED_PIN 2

ESP8266WebServer server(80);

void handleOn() {
  digitalWrite(LED_PIN, LOW);  // LOW = ON for built-in LED
  server.send(200, "text/plain", "LED ON");
}

void handleOff() {
  digitalWrite(LED_PIN, HIGH); // HIGH = OFF for built-in LED
  server.send(200, "text/plain", "LED OFF");
}

void handleStatus() {
  String state = digitalRead(LED_PIN) == LOW ? "ON" : "OFF";
  server.send(200, "text/plain", state);
}

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, HIGH); // Start with LED off

  // Start Access Point
  WiFi.softAP(ssid, password);
  Serial.println("AP started");
  Serial.print("IP: ");
  Serial.println(WiFi.softAPIP()); // Usually 192.168.4.1

  server.on("/on", handleOn);
  server.on("/off", handleOff);
  server.on("/status", handleStatus);
  server.begin();
  Serial.println("Server started");
}

void loop() {
  server.handleClient();
}