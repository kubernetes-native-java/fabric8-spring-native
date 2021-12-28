package io.kubernetesnativejava.fabric8.nativex;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.BeanFactoryNativeConfigurationProcessor;
import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.GenericTypeResolver;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registers everything in Spring Boot userspace packages, as well
 *
 * @author Josh Long
 */
@Slf4j
public class Fabric8BeanFactoryNativeConfigurationProcessor implements BeanFactoryNativeConfigurationProcessor {

	@Override
	public void process(ConfigurableListableBeanFactory beanFactory, NativeConfigurationRegistry registry) {
		Set<Class<?>> registerMe = new HashSet<>();
		List<String> strings = AutoConfigurationPackages.get(beanFactory);
		for (String pkg : strings) {
			Reflections reflections = new Reflections(pkg);
			Set<Class<? extends CustomResource>> customResources = reflections.getSubTypesOf(CustomResource.class);
			registerMe.addAll(customResources);
			registerMe.addAll(reflections.getSubTypesOf(CustomResourceList.class));

			customResources.forEach(cr -> {
				Map<TypeVariable, Type> typeVariableMap = GenericTypeResolver.getTypeVariableMap(cr);
				typeVariableMap.forEach((tv, clazz) -> {

					try {
						Class<?> type = Class.forName(clazz.getTypeName());
						log.info("the type variable is " + type.getName() + " and the class is " + clazz.getTypeName());
						registerMe.add(type);
						// registry.reflection().forType(type).withAccess(TypeAccess.values()).build();
					}
					catch (ClassNotFoundException e) {
						ReflectionUtils.rethrowRuntimeException(e);
					}
				});
			});

		}
		registerMe.forEach(c -> registry.reflection().forType(c).withAccess(TypeAccess.values()).build());
		registerMe.forEach(c -> log.info("registering " + c.getName() + '.'));
	}

}
