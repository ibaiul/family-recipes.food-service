#!/bin/bash

set -e

cd docker/zookeeper

pass="123456"

echo "Generating server keystore ..."
keytool -genkey -alias server -keyalg RSA -keystore server-keystore.jks -keysize 4096 -dname "C=XX, ST=Bizkaia, L=Bilbo, O=O, OU=OU, CN=server" -storepass "${pass}"
echo "Generating self signed root CA cert ..."
openssl req -newkey rsa:4096 -x509 -keyout ca.key -out ca.crt -days 3650 -subj "/C=XX/ST=Bizkaia/L=Bilbo/O=O/OU=OU/CN=ca/emailAddress=ca@example.com" -passout "pass:${pass}"
echo "Generating server csr ..."
keytool -keystore server-keystore.jks -alias server -certreq -file server.csr -storepass "${pass}"
echo "Signing server cert ..."
openssl x509 -req -CA ca.crt -CAkey ca.key -in server.csr -out server.key -days 3650 -CAcreateserial --passin "pass:${pass}"
echo "Importing root cert to server keystore ..."
keytool -keystore server-keystore.jks -alias ca -import -file ca.crt -storepass "${pass}" -noprompt
echo "Importing server cert to server keystore ..."
keytool -keystore server-keystore.jks -alias server -import -file server.key -storepass "${pass}" -noprompt
echo "Generating server truststore ..."
keytool -keystore server-truststore.jks -alias server -import -file ca.crt -storepass "${pass}" -noprompt
echo "Generating client keystore ..."
keytool -genkey -alias client -keyalg RSA -keystore client-keystore.jks -keysize 4096 -dname "C=XX, ST=Bizkaia, L=Bilbo, O=O, OU=OU, CN=client" -storepass "${pass}"
echo "Generating client csr ..."
keytool -keystore client-keystore.jks -alias client -certreq -file client.csr -storepass "${pass}"
echo "Signing client cert ..."
openssl x509 -req -CA ca.crt -CAkey ca.key -in client.csr -out client.key -days 3650 -CAcreateserial --passin "pass:${pass}"
echo "Importing root cert to client keystore ..."
keytool -keystore client-keystore.jks -alias ca -import -file ca.crt -storepass "${pass}" -noprompt
echo "Importing client cert to client keystore ..."
keytool -keystore client-keystore.jks -alias client -import -file client.key -storepass "${pass}" -noprompt
echo "Generating client truststore ..."
keytool -keystore client-truststore.jks -alias client -import -file ca.crt -storepass "${pass}" -noprompt

# trap 'rm -f ca.srl server.csr' EXIT
