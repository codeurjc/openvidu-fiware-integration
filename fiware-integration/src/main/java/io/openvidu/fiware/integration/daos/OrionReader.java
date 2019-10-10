/*
 * Copyright 2018 Kurento (https://www.kurento.org)
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

package io.openvidu.fiware.integration.daos;

import io.openvidu.fiware.integration.commons.JsonManager;
import io.openvidu.fiware.integration.config.OrionConnectorConfiguration;
import io.openvidu.fiware.integration.models.orion.OrionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic Class for Reading Orion Entities.
 *
 * @param <O extends {@link OrionEntity}>
 */
public abstract class OrionReader<T, O extends OrionEntity> {

    protected OrionConnector<O> orionConnector;

    public OrionReader(OrionConnectorConfiguration config, Class<O> clazz) {
        super();
        this.orionConnector = new OrionConnector<O>(config, clazz) {
        };
    }

    public OrionReader(OrionConnectorConfiguration config, JsonManager<O> manager, Class<O> clazz) {
        super();
        this.orionConnector = new OrionConnector<O>(config, manager, clazz) {
        };
    }

    /**
     * Reads a entity in FIWARE as a given Object.
     *
     * @param id a id as String
     */
    public T readObject(String id) {
        System.out.println("11111: " + id);
        O orionEntity = orionConnector.readEntity(id);
        System.out.println("22222: " + id);
        return mapOrionEntityToEntity(orionEntity);
    }

    /**
     * Reads a entity in FIWARE.
     *
     * @param id a id as String
     */
    public O readOrionEntity(String id) {
        return orionConnector.readEntity(id);
    }

    /**
     * Reads a list of entities in FIWARE as a given Object.
     *
     * @param type a type as String
     */
    public List<T> readObjectList(String type) {
        List<O> orionEntityList = orionConnector.readEntityList(type);
        List<T> result = new ArrayList<T>();

        for (O orionEntity : orionEntityList) {
            result.add(mapOrionEntityToEntity(orionEntity));
        }
        return result;
    }

    /**
     * Reads a entity in FIWARE.
     *
     * @param type a id as String
     */
    public List<O> readOrionEntityList(String type) {
        return orionConnector.readEntityList(type);
    }

    /**
     * Given an object, maps to an appropriate output FIWARE object.
     *
     * @param entity a T entity
     */
    abstract public T mapOrionEntityToEntity(final O entity);

}
