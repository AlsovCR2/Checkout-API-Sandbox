package Portfolio.Checkout_api_sandbox;

import org.springframework.boot.SpringApplication;

public class TestCheckoutApiSandboxApplication {

	public static void main(String[] args) {
		SpringApplication.from(CheckoutApiSandboxApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
