#!/bin/bash
if (lsusb | grep -qi "12d1:1f01"); then 
echo "Unbind usb controller!"
echo -n  "1-1" > /sys/bus/usb/drivers/usb/unbind
sleep  5
echo "Rebind usb controller!"
echo -n  "1-1" > /sys/bus/usb/drivers/usb/bind
sleep  10
fi
exit 0
