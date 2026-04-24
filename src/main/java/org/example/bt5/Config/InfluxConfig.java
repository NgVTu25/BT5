package org.example.bt5.Config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class InfluxConfig {
	@Value("${influx.url}")
	private String url;
	@Value("${influx.token}")
	private String token;

	@Value("${influx.org}")
	private String org;
	@Value("${influx.bucket}")
	private String bucket;


	@Bean
	public InfluxDBClient influxDBClient() {

		OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
				.callTimeout(3, TimeUnit.MINUTES)
				.readTimeout(3, TimeUnit.MINUTES)
				.writeTimeout(3, TimeUnit.MINUTES)
				.connectTimeout(3, TimeUnit.MINUTES);

		InfluxDBClientOptions options = InfluxDBClientOptions.builder()
				.url(url)
				.authenticateToken(token.toCharArray())
				.org(org)
				.bucket(bucket)
				.okHttpClient(httpClient)
				.build();

		return InfluxDBClientFactory.create(options);
	}
}