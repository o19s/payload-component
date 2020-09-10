To generate a signature for new packages run the following command:
_Note:_ This requires access to the private key.

openssl dgst -sha1 -sign privatekey.pem payload-component-[VERSION].jar | openssl enc -base64 | tr -d \\n | sed
