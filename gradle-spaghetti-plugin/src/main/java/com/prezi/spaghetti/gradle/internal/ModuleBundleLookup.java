package com.prezi.spaghetti.gradle.internal;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.prezi.spaghetti.bundle.ModuleBundleSet;
import com.prezi.spaghetti.bundle.ModuleBundleType;
import com.prezi.spaghetti.bundle.internal.ModuleBundleLoader;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class ModuleBundleLookup {
	private static final Logger logger = LoggerFactory.getLogger(ModuleBundleLookup.class);

	public static ModuleBundleSet lookup(Project project, Object dependencies, Object lazyDependencies, ModuleBundleType moduleBundleType, Set<File> filterFilesForIncrementalTask) throws IOException {
		Set<File> directFiles = Sets.newLinkedHashSet();
		Set<File> lazyFiles = Sets.newLinkedHashSet();
		Set<File> transitiveFiles = Sets.newLinkedHashSet();

		addFiles(project, dependencies, directFiles, transitiveFiles);
		addFiles(project, lazyDependencies, lazyFiles, transitiveFiles);

		transitiveFiles.removeAll(lazyFiles);

		if (filterFilesForIncrementalTask != null) {
			Predicate<File> removePredicate = file -> {
				if (filterFilesForIncrementalTask.contains(file)) {
					return false;
				}
				if (file.isDirectory()) {
					for (File filterFile : filterFilesForIncrementalTask) {
						if (filterFile.toPath().startsWith(file.toPath())) {
							return false;
						}
					}
				}
				return true;
			};
			directFiles.removeIf(removePredicate);
			lazyFiles.removeIf(removePredicate);
			transitiveFiles.removeIf(removePredicate);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Loading modules from:");
			logger.debug("\tDirect dependencies:\n\t\t{}", Joiner.on("\n\t\t").join(directFiles));
			logger.debug("\tLazy dependencies:\n\t\t{}", Joiner.on("\n\t\t").join(lazyFiles));
			logger.debug("\tTransitive dependencies:\n\t\t{}", Joiner.on("\n\t\t").join(transitiveFiles));
		}

		return ModuleBundleLoader.loadBundles(directFiles, lazyFiles, transitiveFiles, moduleBundleType);
	}

	private static void addFiles(Project project, Object from, Set<File> directFiles, Set<File> transitiveFiles) throws IOException {
		if (from == null) {
			return;
		}

		if (from instanceof Configuration) {
			Configuration config = (Configuration) from;
			Set<ResolvedDependency> firstLevelDependencies = config.getResolvedConfiguration().getFirstLevelModuleDependencies();
			addAllFilesFrom(firstLevelDependencies, directFiles);
			transitiveFiles.addAll(config.resolve());
		} else if (from instanceof ConfigurableFileCollection) {
			for (Object child : ((ConfigurableFileCollection) from).getFrom()) {
				addFiles(project, child, directFiles, transitiveFiles);
			}
		} else if (from instanceof FileCollection) {
			directFiles.addAll(((FileCollection) from).getFiles());
		} else if (from instanceof Collection) {
			for (Object child : ((Collection<?>) from)) {
				addFiles(project, child, directFiles, transitiveFiles);
			}
		} else if (from.getClass().isArray()) {
			for (int i = 0; i < Array.getLength(from); i++) {
				addFiles(project, Array.get(from, i), directFiles, transitiveFiles);
			}
		} else if (from instanceof Callable) {
			try {
				addFiles(project, ((Callable) from).call(), directFiles, transitiveFiles);
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}
		} else if (from instanceof File) {
			directFiles.add((File) from);
		} else {
			for (File file : project.files(from)) {
				directFiles.add(file);
			}
		}
	}

	private static void addAllFilesFrom(Set<ResolvedDependency> dependencies, Set<File> files) throws IOException {
		for (ResolvedDependency dependency : dependencies) {
			for (ResolvedArtifact artifact : dependency.getModuleArtifacts()) {
				files.add(artifact.getFile());
			}
		}
	}
}
