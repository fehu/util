utils
====

[![Build Status](https://travis-ci.org/fehu/util.svg?branch=master)](https://travis-ci.org/fehu/util)

*utils, initially separated from agents projects*

**util** package contains utilities for 

* *file*       file wrappers, streams write/read helpers,
    * output streams: `File(target / (gen.name + ".html")).withOutputStream(File.write.utf8(xml), append = false)`
    * input streams:  `file.withInputStream(File.read[List[String]])`
    * wrapped file: `file.cp(target / path, overwrite = true)`
* *ExecUtils*       basic process execution
* *PrintIndents*    utilities for printing into `StringBuilder` with indents
* *ScopedState[T]*  guards a `ThreadLocal[T]` variable, providing methods
    * `def get: T` returns current state value
    * `def doWith[R](newState: T, expr: => R): R` 
        changes the value returned by `get` during `expr` by-name parameter initialization  
* *UnitInterval*    is a `Range[Double]` in [0, 1]
* *InUnitInterval*  is a value class with `double` underlying runtime representation, that is in range [0, 1]
* *Util*            contains different wrappers and useful functions
    * package object *feh.util* extends *Util*, use `import feh.util._`
    * The fixed point combinator `def Y[A, B](rec: (A => B) => (A => B)): A => B`
    * and it's cached version `CY`, that guards intermediate results for [faster performance](https://gist.github.com/fehu/7615890) 
    * random choice wrappers
    * `Path`, `PathSelector`
    * and more
