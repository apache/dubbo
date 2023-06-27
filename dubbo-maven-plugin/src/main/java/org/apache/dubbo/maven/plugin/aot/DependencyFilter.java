/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.maven.plugin.aot;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for {@link ArtifactsFilter} based on a {@link org.apache.dubbo.maven.plugin.aot.FilterableDependency} list.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 */
public abstract class DependencyFilter extends AbstractArtifactsFilter {

	private final List<? extends FilterableDependency> filters;

	/**
	 * Create a new instance with the list of {@link FilterableDependency} instance(s) to
	 * use.
	 * @param dependencies the source dependencies
	 */
	public DependencyFilter(List<? extends FilterableDependency> dependencies) {
		this.filters = dependencies;
	}

	@Override
	public Set<Artifact> filter(Set<Artifact> artifacts) throws ArtifactFilterException {
		Set<Artifact> result = new HashSet<>();
		for (Artifact artifact : artifacts) {
			if (!filter(artifact)) {
				result.add(artifact);
			}
		}
		return result;
	}

	protected abstract boolean filter(Artifact artifact);

	/**
	 * Check if the specified {@link Artifact} matches the
	 * specified {@link org.apache.dubbo.maven.plugin.aot.FilterableDependency}. Returns
	 * {@code true} if it should be excluded
	 * @param artifact the Maven {@link Artifact}
	 * @param dependency the {@link org.apache.dubbo.maven.plugin.aot.FilterableDependency}
	 * @return {@code true} if the artifact matches the dependency
	 */
	protected final boolean equals(Artifact artifact, FilterableDependency dependency) {
		if (!dependency.getGroupId().equals(artifact.getGroupId())) {
			return false;
		}
		if (!dependency.getArtifactId().equals(artifact.getArtifactId())) {
			return false;
		}
		return (dependency.getClassifier() == null
				|| artifact.getClassifier() != null && dependency.getClassifier().equals(artifact.getClassifier()));
	}

	protected final List<? extends FilterableDependency> getFilters() {
		return this.filters;
	}

}
