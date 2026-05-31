#!/usr/bin/env bash
echo "copy build jars to container assets."
mkdir -p captcha/assets/opt
mkdir -p sabi-backend/assets/opt
cp ../../captcha/target/captcha-service.jar captcha/assets/opt
cp ../../sabi-server/target/sabi-service.jar sabi-backend/assets/opt
echo "done"
