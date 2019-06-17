#!/bin/bash

# export passwords before running gradlew -- source this before running ./gradlew

echo -n "keystore password: "
read keystore_password
export keystore_password

echo -n "key password: "
read key_abdocore_password
export key_abdocore_password
