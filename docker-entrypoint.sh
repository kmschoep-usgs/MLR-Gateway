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

java -Djava.security.egd=file:/dev/./urandom -DdbPassword=$MYSQL_PASSWORD_VAL -DoauthClientSecret=$OAUTH_CLIENT_SECRET_VAL -jar app.jar

exec $?
