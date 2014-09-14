These classes are required by the new sub-services ofwhich the new 
calliope will be composed. The first are MML and Project. Each 
moduleruns on its own port (MML=8083, Project=8084), and will eventually 
be web-apps running inside Tomcat. But for now development requires them 
to be fast-booting and easy to debug Jetty applications. These classes 
in the old Calliope that need to be shared beteen modules will be in 
this library.
