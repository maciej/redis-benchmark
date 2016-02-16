#!/bin/bash

echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
sudo apt-get update -y
sudo apt-get install -y openjdk-8-jdk sbt vim redis-server screen
redis-server &
git clone https://github.com/maciej/redis-benchmark.git
cd redis-benchmark
sbt
#sbt jmh:runMain me.maciejb.redisbench.ParallelRedisBenchmarkApp
