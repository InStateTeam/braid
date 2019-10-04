

## To Run braid from the standalone jar

```
java -jar ./braid-server.jar localhost:10005 user1 test 8999 3 https://repo1.maven.org/maven2/net/corda/corda-finance-contracts/4.0/corda-finance-contracts-4.0.jar https://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0/corda-finance-workflows-4.0.jar
```

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


