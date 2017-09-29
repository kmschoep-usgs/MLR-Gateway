package gov.usgs.wma.mlrgateway.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

public class ZuulAwareRequestWrapper extends HttpServletRequestWrapper {

	private final HttpServletRequest request;

	private final UriComponents components;

	ZuulAwareRequestWrapper(HttpServletRequest request) {
		super(request);
		this.request = request;
		this.components = ServletUriComponentsBuilder.fromRequest(request).build();
	}

	@Override
	public String getRequestURI() {
		return components.getPath();
	}

	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer(components.toUriString());
	}

	@Override
	public int getServerPort() {
		return components.getPort();
	}

}
