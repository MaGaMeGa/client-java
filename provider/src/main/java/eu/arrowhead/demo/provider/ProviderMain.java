package eu.arrowhead.demo.provider;

import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.ArrowheadServer;
import eu.arrowhead.common.api.clients.ServiceRegistryClient;
import eu.arrowhead.common.model.ServiceRegistryEntry;

class ProviderMain extends ArrowheadApplication {

  public static void main(String[] args) {
    new ProviderMain(args).start();
  }

  public ProviderMain(String[] args) {
    super(args);
  }

  @Override
  protected void onStart() {
    final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext.createFromProperties(true);
    final ArrowheadServer server = ArrowheadServer
            .createFromProperties(securityContext)
            .addResources(TemperatureResource.class, RestResource.class)
            .addPackages("eu.arrowhead.demo")
            .start();

    final ServiceRegistryClient registry = ServiceRegistryClient.createFromProperties(securityContext);
    registry.register(ServiceRegistryEntry.createFromProperties(server));
  }

  @Override
  protected void onStop() {
    
  }

}