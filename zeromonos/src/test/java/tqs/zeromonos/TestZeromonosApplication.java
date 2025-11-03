package tqs.zeromonos;

import org.springframework.boot.SpringApplication;

public class TestZeromonosApplication {
	public static void main(String[] args) {
		SpringApplication.from(ZeromonosApplication::main).with(TestcontainersConfiguration.class).run(args);
	}
}
