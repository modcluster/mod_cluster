package org.jboss.modcluster;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Test;

public class SessionDrainingStrategyTest
{
   private Context context = EasyMock.createStrictMock(Context.class);
   
   @Test
   public void defaultStrategy()
   {
      EasyMock.expect(this.context.isDistributable()).andReturn(false);
      
      EasyMock.replay(this.context);
      
      boolean result = SessionDrainingStrategyEnum.DEFAULT.isEnabled(this.context);
      
      EasyMock.verify(this.context);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.context);
      
      EasyMock.expect(this.context.isDistributable()).andReturn(true);
      
      EasyMock.replay(this.context);
      
      result = SessionDrainingStrategyEnum.DEFAULT.isEnabled(this.context);
      
      EasyMock.verify(this.context);
      
      Assert.assertFalse(result);
      
      EasyMock.reset(this.context);
   }
   
   @Test
   public void alwaysStrategy()
   {
      EasyMock.replay(this.context);
      
      boolean result = SessionDrainingStrategyEnum.ALWAYS.isEnabled(this.context);
      
      EasyMock.verify(this.context);
      
      Assert.assertTrue(result);
      
      EasyMock.reset(this.context);
   }
   
   @Test
   public void neverStrategy()
   {
      EasyMock.replay(this.context);
      
      boolean result = SessionDrainingStrategyEnum.NEVER.isEnabled(this.context);
      
      EasyMock.verify(this.context);
      
      Assert.assertFalse(result);
      
      EasyMock.reset(this.context);
   }
}
