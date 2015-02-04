package com.netflix.eureka2.testkit.embedded.server;

import java.util.Properties;

import com.google.inject.Module;
import com.netflix.eureka2.EurekaDashboardModule;
import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.client.EurekaClientBuilder;
import com.netflix.eureka2.client.resolver.ServerResolver;
import com.netflix.eureka2.config.EurekaDashboardConfig;
import com.netflix.eureka2.registry.datacenter.LocalDataCenterInfo.DataCenterType;
import com.netflix.eureka2.testkit.embedded.server.EmbeddedDashboardServer.DashboardServerReport;
import com.netflix.eureka2.transport.EurekaTransports.Codec;

/**
 * @author Tomasz Bak
 */
public class EmbeddedDashboardServer extends EmbeddedEurekaServer<EurekaDashboardConfig, DashboardServerReport> {

    private static final String DASHBOARD_SERVER_NAME = "eureka2-dashboard";
    private static final int DASHBOARD_SERVER_PORTS_FROM = 16000;

    private final int discoveryPort;
    private final ServerResolver registrationServerResolver;
    private final ServerResolver discoveryServerResolver;

    public EmbeddedDashboardServer(EurekaDashboardConfig config,
                                   int discoveryPort, // TODO: remove this property once eureka2 UI tab is refactored
                                   ServerResolver registrationServerResolver,
                                   ServerResolver discoveryServerResolver,
                                   boolean withExt,
                                   boolean withDashboard) {
        super(config, withExt, withDashboard);
        this.discoveryPort = discoveryPort;
        this.registrationServerResolver = registrationServerResolver;
        this.discoveryServerResolver = discoveryServerResolver;
    }

    @Override
    public void start() {
        final EurekaClient eurekaClient = EurekaClientBuilder.newBuilder()
                .withReadServerResolver(discoveryServerResolver)
                .withWriteServerResolver(registrationServerResolver)
                .build();

        Module[] modules = {new EurekaDashboardModule(config, eurekaClient)};
        setup(modules);
    }

    @Override
    protected void loadInstanceProperties(Properties props) {
        super.loadInstanceProperties(props);
        props.setProperty("eureka.client.discovery-endpoint.port", Integer.toString(discoveryPort));
    }

    @Override
    public DashboardServerReport serverReport() {
        String dashboardURI = "http://localhost:" + config.getDashboardPort() + "/dashboard.html";
        return new DashboardServerReport(
                dashboardURI,
                formatAdminURI()
        );
    }

    public static EmbeddedDashboardServer newDashboard(ServerResolver registrationServerResolver,
                                                       ServerResolver discoveryServerResolver,
                                                       int discoveryPort,
                                                       boolean withExt,
                                                       boolean withAdminUI) {
        EurekaDashboardConfig config = EurekaDashboardConfig.newBuilder()
                .withAppName(DASHBOARD_SERVER_NAME)
                .withVipAddress(DASHBOARD_SERVER_NAME)
                .withDataCenterType(DataCenterType.Basic)
                .withCodec(Codec.Avro)
                .withShutDownPort(DASHBOARD_SERVER_PORTS_FROM + 3)
                .withWebAdminPort(DASHBOARD_SERVER_PORTS_FROM + 4)
                .build();
        return new EmbeddedDashboardServer(config, discoveryPort, registrationServerResolver, discoveryServerResolver, withExt, withAdminUI);
    }

    public static class DashboardServerReport {

        private final String dashboardURI;
        private final String adminURI;

        public DashboardServerReport(String dashboardURI, String adminURI) {
            this.dashboardURI = dashboardURI;
            this.adminURI = adminURI;
        }

        public String getDashboardURI() {
            return dashboardURI;
        }

        public String getAdminURI() {
            return adminURI;
        }
    }
}
