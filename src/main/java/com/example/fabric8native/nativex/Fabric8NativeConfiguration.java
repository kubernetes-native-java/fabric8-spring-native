package com.example.fabric8native.nativex;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.NamedCluster;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.nativex.AotOptions;
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
	* @author Josh Long
	*/

@Slf4j
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
				log.info("trying to register " + c.getName() + " for reflection");
				registry.reflection().forType(c).withAccess(TypeAccess.values()).build();
			});
	}

	@SneakyThrows
	<R extends Annotation> Set<Class<?>> resolveSerializationClasses(Class<R> annotationClazz) {
		var method = annotationClazz.getMethod("using");
		var classes = this.reflections.getTypesAnnotatedWith(annotationClazz);
		return classes.stream().map(clazzWithAnnotation -> {
			log.info("found " + clazzWithAnnotation.getName() + " : " + annotationClazz.getName());
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