ps -ef | grep '/usr/bin/java -jar bully.jar' |awk '{print $2}' | xargs kill
