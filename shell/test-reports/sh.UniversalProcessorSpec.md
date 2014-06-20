## UniversalProcessor
  
     A `UniversalProcessor` processes source code, reading its configuration from the same source.
     Provides the following features:                                                            
                        | sh-line |   processes one-line shell expressions (+)        
                        the 'one-line' shell expressions support '\' multi-lining (+)        
                        | sh-block |  processes multi-line shell expression blocks (+)            
            
                        | shortcuts | provide the following shortcuts for scala expressions                         
                                                var:    $name = ...      => var name = ... (+)                
                                                val:    c$name = ...     => val name = ... (+)                
                                                args:   $1, $2, ..., $N  => args(1), args(2), ..., args(N) (+)                
                                                object: ##name           => object name (+)            
                        shortcuts must not affect strings and the following expressions                             
                                                var:    "$arg = $value" TODO (*)                
                                                val:    "c$arg = c$value" TODO (*)                
                                                args:   evidence$1, evidence$2 TODO (*)                
                                                object: x.## max 2, "##ERROR" TODO (*)            
                        Multiline config:                                                                           
                                                several #conf keywords in the begining of the source (+)                
                                                several #conf keywords in different parts of the source (+)                
                                                multi-line, escaped by '\' TODO (*)        
                        | all |       key for enabling all the features listed above (+)            
            
                        Dependency management                                                                       
                                                by package *name*, *group* and *version* TODO (*)                
                                                by package *name* and *group*, choosing latest version TODO (*)                
                                                by package *name* only TODO (*)                
                                                all dependency management methods support scala versioning TODO (*)        
        
                        Quick imports TODO (*)        
        
                        Predefined imports TODO (*)        
        
                        Predefined dependencies TODO (*)                                                                                        
                                                                                                                                                                                
| UniversalProcessor |
| Finished in 20 ms |
| 22 examples, 23 expectations, 0 failure, 0 error, 12 pending |