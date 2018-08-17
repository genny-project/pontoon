package life.genny.bridge;

import java.util.ArrayList;
import java.util.List;

import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;

public class BridgeConfig {

	private static List<PermittedOptions> setInbounds(){
		List<PermittedOptions> inbounds = new ArrayList<PermittedOptions>();
		inbounds.add(new PermittedOptions().setAddress("address.inbound"));
		inbounds.add(new PermittedOptions().setAddressRegex(".*"));
		return inbounds;
	}
	
	private static List<PermittedOptions> setOutbounds(){
		List<PermittedOptions> inbounds = new ArrayList<PermittedOptions>();
		inbounds.add(new PermittedOptions().setAddressRegex("address.outbound"));
		inbounds.add(new PermittedOptions().setAddressRegex("^(?!(address\\.inbound)$).*"));
		return inbounds;
	}

	protected static BridgeOptions setBridgeOptions(){
		BridgeOptions options = new BridgeOptions();
		options.setMaxAddressLength(10000);
		setInbounds().stream().forEach(options::addInboundPermitted);
		setOutbounds().stream().forEach(options::addOutboundPermitted);
		return options;
	}
}
