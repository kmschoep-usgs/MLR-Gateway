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

if [ $sslStorePassword ]; then
    SSL_TRUST_STORE_PASSWORD=$sslStorePassword
elif [ $sslStorePassword_file ]; then 
    SSL_TRUST_STORE_PASSWORD=`cat $sslStorePassword_file`
fi

keytool -importcert -file $water_auth_cert_file -keystore ssl_trust_store.jks -storepass $SSL_TRUST_STORE_PASSWORD -alias auth.nwis.usgs.gov -noprompt

java -Djava.security.egd=file:/dev/./urandom -DdbPassword=$MYSQL_PASSWORD_VAL -DoauthClientSecret=$OAUTH_CLIENT_SECRET_VAL -Djavax.net.ssl.trustStore=ssl_trust_store.jks -Djavax.net.ssl.trustStorePassword=$SSL_TRUST_STORE_PASSWORD -jar app.jar

exec $?
