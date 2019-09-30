

**To Run braid from the standalone jar**

```
java -jar ./braid-server.jar localhost:10005 user1 test 8999 https://repo1.maven.org/maven2/net/corda/corda-finance-contracts/4.0/corda-finance-contracts-4.0.jar https://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0/corda-finance-workflows-4.0.jar
```

**Running locally in an IDE**


1. Run a Sample corda Network with notary and 2 nodes
   - ```CordaStandalone.kt```
1. Run Braid using either of the following
   - ```BraidMain.kt  <node address> <username> <password> <port> [<cordaAppJar1> <cordAppJar2> ....]```
1. The Braid swagger is then available on: http://localhost:8999/swagger.json



**Generating Braid Swagger Documents**

1. Run ```BraidDocsMain.kt <output_file> [<cordApp1Jar> <cordApp2Jar>...]```


