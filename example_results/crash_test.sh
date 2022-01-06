# Crash test
cp ../target/bully-1.0-SNAPSHOT-jar-with-dependencies.jar ./bully.jar
./clean.sh
java -jar bully.jar 6 9005 Normal 3000 config1.txt &
java -jar bully.jar 5 9004 Crash 3000 config1.txt &
java -jar bully.jar 4 9003 Normal 3000 config1.txt &
java -jar bully.jar 3 9002 Normal 3000 config1.txt Initiator &
java -jar bully.jar 2 9001 Normal 3000 config1.txt &
java -jar bully.jar 1 9000 Normal 3000 config1.txt &
sleep 10
cat Node_*.txt >> crash_results.txt
