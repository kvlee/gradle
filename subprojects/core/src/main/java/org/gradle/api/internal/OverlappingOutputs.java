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

package org.gradle.api.internal;

import org.gradle.api.internal.changedetection.state.FileCollectionSnapshot;
import org.gradle.api.internal.changedetection.state.FileContentSnapshot;
import org.gradle.internal.file.FileType;

import javax.annotation.Nullable;
import java.util.Map;

public class OverlappingOutputs {
    private final String propertyName;
    private final String overlappedFilePath;

    public OverlappingOutputs(String propertyName, String overlappedFilePath) {
        this.propertyName = propertyName;
        this.overlappedFilePath = overlappedFilePath;
    }

    @Nullable
    public static OverlappingOutputs detect(String propertyName, FileCollectionSnapshot previousExecution, FileCollectionSnapshot beforeExecution) {
        Map<String, FileContentSnapshot> previousSnapshots = previousExecution.getContentSnapshots();
        Map<String, FileContentSnapshot> beforeSnapshots = beforeExecution.getContentSnapshots();

        for (Map.Entry<String, FileContentSnapshot> beforeSnapshot : beforeSnapshots.entrySet()) {
            String path = beforeSnapshot.getKey();
            FileContentSnapshot fileSnapshot = beforeSnapshot.getValue();
            FileContentSnapshot previousSnapshot = previousSnapshots.get(path);
            // Missing files can be ignored
            if (fileSnapshot.getType() != FileType.Missing) {
                if (createdSincePreviousExecution(previousSnapshot) || changedSincePreviousExecution(fileSnapshot, previousSnapshot)) {
                    return new OverlappingOutputs(propertyName, path);
                }
            }
        }
        return null;
    }

    private static boolean changedSincePreviousExecution(FileContentSnapshot fileSnapshot, FileContentSnapshot previousSnapshot) {
        // _changed_ since last execution, possibly by another task
        return !previousSnapshot.isContentUpToDate(fileSnapshot);
    }

    private static boolean createdSincePreviousExecution(@Nullable FileContentSnapshot previousSnapshot) {
        // created since last execution, possibly by another task
        return previousSnapshot == null;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getOverlappedFilePath() {
        return overlappedFilePath;
    }

    public String toString() {
        return String.format("output property '%s' with path '%s'", propertyName, overlappedFilePath);
    }
}
