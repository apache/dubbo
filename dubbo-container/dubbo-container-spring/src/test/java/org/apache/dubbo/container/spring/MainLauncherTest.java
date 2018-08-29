package org.apache.dubbo.container.spring;

import java.util.concurrent.TimeUnit;

import org.apache.dubbo.container.Main;
import org.junit.Assert;
import org.junit.Test;


/**
 * SpringContainerTest
 *
 */
public class MainLauncherTest 
{
	@Test
    public void testMain(){		
        System.setProperty(Main.SHUTDOWN_HOOK_KEY,"true");
        new Thread(new Runnable() {			
			@Override
			public void run() {
				try {
					TimeUnit.SECONDS.sleep(5);
				} catch (InterruptedException e) {
				}
				Class<?> obj = SpringContainer.getContext().getBean("container").getClass();
		        Assert.assertEquals(SpringContainer.class,obj );
		        System.exit(0);
			}
		}).start();
        Main.main(null);
    }
}

