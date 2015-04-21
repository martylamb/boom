package com.martiansoftware.boom;

import com.martiansoftware.dumbtemplates.DumbLazyClasspathTemplateStore;
import com.martiansoftware.dumbtemplates.DumbLazyFileTemplateStore;
import com.martiansoftware.dumbtemplates.DumbLogger;
import com.martiansoftware.dumbtemplates.DumbTemplate;
import com.martiansoftware.dumbtemplates.DumbTemplateStore;
import java.io.IOException;
import java.io.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mlamb
 */
class Templates {
    private static final Logger log = LoggerFactory.getLogger(Templates.class);
    
    static DumbTemplateStore init() {
        // dumbtemplate logging is intentionally... dumb.
        // emsmarten it a little.
        DumbLogger d = new DumbLogger() {
            @Override public void log(String msg) {
                if (msg.startsWith("Error: ")) {
                    log.error(msg.substring(7));
                } else if (msg.startsWith("Warning: ")) {
                    log.warn(msg.substring(9));
                } else log.info(msg);
            }
            @Override public void log(Exception e) {
                log.error(e.getMessage(), e);
            }
        };

        DumbTemplateStore main;
        if (Boom.debug()) {
            main = new DumbLazyFileTemplateStore(new java.io.File("src/main/resources/templates"), d);            
        } else {
            main = new DumbLazyClasspathTemplateStore("/templates", d);            
        }        
        DumbTemplateStore defaults = new DumbLazyClasspathTemplateStore("/boom-default-templates", d);

        return new DumbTemplateStoreWithDefaults(main, defaults);
    }

    private static class DumbTemplateStoreWithDefaults extends DumbTemplateStore {
        private final DumbTemplateStore _main;
        private final DumbTemplateStore _defaults;
        DumbTemplateStoreWithDefaults(DumbTemplateStore main, DumbTemplateStore defaults) {
            _main = main;
            _defaults = defaults;
        }
        @Override public DumbTemplateStore add(String templateName, String templateDef) {
            _main.add(templateName, templateDef);
            return this;
        }
        @Override public DumbTemplateStore add(String templateName, Reader templateDef) throws IOException {        
            _main.add(templateName, templateDef);
            return this;
        }
        @Override public DumbTemplate get(String templateName) {
            DumbTemplate result = _main.get(templateName);
            return (result == null) ? _defaults.get(templateName) : result;
        }        
        
    }
}
