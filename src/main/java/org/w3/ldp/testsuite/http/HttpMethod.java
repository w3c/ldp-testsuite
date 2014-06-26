package org.w3.ldp.testsuite.http;

public enum HttpMethod {

	GET("GET"),
	PUT("PUT"),
	POST("POST"),
	DELETE("DELETE"),
	PATCH("PATCH"),
	HEAD("HEAD"),
	OPTIONS("OPTIONS");

	private String name;

	HttpMethod(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
