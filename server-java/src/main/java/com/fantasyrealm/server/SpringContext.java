package com.fantasyrealm.server;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContext implements ApplicationContextAware {
    private static ApplicationContext ctx;
    @Override public void setApplicationContext(ApplicationContext c) { ctx = c; }
    public static <T> T getBean(Class<T> c) {
        return ctx != null ? ctx.getBean(c) : null;
    }
}
