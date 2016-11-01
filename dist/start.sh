#!/bin/bash

#sed -i 's/test_update/update/g' /etc/htCarSensor.conf 
 
#path ot configuration file
Configuration=/etc/htCarSensor.conf


HomePath=/home/CarSensorJLib


#load configuration
source $Configuration







# New version check

$HomePath/version_check.sh &


echo "Sensor Start up!"

java  -Djava.library.path=/usr/lib/jni -jar $HomePath/SensorEventPublisher.jar -s $ConvertorServerName -p $ConvertorServerPort -sp $SensorPort  >& /tmp/carsensor.log&

#java -Djava.library.path=/usr/lib/jni -cp `cat /home/pi/sox/classpath` Main -d FujisawaCarSensor8  &
#>& /tmp/carsensor.log&
#java -cp `cat /home/pi/sox/classpath` Main -d romenTest >& /tmp/carsensor.log&
