# Fabric8 Spring Native Integrations 

Earlier, I wrote an example on how to [get Spring Native and the official Kubernetes Native Java client](https://joshlong.com/jl/blogPost/kubernetes-java-client-and-spring-native-and-graalvm.html) working with the newly released [Spring Native 0.11](https://www.youtube.com/watch?v=DVo5vmk5Cuw&t=2288s). I mentioned that I had to write [a trivial configuration class](https://github.com/kubernetes-native-java/spring-native-kubernetes/blob/main/src/main/java/io/kubernetes/nativex/KubernetesApiNativeConfiguration.java) to register the types that were used reflectively in the Spring Boot application, something that GraalVM frowns upon. 

Then, [Marc Nuri](https://twitter.com/MarcNuri) - who works on the Fabric8 project (the excellent Red Hat-driven client for Kubernetes) - took my trivial example, some of which I in turn took from the good [Dr. Dave Syer](https://twitter.com/david_syer), and turned into an example written in terms of Faric8. The Fabric8 project looks really good, and I wanted an excuse to get around to making that work at some point sooner rather than later, too! Now I had a great reason. Good news: it was even easier to get this working with Spring Boot and Spring Native. There's no Spring Boot autoconfiguration, _per se_, and there's no existing integration with Spring Nativem so I had to write that myself, but it was about as easy as the integration I wrote for the official Kubernetes Java client. This has to do mainly I think with the fact that there were fewer things in the project that I had to explicitly register. I could see patterns and then register everything that fit that pattern, and it _just worked_. (Three cheers for consistency!) It's so much more tedious to write GraalVM and Spring Native hints (configurations, basically) when you're sort of discovering the places that need those hints by surprise, one at a time. The cycle time is of course very slow for GraalVM compiles, so I was very happy to get something working so quickly: great job, Fabric8!

Here's the code for that integration:

```java
package io.kubernetesnativejava.fabric8.nativex;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.NamedCluster;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.nativex.AotOptions;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.nativex.type.NativeConfiguration;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
	* Spring Native support for excellent Fabric8 Kubernetes client from Red Hat (thanks, Red Hat!)
	*
	* @author Josh Long
	*/
@Slf4j
@NativeHint(options = {"-H:+AddAllCharsets", "--enable-https", "--enable-url-protocols=https"})
public class Fabric8NativeConfiguration implements NativeConfiguration {

	private final Class<?> clazz = NamedCluster.class;
	private final Reflections reflections = new Reflections(clazz.getPackageName(), clazz);

	@Override
	public void computeHints(NativeConfigurationRegistry registry, AotOptions aotOptions) {
		var subtypesOfKubernetesResource = reflections.getSubTypesOf(KubernetesResource.class);
		var othersToAddForReflection = List.of(
			io.fabric8.kubernetes.internal.KubernetesDeserializer.class
		);
		var combined = new HashSet<Class<?>>();
		combined.addAll(subtypesOfKubernetesResource);
		combined.addAll(othersToAddForReflection);
		combined.addAll(resolveSerializationClasses(JsonSerialize.class));
		combined.addAll(resolveSerializationClasses(JsonDeserialize.class));
		combined
			.stream()
			.filter(Objects::nonNull)
			.forEach(c -> {
				if (log.isDebugEnabled()) {
					log.debug("trying to register " + c.getName() + " for reflection");
				}
				registry.reflection().forType(c).withAccess(TypeAccess.values()).build();
			});
	}

	@SneakyThrows
	private <R extends Annotation> Set<Class<?>> resolveSerializationClasses(Class<R> annotationClazz) {
		var method = annotationClazz.getMethod("using");
		var classes = this.reflections.getTypesAnnotatedWith(annotationClazz);
		return classes.stream().map(clazzWithAnnotation -> {

			if (log.isDebugEnabled()) {
				log.debug("found " + clazzWithAnnotation.getName() + " : " + annotationClazz.getName());
			}

			var annotation = clazzWithAnnotation.getAnnotation(annotationClazz);
			try {
				if (annotation != null) {
					return (Class<?>) method.invoke(annotation);
				}
			}
			catch (Exception e) {
				ReflectionUtils.rethrowRuntimeException(e);
			}
			return null;
		})
			.collect(Collectors.toSet());
	}

}
``` 


You'll need to add that your `META-INF/spring.factories` file, like this: 

```properties
org.springframework.nativex.type.NativeConfiguration=io.kubernetesnativejava.fabric8.nativex.Fabric8NativeConfiguration
```

Now you just need an example that uses the Fabric8 API. I was able to sort of figure it out by looking at the blog post. The result looks fairly similar to my original example, so that helped, too. Here's the example:


```java
package com.example.fabric8native;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextStoppedEvent;

import java.util.Objects;

/**
	* This example uses <a href="https://developers.redhat.com/blog/2020/05/20/getting-started-with-the-fabric8-kubernetes-java-client#using_fabric8_with_kubernetes">
	* the Red Hat Fabric8 Kubernetes client</a>.
	* <p>
	* The example is inspired by <a href="https://blog.marcnuri.com/fabric8-kubernetes-java-client-and-quarkus-and-graalvm">this blog</a>,
	* which in turn was inspired by a blog I did.
	*
	* @author Josh Long
	*/

@Slf4j
@SpringBootApplication
public class Fabric8NativeApplication {

	public static void main(String[] args) {
		SpringApplication.run(Fabric8NativeApplication.class, args);
	}

	@Bean
	KubernetesClient kubernetesClient() {
		return new DefaultKubernetesClient();
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> start(KubernetesClient client,
																																																		SharedInformerFactory sharedInformerFactory,
																																																		ResourceEventHandler<Node> nodeEventHandler) {
		return args -> {
			client.nodes().list(new ListOptionsBuilder().withLimit(1L).build());
			sharedInformerFactory.startAllRegisteredInformers();
			var nodeHandler = sharedInformerFactory.getExistingSharedIndexInformer(Node.class);
			nodeHandler.addEventHandler(nodeEventHandler);
		};
	}

	@Bean
	ApplicationListener<ContextStoppedEvent> stop(SharedInformerFactory sharedInformerFactory) {
		return event -> sharedInformerFactory.stopAllRegisteredInformers(true);
	}

	@Bean
	SharedInformerFactory sharedInformerFactory(KubernetesClient client) {
		return client.informers();
	}

	@Bean
	SharedIndexInformer<Node> nodeInformer(SharedInformerFactory factory) {
		return factory.sharedIndexInformerFor(Node.class, NodeList.class, 0);
	}

	@Bean
	SharedIndexInformer<Pod> podInformer(SharedInformerFactory factory) {
		return factory.sharedIndexInformerFor(Pod.class, PodList.class, 0);
	}

	@Bean
	ResourceEventHandler<Node> nodeReconciler(SharedIndexInformer<Pod> podInformer) {
		return new ResourceEventHandler<>() {

			@Override
			public void onAdd(Node node) {
				log.info("node: " + Objects.requireNonNull(node.getMetadata()).getName());
				podInformer.getIndexer().list().stream()
					.map(pod -> Objects.requireNonNull(pod.getMetadata()).getName())
					.forEach(podName -> log.info("pod name:" + podName));
			}

			@Override
			public void onUpdate(Node oldObj, Node newObj) {
			}

			@Override
			public void onDelete(Node node, boolean deletedFinalStateUnknown) {
			}
		};
	}
}
```

Compile that using `mvn -DskipTests=true -Pnative clean package` and then run it: `./target/fabric8-native`. I get the following output. 


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

That's 29 thousandths of a second. And, best part: the resulting application takes up a meager 68M of RAM. 

Not bad! 