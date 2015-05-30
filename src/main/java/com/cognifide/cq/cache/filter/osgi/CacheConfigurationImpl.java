/*
 * Copyright 2015 Cognifide Polska Sp. z o. o..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cognifide.cq.cache.filter.osgi;

import com.cognifide.cq.cache.definition.osgi.OsgiConfigurationHelper;
import com.cognifide.cq.cache.model.alias.PathAliasReader;
import com.cognifide.cq.cache.model.alias.PathAliasStore;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import net.sf.ehcache.config.Configuration;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(label = "Sling Caching Filter", description = "Sling Caching Filter", metatype = true, immediate = true)
@Service(value = {CacheConfiguration.class, CacheManagerProvider.class})
public class CacheConfigurationImpl implements CacheConfiguration, CacheManagerProvider {

	private static final Logger logger = LoggerFactory.getLogger(CacheConfigurationImpl.class);

	private static final String CACHING_PROVIDER = "org.ehcache.jcache.JCacheCachingProvider";

	private static final boolean FILTER_ENABLED_DEFAULT_VALUE = false;

	private static final int MAX_ENTRIES_IN_CACHE_DEFAULT_VALUE = 1000;

	private static final int VALIDITY_TIME_DEFAULT_VALUE = 3600;

	private static final String EVICTION_POLICY_DEFAULT_VALUE = "LRU";

	@Property(
					label = "Enabled",
					description = "Turns on caching filter",
					boolValue = FILTER_ENABLED_DEFAULT_VALUE)
	private static final String FILTER_ENABLED_PROPERTY = "cache.config.enabled";

	@Property(
					label = "Validity time",
					description = "Specifies cache entry validity time (in seconds). Used when Resource Type Definition has validity time not set.",
					intValue = VALIDITY_TIME_DEFAULT_VALUE)
	private static final String VALIDITY_TIME_PROPERTY = "cache.config.validity.time";

	@Property(label = "Path aliases",
					description = "Path aliases format: $<alias name>|<path 1>|<path 2>|...",
					unbounded = PropertyUnbounded.ARRAY)
	private static final String PROPERTY_PATH_ALIASES = "cache.config.pathaliases";

	@Property(
					label = "Max entries in cache",
					description = "Max entries in cache, disk and heap. 0 means unlimited.",
					intValue = MAX_ENTRIES_IN_CACHE_DEFAULT_VALUE)
	private static final String MAX_ENTRIES_IN_CACHE_PROPERTY = "cache.config.capacity";

	@Property(
					label = "Eviction policy",
					description = "Sets the eviction policy",
					options = {
						@PropertyOption(name = "LRU", value = "LRU"),
						@PropertyOption(name = "LFU", value = "LFU"),
						@PropertyOption(name = "FIFO", value = "FIFO")
					},
					value = EVICTION_POLICY_DEFAULT_VALUE)
	private static final String EVICTION_POLICY_PROPERTY = "cache.config.eviction.policy";

	@Reference
	private PathAliasStore pathAliasStore;

	private CacheManager cacheManager;

	private boolean enabled;

	private int maxEntriesInCache;

	private int validityTime;

	private String evictionPolicy;

	@Activate
	public void activate(ComponentContext context) {
		String[] aliasesStrings = OsgiConfigurationHelper.getStringArrayValuesFrom(PROPERTY_PATH_ALIASES, context);
		pathAliasStore.addAliases(new PathAliasReader().readAliases(aliasesStrings));

		enabled = OsgiConfigurationHelper.getBooleanValueFrom(FILTER_ENABLED_PROPERTY, context);
		maxEntriesInCache = OsgiConfigurationHelper.getIntegerValueFrom(MAX_ENTRIES_IN_CACHE_PROPERTY, context);
		validityTime = OsgiConfigurationHelper.getIntegerValueFrom(VALIDITY_TIME_PROPERTY, context);
		evictionPolicy = OsgiConfigurationHelper.getStringValueFrom(EVICTION_POLICY_PROPERTY, context);

		createCacheManager();
	}

	private void createCacheManager() {
		if (null == cacheManager || cacheManager.isClosed()) {
			Caching.setDefaultClassLoader(getClass().getClassLoader());
			CachingProvider cachingProvider = Caching.getCachingProvider(CACHING_PROVIDER);
			cacheManager = cachingProvider.getCacheManager();
			configureCacheManagerVendorSpecific();
			logger.info("Cache manager {} was created.", cacheManager.getURI());
		}
	}

	private void configureCacheManagerVendorSpecific() {
		net.sf.ehcache.config.CacheConfiguration ehcacheConfiguration = new net.sf.ehcache.config.CacheConfiguration();
		ehcacheConfiguration.setMaxEntriesInCache(maxEntriesInCache);
		ehcacheConfiguration.setMaxEntriesLocalDisk(maxEntriesInCache);
		ehcacheConfiguration.setMaxEntriesLocalHeap(maxEntriesInCache);
		ehcacheConfiguration.setMemoryStoreEvictionPolicy(evictionPolicy);
		Configuration configuration = cacheManager.unwrap(net.sf.ehcache.CacheManager.class).getConfiguration();
		configuration.setDefaultCacheConfiguration(ehcacheConfiguration);
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public int getValidityTimeInSeconds() {
		return validityTime;
	}

	@Override
	public CacheManager getCacheManger() {
		return cacheManager;
	}

	@Deactivate
	protected void deactivate() {
		cacheManager.close();
	}

}
