#!/bin/bash

mvn clean package 

gnome-terminal -- bash -c "java -jar router/target/router-1.0-SNAPSHOT.jar Router; exec bash" &
sleep 1
gnome-terminal -- bash -c "java -jar market/target/market-1.0-SNAPSHOT.jar  Market; exec bash" &
sleep 1
gnome-terminal -- bash -c "java -jar broker/target/broker-1.0-SNAPSHOT.jar  Broker; exec bash" &
