package ham.arhat.core;

import com.google.inject.AbstractModule;
import ham.arhat.config.facade.ConfigurationFactory;
import ham.arhat.config.loader.ConfigCache;

public class GuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ConfigurationFactory.class).to(ConfigCache.class);
    }
}
