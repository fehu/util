utils
====
utils, initially separated from agents projects

**util** package contains utilities for 

* *ExecUtils*       basic process execution
* *FileUtils*       file wrappers, streams write/read helpers, `Path` 
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
    * and more

**compiler** package contains 

* *SourceCodePrinter*, based on [scala-refactoring](http://scala-refactoring.org/)
* experiments with interpreter
 
**shell** package contains **TODO**

* [Test reports](shell/test-reports) 
* *ProcessWrappers* advanced process execution utils

---

### SBT Keys

sbt `copy-test-reports` task copies specs2 markdown reports to `test-reports-copy-dir`

sbt `clean-test-reports` task cleans `test-reports-copy-dir`

set `clean-test-reports-on-clean-auto := true` to clean `test-reports-copy-dir` on `clean`

set `autoAddReportsToGit := true` to add reports to git repo on `copy-test-reports`

"test-reports" directory is set by `copyTestReportsDir <<= baseDirectory(base => Some(base / "test-reports"))`

to disable test reports copying, set `copyTestReportsDir := None`
