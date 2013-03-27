package org.jbpm.runtime.manager.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.jbpm.process.audit.AuditLoggerFactory;
import org.jbpm.process.audit.AuditLoggerFactory.Type;
import org.kie.internal.runtime.manager.cdi.RuntimeManagerScoped;
import org.jbpm.task.wih.ExternalTaskEventListener;
import org.jbpm.task.wih.LocalHTWorkItemHandler;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.WorkingMemoryEventListener;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.internal.runtime.StatefulKnowledgeSession;

import org.kie.internal.runtime.manager.Disposable;
import org.kie.internal.runtime.manager.DisposeListener;
import org.kie.internal.runtime.manager.Runtime;
import org.kie.internal.task.api.EventService;

public class DefaultRegisterableItemsFactory extends SimpleRegisterableItemsFactory {

    @Inject
    private BeanManager beanManager;
    
    @Override
    public Map<String, WorkItemHandler> getWorkItemHandlers(Runtime runtime) {
        Map<String, WorkItemHandler> defaultHandlers = new HashMap<String, WorkItemHandler>();
        //HT handler 
        WorkItemHandler handler = getHTWorkItemHandler(runtime);
        defaultHandlers.put("Human Task", handler);
        // add any custom registered
        defaultHandlers.putAll(super.getWorkItemHandlers(runtime));
        
        return defaultHandlers;
    }


    @Override
    public List<ProcessEventListener> getProcessEventListeners(Runtime runtime) {
        List<ProcessEventListener> defaultListeners = new ArrayList<ProcessEventListener>();
        
        // add any custom listeners
        defaultListeners.addAll(super.getProcessEventListeners(runtime));
        return defaultListeners;
    }

    @Override
    public List<AgendaEventListener> getAgendaEventListeners(Runtime runtime) {
        List<AgendaEventListener> defaultListeners = new ArrayList<AgendaEventListener>();
        
        // add any custom listeners
        defaultListeners.addAll(super.getAgendaEventListeners(runtime));
        return defaultListeners;
    }

    @Override
    public List<WorkingMemoryEventListener> getWorkingMemoryEventListeners(Runtime runtime) {
        // register JPAWorkingMemoryDBLogger
        AuditLoggerFactory.newInstance(Type.JPA, (StatefulKnowledgeSession)runtime.getKieSession(), null);
        List<WorkingMemoryEventListener> defaultListeners = new ArrayList<WorkingMemoryEventListener>();
        
        // add any custom listeners
        defaultListeners.addAll(super.getWorkingMemoryEventListeners(runtime));
        return defaultListeners;
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected WorkItemHandler getHTWorkItemHandler(Runtime runtime) {
        
        ExternalTaskEventListener listener = null;
        if (beanManager == null) {
            listener = new ExternalTaskEventListener();
        } else {
            Context context = beanManager.getContext(RuntimeManagerScoped.class);
            Set<Bean<?>> beans = beanManager.getBeans(ExternalTaskEventListener.class);
            Bean<ExternalTaskEventListener> bean = (Bean<ExternalTaskEventListener>) beanManager.resolve(beans);
            CreationalContext<ExternalTaskEventListener> creationalContext = beanManager.createCreationalContext(bean);
            listener = context.get(bean, creationalContext);
        }
        
        listener.setRuntimeManager(((RuntimeImpl)runtime).getManager());
        
        LocalHTWorkItemHandler humanTaskHandler = new LocalHTWorkItemHandler();
        humanTaskHandler.setRuntimeManager(((RuntimeImpl)runtime).getManager());
        if (runtime.getTaskService() instanceof EventService) {
            ((EventService)runtime.getTaskService()).registerTaskLifecycleEventListener(listener);
        }
        
        if (runtime instanceof Disposable) {
            ((Disposable)runtime).addDisposeListener(new DisposeListener() {
                
                @Override
                public void onDispose(Runtime runtime) {
                    if (runtime.getTaskService() instanceof EventService) {
                        ((EventService)runtime.getTaskService()).clearTaskLifecycleEventListeners();
                        ((EventService)runtime.getTaskService()).clearTasknotificationEventListeners();
                    }
                }
            });
        }
        return humanTaskHandler;
    }    
}
