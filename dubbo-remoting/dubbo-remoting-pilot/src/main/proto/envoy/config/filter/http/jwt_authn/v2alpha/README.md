# JWT Authentication HTTP filter config

## Overview

1. The proto file in this folder defines an HTTP filter config for "jwt_authn" filter.

2. This filter will verify the JWT in the HTTP request as:
    - The signature should be valid
    - JWT should not be expired
    - Issuer and audiences are valid and specified in the filter config.

3. [JWK](https://tools.ietf.org/html/rfc7517#appendix-A) is needed to verify JWT signature. It can be fetched from a remote server or read from a local file. If the JWKS is fetched remotely, it will be cached by the filter.

3. If a JWT is valid, the user is authenticated and the request will be forwarded to the backend server. If a JWT is not valid, the request will be rejected with an error message.

## The locations to extract JWT

JWT will be extracted from the HTTP headers or query parameters. The default location is the HTTP header:
```
Authorization: Bearer <token>
```
The next default location is in the query parameter as:
```
?access_token=<TOKEN>
```

If a custom location is desired, `from_headers` or `from_params` can be used to specify custom locations to extract JWT.

## HTTP header to pass successfully verified JWT

If a JWT is valid, its payload will be passed to the backend in a new HTTP header specified in `forward_payload_header` field. Its value is base64 encoded JWT payload in JSON.
