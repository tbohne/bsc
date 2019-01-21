Heuristische Lösungsverfahren für Stacking-Probleme mit Transportkosten
=====================================================

Stacking-Probleme beschreiben Situationen, in denen es darum geht, eine Menge von Items zulässigen Positionen in Stacks zuzuordnen, sodass bestimmte Nebenbedingungen respektiert werden und ggf. eine Zielfunktion optimiert wird. Sie treten in der Praxis häufig im Umfeld von Lagerhallen und Container-Terminals auf.
In dieser Arbeit werden heuristische Lösungsverfahren für verschiedene Varianten von Stacking-Problemen entwickelt, bei denen der Fokus auf der Minimierung der Transportkosten liegt. Dabei werden MIP-Formulierungen zum experimentellen Vergleich genutzt.

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
