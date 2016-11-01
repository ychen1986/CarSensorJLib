#!/bin/bash

#This script check the newest version of htCarSenor published.
#exit 0 if both the version check and update (if new update available) succeed
#exit 1 if the version check fials
#exit 2 if the update fails

#Path ot configuration file
Configuration=/etc/htCarSensor.conf

# Load the configuration file

HomePath=/home/pi/htCarSensor


source $Configuration

CurrentVesion=$Version

UpdateURL=$UpdateServer

UpdateINFO=/tmp/htCarSenor.update

echo "Check new update information"

if !(wget --tries=10 --timeout=60  --output-document $UpdateINFO   $UpdateURL)
then
   echo "Check update information FAIL!"
   exit 1
fi


vercomp () {
    if [[ $1 == $2 ]]
    then
        return 0
    fi
    local IFS=.
    local i ver1=($1) ver2=($2)
    # fill empty fields in ver1 with zeros
    for ((i=${#ver1[@]}; i<${#ver2[@]}; i++))
    do
        ver1[i]=0
    done
    for ((i=0; i<${#ver1[@]}; i++))
    do
        if [[ -z ${ver2[i]} ]]
        then
            # fill empty fields in ver2 with zeros
            ver2[i]=0
        fi
        if ((10#${ver1[i]} > 10#${ver2[i]}))
        then
            return 1
        fi
        if ((10#${ver1[i]} < 10#${ver2[i]}))
        then
            return 2
        fi
    done
    return 0
}



source $UpdateINFO

UpdateURL=$UPDATE_URL

NewVersion=$CURRENT_VERSION
vercomp $CurrentVesion $NewVersion
tmp=$?


if [[ $tmp == 2 ]]
then
	echo "Current Version:$CurrentVesion" > $UpdateLog 
	echo "Current Version:$CurrentVesion" 
	echo "New Avaliable Version:$NewVersion" 
	echo "New Avaliable Version:$NewVersion" > $UpdateLog
	echo "Update Start ===================================>" 
	echo "Update Start ===================================>" > $UpdateLog
	

	sudo $HomePath/update.sh -u $UpdateURL
	if [[ $? == 0 ]]
	then
		echo "Updating succeed"
		echo "Configuration"
		sed -i 's/Version='$CurrentVesion'/Version='$NewVersion'/' $Configuration
		if [[ RESTART==YES ]]
		then
			echo "Reboot!"
			sudo sync
			sudo reboot
		fi

	else
		echo "Updating fail"
		exit 2
	fi
else
	echo "No available update"
fi

echo "Version check/update finish"



exit 0
