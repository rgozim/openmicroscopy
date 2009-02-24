/*
 *   $Id$
 *
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.scheduler;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.quartz.JobDetailAwareTrigger;

/**
 * Produces a <a href="http://www.opensymphony.com/quartz/Quartz</a>
 * {@link Scheduler} which automatically loads all the triggers it can find.
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 3.0-Beta3
 */
public class SchedulerFactoryBean extends
        org.springframework.scheduling.quartz.SchedulerFactoryBean implements
        ApplicationListener {

    private final static Log log = LogFactory
            .getLog(SchedulerFactoryBean.class);

    private final Map<String, Trigger> triggers = new HashMap<String, Trigger>();

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            ContextRefreshedEvent cre = (ContextRefreshedEvent) event;
            String[] names = cre.getApplicationContext().getBeanNamesForType(
                    Trigger.class);
            for (String name : names) {
                if (triggers.containsKey(name)) {
                    log.error("Scheduler already has trigger named: " + name);
                    continue;
                }
                Trigger trigger = (Trigger) cre.getApplicationContext()
                        .getBean(name);
                registerTrigger(name, trigger);
            }
            if (!isRunning()) {
                start();
            }
        }
    }

    /**
     * Registers a {@link JobDetailAwareTrigger}. A method like this should
     * really have protected visibility in the superclass.
     */
    protected void registerTrigger(String beanName, Trigger trigger) {
        try {
            Scheduler scheduler = (Scheduler) getObject();
            triggers.put(beanName, trigger);
            JobDetailAwareTrigger jdat = (JobDetailAwareTrigger) trigger;
            JobDetail job = jdat.getJobDetail();
            scheduler.addJob(job, false);
            scheduler.scheduleJob(trigger);
        } catch (SchedulerException se) {
            throw new RuntimeException(se);
        }
    }
}
