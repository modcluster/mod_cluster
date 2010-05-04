package org.jboss.modcluster;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class SystemPropertyJvmRouteFactoryTestCase
{
   @Test
   public void defaultSystemProperty()
   {
      String expected = "expected";
      System.setProperty("jboss.mod_cluster.jvmRoute", expected);
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      JvmRouteFactory defaultFactory = EasyMock.createStrictMock(JvmRouteFactory.class);
      JvmRouteFactory factory = new SystemPropertyJvmRouteFactory(defaultFactory);
      
      EasyMock.expect(defaultFactory.createJvmRoute(engine)).andReturn("unexpected");
      
      EasyMock.replay(defaultFactory);
      
      String result = factory.createJvmRoute(engine);
      
      EasyMock.verify(defaultFactory);
      
      Assert.assertSame(expected, result);
   }
   
   @Test
   public void customSystemProperty()
   {
      String expected = "expected";
      System.setProperty("jboss.jvmRoute", expected);
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      JvmRouteFactory defaultFactory = EasyMock.createStrictMock(JvmRouteFactory.class);
      JvmRouteFactory factory = new SystemPropertyJvmRouteFactory(defaultFactory, "jboss.jvmRoute");
      
      EasyMock.expect(defaultFactory.createJvmRoute(engine)).andReturn("unexpected");
      
      EasyMock.replay(defaultFactory);
      
      String result = factory.createJvmRoute(engine);
      
      EasyMock.verify(defaultFactory);
      
      Assert.assertSame(expected, result);
   }
   
   @Test
   public void defaultJvmRoute()
   {
      String expected = "expected";
      System.clearProperty("jboss.mod_cluster.jvmRoute");
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      JvmRouteFactory defaultFactory = EasyMock.createStrictMock(JvmRouteFactory.class);
      JvmRouteFactory factory = new SystemPropertyJvmRouteFactory(defaultFactory);
      
      EasyMock.expect(defaultFactory.createJvmRoute(engine)).andReturn(expected);
      
      EasyMock.replay(defaultFactory);
      
      String result = factory.createJvmRoute(engine);
      
      EasyMock.verify(defaultFactory);
      
      Assert.assertSame(expected, result);
   }
   
   @Test
   public void customDefaultJvmRoute()
   {
      String expected = "expected";
      System.clearProperty("jboss.jvmRoute");
      
      Engine engine = EasyMock.createStrictMock(Engine.class);
      JvmRouteFactory defaultFactory = EasyMock.createStrictMock(JvmRouteFactory.class);
      JvmRouteFactory factory = new SystemPropertyJvmRouteFactory(defaultFactory, "jboss.jvmRoute");
      
      EasyMock.expect(defaultFactory.createJvmRoute(engine)).andReturn(expected);
      
      EasyMock.replay(defaultFactory);
      
      String result = factory.createJvmRoute(engine);
      
      EasyMock.verify(defaultFactory);
      
      Assert.assertSame(expected, result);
   }
}
