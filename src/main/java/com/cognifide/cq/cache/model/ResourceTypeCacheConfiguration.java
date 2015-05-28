package com.cognifide.cq.cache.model;

import com.cognifide.cq.cache.definition.ResourceTypeCacheDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.sling.commons.osgi.OsgiUtil;

/**
 * @author Bartosz Rudnicki
 */
public class ResourceTypeCacheConfiguration extends CacheConfigurationEntryImpl {

	private boolean enabled;

	private final List<Pattern> invalidatePaths = new ArrayList<Pattern>();

	private String resourceTypePath;

	public ResourceTypeCacheConfiguration(String resourceType, int time) {
		super(resourceType, time, Integer.MIN_VALUE);
		enabled = false;
	}

	public ResourceTypeCacheConfiguration(String resourceType, int time, int cacheLevel) {
		super(resourceType, time, cacheLevel);
	}

	public ResourceTypeCacheConfiguration(ResourceTypeCacheDefinition resourceTypeCacheDefinition, int defaultTime) {
		super(resourceTypeCacheDefinition.getResourceType(),
				OsgiUtil.toInteger(resourceTypeCacheDefinition.getValidityTimeInSeconds(), defaultTime),
				resourceTypeCacheDefinition.getCacheLevel());
		this.enabled = resourceTypeCacheDefinition.isEnabled();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<Pattern> getInvalidatePaths() {
		return Collections.unmodifiableList(invalidatePaths);
	}

	public void addInvalidatePath(String regex) {
		addInvalidatePath(Pattern.compile(regex));
	}

	public void addInvalidatePaths(Collection<String> regexps) {
		for (String regexp : regexps) {
			addInvalidatePath(regexp);
		}
	}

	public void addInvalidatePath(Pattern pattern) {
		invalidatePaths.add(pattern);
	}

	public String getResourceTypePath() {
		return resourceTypePath;
	}

	public void setResourceTypePath(String resourceTypePath) {
		this.resourceTypePath = resourceTypePath;
	}
}
