#!/usr/bin/env bash 

Earlier, I wrote an example on how to get Spring Native and the official Kubernetes Native Java client working with the newly released Spring Native 0.11. I mentioned that I had to write a trivial configuratio class to register the types that were used in the Spring Boot application in a reflective fashion, something that GraalVM frowns upon. The configuration class was pretty trivial, especially since the application  used  

```

❯ ./target/fabric8-native
2021-12-22 17:31:13.069  INFO 34762 --- [           main] o.s.nativex.NativeListener               : AOT mode enabled

.   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
'  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/
:: Spring Boot ::                (v2.6.2)

2021-12-22 17:31:13.071  INFO 34762 --- [           main] c.e.f.Fabric8NativeApplication           : Starting Fabric8NativeApplication v0.0.1-SNAPSHOT using Java 17.0.1 on mbp2019.local with PID 34762 (/Users/jlong/Downloads/fabric8-native/target/fabric8-native started by jlong in /Users/jlong/Downloads/fabric8-native)
2021-12-22 17:31:13.071  INFO 34762 --- [           main] c.e.f.Fabric8NativeApplication           : No active profile set, falling back to default profiles: default
2021-12-22 17:31:13.083  INFO 34762 --- [           main] c.e.f.Fabric8NativeApplication           : Started Fabric8NativeApplication in 0.029 seconds (JVM running for 0.03)
2021-12-22 17:31:13.665  INFO 34762 --- [-controller-Pod] i.f.k.client.informers.cache.Controller  : informer#Controller: ready to run resync and reflector runnable
2021-12-22 17:31:13.665  INFO 34762 --- [controller-Node] i.f.k.client.informers.cache.Controller  : informer#Controller: ready to run resync and reflector runnable
2021-12-22 17:31:13.665  INFO 34762 --- [-controller-Pod] i.f.k.client.informers.cache.Controller  : informer#Controller: resync skipped due to 0 full resync period
2021-12-22 17:31:13.665  INFO 34762 --- [controller-Node] i.f.k.client.informers.cache.Controller  : informer#Controller: resync skipped due to 0 full resync period
2021-12-22 17:31:13.665  INFO 34762 --- [-controller-Pod] i.f.k.client.informers.cache.Reflector   : Started ReflectorRunnable watch for class io.fabric8.kubernetes.api.model.Pod
2021-12-22 17:31:13.666  INFO 34762 --- [controller-Node] i.f.k.client.informers.cache.Reflector   : Started ReflectorRunnable watch for class io.fabric8.kubernetes.api.model.Node
2021-12-22 17:31:14.254  INFO 34762 --- [pool-2-thread-1] c.e.f.Fabric8NativeApplication           : node: gke-knj-demos-default-pool-8fdf2ef6-55q9
2021-12-22 17:31:14.254  INFO 34762 --- [pool-2-thread-1] c.e.f.Fabric8NativeApplication           : node: gke-knj-demos-default-pool-8fdf2ef6-p3kz
2021-12-22 17:31:14.254  INFO 34762 --- [pool-2-thread-1] c.e.f.Fabric8NativeApplication           : node: gke-knj-demos-default-pool-8fdf2ef6-xh08

```