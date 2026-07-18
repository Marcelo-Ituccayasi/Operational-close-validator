package com.marceloituccayasi.ocv;

import org.springframework.boot.SpringApplication;

public class TestOperationalCloseValidatorApplication {

	public static void main(String[] args) {
		SpringApplication.from(OperationalCloseValidatorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
