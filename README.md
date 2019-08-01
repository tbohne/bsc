Heuristische Lösungsverfahren für Stacking-Probleme mit Transportkosten
=====================================================

Stacking-Probleme beschreiben Situationen, in denen es darum geht, eine Menge von Items zulässigen Positionen in Stacks zuzuordnen, sodass bestimmte Nebenbedingungen respektiert werden und ggf. eine Zielfunktion optimiert wird. Sie treten in der Praxis häufig im Umfeld von Lagerhallen und Container-Terminals auf.
In dieser Arbeit werden heuristische Lösungsverfahren für verschiedene Varianten von Stacking-Problemen entwickelt, bei denen der Fokus auf der Minimierung der Transportkosten liegt. Dabei werden MIP-Formulierungen zum experimentellen Vergleich genutzt.

Stacking problems describe situations in which a set of items has to be assigned to feasible
positions in stacks, such that certain constraints are respected and, if necessary, an objective function is optimized.
In practice, such problems for example occur in warehouses and container terminals.
In the present work heuristic approaches are developed for various stacking problems, where the focus is on
minimizing transport costs. MIP formulations are used for experimental comparison.

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
