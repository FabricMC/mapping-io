/*
 * Copyright (c) 2021 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.mappingio.tree;

import net.fabricmc.mappingio.MappingVisitor;

/**
 * {@link MappingTree} that can be visited.
 *
 * <p>Accessing data manipulating {@link MappingTree} methods during an ongoing visitation pass
 * may lead to undefined behavior and data corruption.
 *
 * <p>Visited data isn't guaranteed to be available until the visitation pass is complete.
 */
public interface VisitableMappingTree extends MappingTree, MappingVisitor {
}
