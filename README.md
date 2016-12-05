# kgserver

KGServer is an ADILOS gatekeeper that can be deployed as a network
service and used to test/demo ADILOS remote login capability and
kgagent.

Agents and servers speak JSON-RPC 1.0
[specfication](http://json-rpc.org/wiki/specification)
with one exception: KG transmits null values as six characters:

~~~~
  "null"
~~~~

instead of four characters

~~~~
  null
~~~~

This is due to a limitation of the JSON library. This limitation
might be removed after the standard JRE includes JSR 353

Note in the description we identify values as "b64 ...". The b64 is
our notation to indicate the content is Base64 and we do not
include the 'b' '6' '4' ':' characters

## Basic Message Format:

Each JSON-RPC message is one line (terminated by '\n') of UTF-8
characters.

## Challenge/Response Message Formats:

Agent begins the conversation by opening a connection and sending
a JSON-RPC command to the server.  The server returns an error
containing a challenge:

~~~~
{ "result": "null",
  "error":  { "code":    1,
              "message": "adilos.challenge",
              "data":    "b64 challenge" },
  "id":     "null" }
~~~~

Agent interacts with keymaster via QR code exchange, then forms a valid
ADILOS response and sends:

~~~~
{ "method": "adilos.response",
  "params": ["b64:responsetxt"],
  "id":     client.cookie | "null" }
~~~~

The id parameter is optional. kgserver will return the same
value in future responses. Client may update this cookie in any
request - the server always returns the last-specified value.

Once the server receives the adilos.reponse, verifies it and adds the key
to the sessions table it returns a response containing the default/root
resource.

## Test Mode:

KGServer can operate in test mode in which any new public key can
be automatically registered. USE WITH EXTREME CAUTION, NOT IN PRODUCTION.

~~~~
{ "method": "test.register",
  "params": ["b64 publickey"],
  "id":     "null" }
~~~~

## Steady-State Message Formats:

Thereafter, requests take the form:

~~~~
{ "method": "request",
  "params": [{"req": "b64 data",
              "sig": "b64 ECDSA(req,a)"}],
  "id:":    client.cookie | "null" }
~~~~

Note that signatures are calculated and verified with the raw bytes, not the
Base64 value.

Responses take the form:

~~~~
{ "result": { "rsp": "b64 data",
              "sig": "b64 ECDSA(rsp,g)" },
  "error":  null,
  "id":     client.cookie | "null" }
~~~~

## KGServer Functions

The default implementation maintains a subdirectory of files. Agents may
request resources in the "req" field and expect a html file in the "rsP".
If the resource is not found an error is returned but the connection is
not dropped.

## Dependencies:

** Java **
- developed on Oracle JDK 1.8 on Ubuntu64

** libsecp256k1 **
- github.com/bitcoin-core/secp256k1

** Google's ZXing library **
- github.com/zxing/zxing

** json-simple library **
- For generating and processing JSON
- github.com/fangyidong/json-simple

** Hypersonic SQL Database **
- filedb for maintaining the Access Control List (ACL)
- http://hsqldb.org
