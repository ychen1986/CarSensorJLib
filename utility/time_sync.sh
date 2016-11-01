#!/bin/bash

#sudo service ntp stop
#sudo ntpd -qs ntp.nict.jp
#sudo service ntp start

while true; do
  if sudo  sntp -t 5 -s "ntp.nict.jp" ; then
     echo  "Time sync succeed! "
     exit 0
  else 
     echo  ""
     echo  "Time sync fail!"
     echo  "Retry"
     #echo  "Retry after 10 seconds!"
     #sleep 1
  fi
done


#sudo dpkg-reconfigure ntp
