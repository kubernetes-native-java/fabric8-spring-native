package com.example.fabric8native.nativex;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.NamedCluster;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.nativex.AotOptions;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.nativex.type.NativeConfiguration;

import java.util.HashSet;
import java.util.List;


@Slf4j
public class Fabric8NativeConfiguration implements NativeConfiguration {

	@Override
	public void computeHints(NativeConfigurationRegistry registry, AotOptions aotOptions) {

		var namedClusterClass = NamedCluster.class;
		var reflections = new Reflections(namedClusterClass.getPackageName(), namedClusterClass);
		var subtypesOfKubernetesResource = reflections.getSubTypesOf(KubernetesResource.class);
		var othersToAddForReflection = List.of(
			io.fabric8.kubernetes.internal.KubernetesDeserializer.class
		);

		var combined = new HashSet<Class<?>>();
		combined.addAll(subtypesOfKubernetesResource);
		combined.addAll(othersToAddForReflection);

		for (var c : combined) {
			log.info("trying to register " + c.getName() + " for reflection");
			registry.reflection().forType(c).withAccess(TypeAccess.values()).build();
		}
	}
}