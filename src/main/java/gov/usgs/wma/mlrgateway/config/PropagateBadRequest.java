package gov.usgs.wma.mlrgateway.config;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

import feign.codec.ErrorDecoder;
import gov.usgs.wma.mlrgateway.FeignBadResponseWrapper;

@Configuration
public class PropagateBadRequest {

	@Bean
	public ErrorDecoder errorDecoder() {
		return (methodKey, response) -> {
			int status = response.status();
			StringBuilder body = new StringBuilder("{\"").append(methodKey).append("\": [");
			try {
				body.append(IOUtils.toString(response.body().asReader()));
			} catch (Exception ignored) {}
			body.append("]}");
			HttpHeaders httpHeaders = new HttpHeaders();
			response.headers().forEach((k, v) -> httpHeaders.add("feign-" + k, StringUtils.join(v,",")));
			return new FeignBadResponseWrapper(status, httpHeaders, body.toString());
		};
	}

}
