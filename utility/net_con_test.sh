#!/bin/bash


#  Network inteface test


while !(ifconfig | grep -q -E "wlan0|eth1");do

 echo "Network inteface not available!"
 echo "Retry after 10 seconds!"
 sleep 10

done
# Network interface wlan0 or eth1 exists
echo  "Network inteface OK"


# Network connectivity test
while !(fping google.com | grep -q "alive"); do
 
 echo "Network is not connenected!"
 echo "Retry after 10 seconds!"
 sleep 10
done

echo  "Netowrk connectivity OK"

exit 0
