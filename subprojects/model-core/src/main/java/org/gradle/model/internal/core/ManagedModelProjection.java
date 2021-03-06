/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.model.internal.core;

import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.gradle.model.internal.manage.instance.ManagedInstance;
import org.gradle.model.internal.manage.instance.ManagedProxyFactory;
import org.gradle.model.internal.manage.instance.ModelElementState;
import org.gradle.model.internal.manage.schema.ModelProperty;
import org.gradle.model.internal.manage.schema.ModelSchema;
import org.gradle.model.internal.manage.schema.ModelSchemaStore;
import org.gradle.model.internal.type.ModelType;

public class ManagedModelProjection<M> extends TypeCompatibilityModelProjectionSupport<M> {

    private final ModelSchemaStore schemaStore;
    private final ManagedProxyFactory proxyFactory;
    private final ModelSchema<M> schema;

    public ManagedModelProjection(ModelType<M> type, ModelSchemaStore schemaStore, ManagedProxyFactory proxyFactory) {
        super(type, true, true);
        this.schema = schemaStore.getSchema(type);
        this.schemaStore = schemaStore;
        this.proxyFactory = proxyFactory;
    }

    @Override
    protected ModelView<M> toView(final MutableModelNode modelNode, final ModelRuleDescriptor ruleDescriptor, final boolean writable) {
        return new ModelView<M>() {

            private boolean closed;

            public ModelType<M> getType() {
                return ManagedModelProjection.this.getType();
            }

            public M getInstance() {
                Class<M> clazz = getType().getConcreteClass(); // safe because we know schema must be of a concrete type
                return proxyFactory.createProxy(new State(), clazz);
            }

            public void close() {
                closed = true;
            }

            // TODO we are relying on the creator having established these links, we should be checking
            class State implements ModelElementState {
                public <T> T get(ModelType<T> modelType, String name) {
                    MutableModelNode propertyNode = modelNode.getLink(name);
                    ModelProperty<?> property = schema.getProperties().get(name);

                    if (!property.isWritable()) {
                        // TODO we are creating a new object each time the getter is called - we should reuse the instance for the life of the viewq
                        if (writable) {
                            ModelView<? extends T> modelView = propertyNode.asWritable(modelType, ruleDescriptor, null);
                            if (closed) {
                                //noinspection ConstantConditions
                                modelView.close();
                            }
                            //noinspection ConstantConditions
                            return modelView.getInstance();
                        } else {
                            //noinspection ConstantConditions
                            return propertyNode.asReadOnly(modelType, ruleDescriptor).getInstance();
                        }
                    } else {
                        return propertyNode.getPrivateData(modelType);
                    }
                }

                public <T> void set(ModelType<T> propertyType, String name, T value) {
                    if (!writable) {
                        throw new IllegalStateException(String.format("Cannot mutate model element '%s' of type '%s' as it is an input to rule '%s'", modelNode.getPath(), getType(), ruleDescriptor));
                    }
                    if (closed) {
                        throw new IllegalStateException(String.format("Cannot mutate model element '%s' of type '%s' used as subject of rule '%s' after the rule has completed", modelNode.getPath(), getType(), ruleDescriptor));
                    }

                    ModelSchema<T> schema = schemaStore.getSchema(propertyType);

                    if (schema.getKind().isManaged() && !ManagedInstance.class.isInstance(value)) {
                        throw new IllegalArgumentException(String.format("Only managed model instances can be set as property '%s' of class '%s'", name, getType()));
                    }
                    modelNode.getLink(name).setPrivateData(propertyType, value);
                }
            }
        };
    }


}
