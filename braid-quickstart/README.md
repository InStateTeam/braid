# braid-quickstart Maven Archetype

## Build

To build locally and install correctly, execute the following in the parent project directory:

```bash
mvn install -pl braid-quickstart archetype:update-local-catalog
```


## How to use:

Interactive mode:

```bash
mvn archetype:generate -DarchetypeGroupId=io.bluebank -DarchetypeArtifactId=braid-quickstart 
```

Non-interactive mode:

```bash
mvn archetype:generate -B -DarchetypeGroupId=io.bluebank -DarchetypeArtifactId=braid-quickstart -DgroupId=io.bluebank.play -DartifactId=myservice -Dversion=1.0-SNAPSHOT 
```