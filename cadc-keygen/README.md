This is a simple command line tool for generating public-private key pair for use with RsaSignatureGenerator/Verifier.
 
The equivalent keys can also be created with `ssh-keygen`:

```sh
#!/bin/bash

KEY=example

ssh-keygen -b 2048 -t rsa -m pkcs8 -f $KEY
ssh-keygen -e -m pkcs8 -f ${KEY}.pub > ${KEY}-public.key
mv $KEY ${KEY}-private.key
rm ${KEY}.pub
ls -l ${KEY}*
```
