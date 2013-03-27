/*
 * Copyright 2013 JBoss by Red Hat.
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
package org.jbpm.runtime.manager.impl;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.kie.internal.runtime.manager.cdi.RuntimeManagerScoped;
import org.jbpm.runtime.manager.impl.mapper.JPAMapper;
import org.jbpm.task.identity.MvelUserGroupCallbackImpl;
import org.kie.api.runtime.EnvironmentName;

/**
 *
 * @author salaboy
 */
public class CDIRuntimeEnvironment extends SimpleRuntimeEnvironment{
    private EntityManagerFactory emf;
    
    @Inject
    private DefaultRegisterableItemsFactory itemsFactory;
    
    public CDIRuntimeEnvironment() {
        super();
        System.out.println(">>>>. THIS IS MY HASHCODE: "+this.hashCode());
    }

    public EntityManagerFactory getEmf() {
        return emf;
    }

    public void setEmf(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    public void init() {
        if (emf == null) {
            emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
        }   
        addToEnvironment(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
        this.mapper = new JPAMapper(emf);
        // TODO is this the right one to be default?
        this.userGroupCallback = new MvelUserGroupCallbackImpl();
        if(itemsFactory != null){
            this.registerableItemsFactory = itemsFactory;
        }
        
    }
}
