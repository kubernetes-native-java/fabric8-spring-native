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
	* the Red Hat Fabric8 Kubernetes client</a>. The example is inspired by <a href="https://blog.marcnuri.com/fabric8-kubernetes-java-client-and-quarkus-and-graalvm">this blog</a>,
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
					.forEach(podName -> log.info("\tpod:" + podName));
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



