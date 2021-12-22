package com.example.fabric8native;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.nativex.hint.TypeHint;

/**
	* This example uses <a href="https://developers.redhat.com/blog/2020/05/20/getting-started-with-the-fabric8-kubernetes-java-client#using_fabric8_with_kubernetes">
	* the Red Hat Fabric8 Kubernetes client</a>
	*
	* @author Josh Long
	*/
@NativeHint(options = {"-H:+AddAllCharsets", "--enable-https", "--enable-url-protocols=https"})

@SpringBootApplication
public class Fabric8NativeApplication {

	public static void main(String[] args) {
		SpringApplication.run(Fabric8NativeApplication.class, args);
	}

	@Bean
	ApplicationRunner runner() {
		return args -> {
			try (var client = new DefaultKubernetesClient()) {

				client.pods().inNamespace("default").list().getItems().forEach(
					pod -> System.out.println(pod.getMetadata().getName())
				);

			}
			catch (KubernetesClientException ex) {
				// Handle exception
				ex.printStackTrace();
			}
		};
	}

}
