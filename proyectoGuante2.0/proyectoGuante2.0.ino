#include <SoftwareSerial.h>
#include <Wire.h>
// Bluetoorth serial
SoftwareSerial BTSerial(10, 11); // TX=10 | RX=

//Direccion I2C de la IMU
#define MPU 0x68

//Ratios de conversion
#define A_R 16384.0
#define G_R 131.0

//Conversion de radianes a grados 180/PI
#define RAD_A_DEG = 57.295779

//MPU-6050 da los valores en enteros de 16 bits
//Valores sin refinar
int16_t AcX, AcY, AcZ, GyX, GyY, GyZ;
int dataSend[5][2];
//Angulos
float Acc[2];
float Gy[2];
float Angle[2];
int x;
int y;
char strX[5];
char strY[5];
float aux[2][2];
void setup() {
  // put your setup code here, to run once:
  Wire.begin();
  Wire.beginTransmission(MPU);
  Wire.write(0x6B);
  Wire.write(0);
  Wire.endTransmission(true);
  Serial.begin(9600);
  BTSerial.begin(9600);

}
void loop() {

  getData();
  //printData();
  setData();
}

void getData() {
  /*Primero obtenemos los datos del acelerometro*/

  for (int i = 0 ; i < 5; i++) {
    for (int j = 0 ; j < 5 ; j++) {
      dataSend[i][0] += analogRead(i);
    }

    //Leer los valores del Acelerometro de la IMU
    Wire.beginTransmission(MPU);
    Wire.write(0x3B); //Pedir el registro 0x3B - corresponde al AcX
    Wire.endTransmission(false);
    Wire.requestFrom(MPU, 6, true ); //A partir del 0x3B, se piden 6 registros
    AcX = Wire.read() << 8 | Wire.read(); //Cada valor ocupa 2 registros
    AcY = Wire.read() << 8 | Wire.read();
    AcZ = Wire.read() << 8 | Wire.read();

    //A partir de los valores del acelerometro, se calculan los angulos Y, X
    //respectivamente, con la formula de la tangente.
    Acc[1] = atan(-1 * (AcX / A_R) / sqrt(pow((AcY / A_R), 2) + pow((AcZ / A_R), 2))) * RAD_TO_DEG;
    Acc[0] = atan((AcY / A_R) / sqrt(pow((AcX / A_R), 2) + pow((AcZ / A_R), 2))) * RAD_TO_DEG;

    //Leer los valores del Giroscopio
    Wire.beginTransmission(MPU);
    Wire.write(0x43);
    Wire.endTransmission(false);
    Wire.requestFrom(MPU, 4, true); //A diferencia del Acelerometro, solo se piden 4 registros
    GyX = Wire.read() << 8 | Wire.read();
    GyY = Wire.read() << 8 | Wire.read();

    //Calculo del angulo del Giroscopio
    Gy[0] = GyX / G_R;
    Gy[1] = GyY / G_R;

    //Aplicar el Filtro Complementario
    Angle[0] = 0.98 * (Angle[0] + Gy[0] * 0.010) + 0.02 * Acc[0];
    Angle[1] = 0.98 * (Angle[1] + Gy[1] * 0.010) + 0.02 * Acc[1];

    aux[0][0] += Angle[0];
    aux[1][0] += Angle[1];


    dataSend[i][1] = dataSend[i][0] / 5;
    dataSend[i][0] = 0;
  }
  x = aux[0][0] / 5;
  y = aux[1][0] / 5;
  aux[0][0] = 0;
  aux[1][0] = 0;
  /* ahora se obtienes los datos de los sensores el guante*/
  for (int i = 0 ; i < 5 ; i++) {
    dataSend[i][1] = dataSend[i][1] / 10; // map(dataSend[i][1], 0, 100, 0 , 1023);

  }

  sprintf(strX, "%03d", x);
  sprintf(strY, "%03d", y);



}
void setData() {

  for (int i = 0 ; i < 5 ; i++) {
    BTSerial.print(dataSend[i][1]);
  }
  BTSerial.print(strX);
  BTSerial.print(strY);
  BTSerial.print("*");
  delay(10);
}

void printData() {
  //'Mostrar los valores por consola
  for (int i = 0 ; i < 5 ; i++) {

    Serial.print("Val"); Serial.print(i); Serial.print(" : "); Serial.print(dataSend[i][1]); Serial.print("\n");
  }
  Serial.print("Angle X: "); Serial.print(strX); Serial.print(" . ");  Serial.print("Angle Y: "); Serial.print(strY); Serial.print("\n");

  delay(250);

}



