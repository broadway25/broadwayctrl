# broadwayctrl


RXTX Library on Rasp PI :  sudo apt-get install librxtx-java

sudo apt install openjdk-9-jdk

To find the port :  cat /proc/tty/drivers



 java -Djava.library.path=/usr/lib/jni -cp ./conf:broadwayctrl-assembly-1.0.jar com.gounder.ervice.akka.rest.Rest





sudo rm /var/lock/LCK..ttyUSB0;cu -l /dev/ttyUSB0 -s 115200

# rc.local

java -Djava.library.path=/usr/lib/jni -cp /opt/gounder/conf/:/opt/gounder/apps/broadwayctrl-assembly-1.0.jar com.gounder.mediaplayer.htd.rest.MediaPlayerService 2>&1 >> /opt/gounder/logs/boradway-ctrl.log &

java -cp /opt/gounder/conf/:/opt/gounder/apps/kasa-akka-rest-api-assembly-1.0.jar com.gounder.smartcity.device.control.kasa.rest.KasaControlService 2>&1 >> /opt/gounder/logs/kasa-akka-api.log &
