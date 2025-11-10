package com.cine.cinelog;

import org.junit.Test;
import org.junit.Assert;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@SpringBootTest
public class CinelogApplicationTest {

    @Test
    public void mainMethodShouldExistAndBePublicStaticVoid() throws NoSuchMethodException {
        Method main = CinelogApplication.class.getMethod("main", String[].class);
        Assert.assertNotNull(main);
        Assert.assertEquals(void.class, main.getReturnType());
        int mods = main.getModifiers();
        Assert.assertTrue("main should be public", Modifier.isPublic(mods));
        Assert.assertTrue("main should be static", Modifier.isStatic(mods));
    }

    @Test
    public void shouldBeAnnotatedWithSpringBootApplicationAndHaveExpectedScanBasePackages() {
        SpringBootApplication ann = CinelogApplication.class.getAnnotation(SpringBootApplication.class);
        Assert.assertNotNull("SpringBootApplication annotation must be present", ann);
        String[] scan = ann.scanBasePackages();
        Assert.assertNotNull(scan);
        boolean contains = false;
        for (String s : scan) {
            if ("com.cine.cinelog".equals(s)) {
                contains = true;
                break;
            }
        }
        Assert.assertTrue("scanBasePackages should contain com.cine.cinelog", contains);
    }
}