bsc
=====================================================

### DEPENDENCIES
- **CPLEX (latest - academic license)**
- **jgrapht-core-1.3.1**

### BUILD PROCESS (IntelliJ IDEA)
```
Build -> Build Artifacts -> StorageLoadingProblems.jar
```

### RUN .jar and dynamically link CPLEX
```
$ java -jar -Djava.library.path="/opt/ibm/ILOG/CPLEX_Studio128/opl/bin/x86-64_linux/" StorageLoadingProblems.jar
```
