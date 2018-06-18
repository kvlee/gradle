/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.cache.internal;

import org.apache.commons.io.FileUtils;
import org.gradle.cache.CleanableStore;
import org.gradle.cache.CleanupAction;
import org.gradle.internal.time.CountdownTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public abstract class AbstractCacheCleanup implements CleanupAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCacheCleanup.class);

    private final FilesFinder eligibleFilesFinder;

    public AbstractCacheCleanup(FilesFinder eligibleFilesFinder) {
        this.eligibleFilesFinder = eligibleFilesFinder;
    }

    @Override
    public void clean(CleanableStore cleanableStore, CountdownTimer timer) {
        int filesDeleted = 0;
        for (File file : findEligibleFiles(cleanableStore)) {
            if (timer.hasExpired()) {
                LOGGER.warn("{} cleanup was aborted because timeout has expired", cleanableStore.getDisplayName());
                break;
            }
            if (shouldDelete(file)) {
                if (FileUtils.deleteQuietly(file)) {
                    handleDeletion(file);
                    filesDeleted++;
                }
            }
        }
        LOGGER.info("{} cleanup deleted {} files/directories.", cleanableStore.getDisplayName(), filesDeleted);
    }

    protected abstract boolean shouldDelete(File file);

    protected abstract void handleDeletion(File file);

    private Iterable<File> findEligibleFiles(CleanableStore cleanableStore) {
        return eligibleFilesFinder.find(cleanableStore.getBaseDir(), new NonReservedFileFilter(cleanableStore.getReservedCacheFiles()));
    }

}
