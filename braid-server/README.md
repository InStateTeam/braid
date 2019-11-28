

## To Run braid from the standalone jar

Either build or download the latest jar from [https://repo1.maven.org/maven2/io/bluebank/braid/braid-server/](https://repo1.maven.org/maven2/io/bluebank/braid/braid-server/).

```
java -jar ./braid-server.jar localhost:10005 user1 test 8999 3 https://repo1.maven.org/maven2/net/corda/corda-finance-contracts/4.0/corda-finance-contracts-4.0.jar https://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0/corda-finance-workflows-4.0.jar
```

## Running using Docker

A braid-server image is provided in Docker Hub:
[https://hub.docker.com/repository/docker/cordite/braid](https://hub.docker.com/repository/docker/cordite/braid)

Start it using 

```bash
docker run -p 8080:8080 -v "<cordapps-directory>":/opt/braid/cordapps cordite/braid:<version>
```

## Enabling TLS

The server will look for the following list of system properties or environment variables:

<table>
<thead>
<tr><th>Property</th><th>Env. Variable</th><th>Description</th></tr>
</thead>
<tbody>
<tr>
<td>braid.server.tls.cert.path</td><td>BRAID_SERVER_TLS_CERT_PATH</td>
<td>Path to a certificate file. Supported formats are jks, p12 or pfx, and pem</td>
</tr>
<tr>
<td>braid.server.tls.cert.secret</td><td>BRAID_SERVER_TLS_CERT_SECRET</td>
<td>The secret required to access the certificate. For jks and p12/pfx, this is a password. For pem files this is path to the private key in pem form</td>
</tr>
<tr>
<td>braid.server.tls.cert.type</td>
<td>BRAID_SERVER_TLS_CERT_TYPE</td>
<td>the type of cert: jks, p12/pfx, pem. If omitted the server will attempt to autodetect using the cert file extension</td>
</tr>
</tbody>
</table>

## Running locally in an IDE

To run braid locally within the IDE (IDEA), run one or more of the following modules,

- `BraidMainTest`
- `CordaAndBraidStandalone`
- `CordaStandalone`

... which run Corda (a sample Corda network with 2 nodes including a notary) and/or Braid.

These modules contain `main` functions, and
are in the `braid-server/src/test/kotlin/io/bluebank/braid/server` directory.

Depending on what you're testing, you might want to edit
[CordaAndBraidStandalone.kt](./src/test/kotlin/io/bluebank/braid/server/CordaStandalone.kt)
to change the number of nodes being tested,
and/or to change the value of the `startNodesInProcess` parameter.

Don't forget to add `-ea -javaagent:lib/quasar.jar` in the "VM options" of the IDE's
"Run/Debug Configuration dialog.

After you run `CordaAndBraidStandalone` (for example), and Braid has started,
the Braid swagger is then available on http://localhost:8080/swagger.json

Or, braid-server/src/main/kotlin/io/bluebank/braid/server/BraidMain.kt is similar to
braid-server/src/test/kotlin/io/bluebank/braid/server/BraidTestMain.kt

```BraidMain.kt  <node address> <username> <password> <port> [<cordaAppJar1> <cordAppJar2> ....]```

## Generating Braid Swagger Documents

Run:

```BraidDocsMain.kt <output_file> [<cordApp1Jar> <cordApp2Jar>...]```


