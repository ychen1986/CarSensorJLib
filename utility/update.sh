#!/bin/bash


#Path ot configuration file
Configuration=/etc/htCarSensor.conf

# Load the configuration file

HomePath=/home/pi/htCarSensor


source $Configuration

usage()
	{
	echo "usage: update -u, --url	<url from which to downlowd update>"
	exit 0
	}



UPDATE_URL="http://dali.ht.sfc.keio.ac.jp/~yin/htCarSensor/Distribution/CarSensor.gz.tar"

UPDATE_FILE=/tmp/update.gz.tar

TEMP=`getopt -o u: --long url: -n 'update.sh' -- "$@"`

if [ $? != 0 ] ; then usage ; exit 1 ; fi

# Note the quotes around `$TEMP': they are essential!
eval set -- "$TEMP"



while true ; do
	case "$1" in
		-u|--url) UPDATE_URL=$2; shift 2 ;;
		
		--) shift ; break ;;
		*) echo "Internal error!" ; exit 1 ;;
	esac
done

# Dowload the update file
if !(wget --tries=2  --timeout=60   --output-document $UPDATE_FILE   $UPDATE_URL  --append-output=$UpdateLog)
then
   echo echo "Download update file FAIL!" > $UpdateLog
   exit 1
fi


# untar the downloaded file
if tar xzvf  $UPDATE_FILE &> /dev/null; then
   tar xzvf  $UPDATE_FILE --directory $HomePath
   sudo chown -R pi $HomePath
fi

rm $UPDATE_FILE

exit 0


exit 0
