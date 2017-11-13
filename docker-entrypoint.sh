#!/bin/sh
set -x

if [ $dbPassword ]; then
    MYSQL_PASSWORD_VAL=$dbPassword
elif [ $dbPassword_file ]; then
    MYSQL_PASSWORD_VAL=`cat $dbPassword_file`
fi

if [ $oauthClientSecret ]; then
    OAUTH_CLIENT_SECRET_VAL=$oauthClientSecret
elif [ $oauthClientSecret_file ]; then
    OAUTH_CLIENT_SECRET_VAL=`cat $oauthClientSecret_file`
fi

keystorePassword=`cat $keystore_password_path`

openssl pkcs12 -export -in $token_cert_path -inkey $token_key_path -name $keystoreOAuthKey -out oauth.p12 -password pass:$keystorePassword
openssl pkcs12 -export -in $ssl_cert_path -inkey $ssl_key_path -name tomcat -out tomcat.p12 -password pass:$keystorePassword


keytool -importcert -file $auth_cert_path -keystore ssl_trust_store.jks -storepass $keystorePassword -alias auth.nwis.usgs.gov -noprompt
keytool -v -importkeystore -deststorepass $keystorePassword -destkeystore ssl_trust_store.jks -deststoretype JKS -srckeystore oauth.p12 -srcstorepass $keystorePassword -srcstoretype PKCS12 -noprompt
keytool -v -importkeystore -deststorepass $keystorePassword -destkeystore ssl_trust_store.jks -deststoretype JKS -srckeystore tomcat.p12 -srcstorepass $keystorePassword -srcstoretype PKCS12 -noprompt

keytool -list -keystore ssl_trust_store.jks -storepass $keystorePassword

java -Djava.security.egd=file:/dev/./urandom -DdbPassword=$MYSQL_PASSWORD_VAL -DoauthClientSecret=$OAUTH_CLIENT_SECRET_VAL -DkeystoreLocation=ssl_trust_store.jks DkeystorePassword=$keystorePassword -Djavax.net.ssl.trustStore=ssl_trust_store.jks -Djavax.net.ssl.trustStorePassword=$keystorePassword -jar app.jar

exec $?
