#!/bin/bash
apt-get update -qq
apt-get install -y -qq netcat
{
  echo "--- START NET TEST ---"
  echo "Testing Redis 10.217.135.172:6379"
  nc -vz -w 5 10.217.135.172 6379 && echo "REDIS: OK" || echo "REDIS: FAIL"
  echo "--- END NET TEST ---"
} | tee /var/log/net-test.log
# send to serial console for retrieval
cat /var/log/net-test.log > /dev/ttyS0
