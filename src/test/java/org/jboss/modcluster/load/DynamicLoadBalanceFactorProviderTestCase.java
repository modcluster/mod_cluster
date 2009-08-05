/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.modcluster.load;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.easymock.EasyMock;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadContext;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.modcluster.load.metric.LoadMetricSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Paul Ferraro
 *
 */
@SuppressWarnings({ "unchecked", "boxing" })
public class DynamicLoadBalanceFactorProviderTestCase
{
   LoadMetric<LoadContext> metric1 = EasyMock.createStrictMock(LoadMetric.class);
   LoadMetric<LoadContext> metric2 = EasyMock.createStrictMock(LoadMetric.class);
   LoadMetric<LoadContext> metric3 = EasyMock.createStrictMock(LoadMetric.class);
   private final LoadMetricSource<LoadContext> source1 = EasyMock.createStrictMock(LoadMetricSource.class);
   private final LoadMetricSource<LoadContext> source2 = EasyMock.createStrictMock(LoadMetricSource.class);
   
   private DynamicLoadBalanceFactorProvider provider;
   
   
   @Before
   public void init() throws Exception
   {
      EasyMock.expect(this.metric1.getSource()).andReturn(this.source1);
      EasyMock.expect(this.metric2.getSource()).andReturn(this.source1);
      EasyMock.expect(this.metric3.getSource()).andReturn(this.source2);
      
      EasyMock.replay(this.metric1, this.metric2, this.metric3);
      
      this.provider = new DynamicLoadBalanceFactorProvider(new LinkedHashSet<LoadMetric<LoadContext>>(Arrays.asList(this.metric1, this.metric2, this.metric3)));
      
      EasyMock.verify(this.metric1, this.metric2, this.metric3);
      EasyMock.reset(this.metric1, this.metric2, this.metric3);
      
      this.provider.setHistory(1);
   }

   @Test
   public void getLoadBalanceFactor() throws Exception
   {
      LoadContext context1 = EasyMock.createStrictMock(LoadContext.class);
      
      EasyMock.expect(this.metric1.getWeight()).andReturn(1);
      
      EasyMock.expect(this.source1.createContext()).andReturn(context1);

      EasyMock.expect(this.metric1.getWeight()).andReturn(1);
      EasyMock.expect(this.metric1.getLoad(context1)).andReturn(0.2);
      EasyMock.expect(this.metric1.getCapacity()).andReturn(1d);
      
      EasyMock.expect(this.metric2.getWeight()).andReturn(2);
      EasyMock.expect(this.metric2.getLoad(context1)).andReturn(400d);
      EasyMock.expect(this.metric2.getCapacity()).andReturn(1000d);
      
      context1.close();
      
      EasyMock.expect(this.metric3.getWeight()).andReturn(0);
      
      EasyMock.replay(this.source1, this.source2, context1, this.metric1, this.metric2, this.metric3);
      
      int loadBalanceFactor = this.provider.getLoadBalanceFactor();
      
      EasyMock.verify(this.source1, this.source2, context1, this.metric1, this.metric2, this.metric3);
      
      Assert.assertEquals(67, loadBalanceFactor);
      
      EasyMock.reset(this.source1, this.source2, context1, this.metric1, this.metric2, this.metric3);
      
      // Test time-decay function
      EasyMock.expect(this.metric1.getWeight()).andReturn(1);
      
      EasyMock.expect(this.source1.createContext()).andReturn(context1);

      EasyMock.expect(this.metric1.getWeight()).andReturn(1);
      EasyMock.expect(this.metric1.getLoad(context1)).andReturn(0.4);
      EasyMock.expect(this.metric1.getCapacity()).andReturn(1d);
      
      EasyMock.expect(this.metric2.getWeight()).andReturn(2);
      EasyMock.expect(this.metric2.getLoad(context1)).andReturn(600d);
      EasyMock.expect(this.metric2.getCapacity()).andReturn(1000d);
      
      context1.close();
      
      EasyMock.expect(this.metric3.getWeight()).andReturn(0);
      
      EasyMock.replay(this.source1, this.source2, context1, this.metric1, this.metric2, this.metric3);
      
      loadBalanceFactor = this.provider.getLoadBalanceFactor();
      
      EasyMock.verify(this.source1, this.source2, context1, this.metric1, this.metric2, this.metric3);
      
      Assert.assertEquals(53, loadBalanceFactor);
      
      EasyMock.reset(this.source1, this.source2, context1, this.metric1, this.metric2, this.metric3);
      
      //Test decay queue overflow
      EasyMock.expect(this.metric1.getWeight()).andReturn(1);
      
      EasyMock.expect(this.source1.createContext()).andReturn(context1);

      EasyMock.expect(this.metric1.getWeight()).andReturn(1);
      EasyMock.expect(this.metric1.getLoad(context1)).andReturn(0.3);
      EasyMock.expect(this.metric1.getCapacity()).andReturn(1d);
      
      EasyMock.expect(this.metric2.getWeight()).andReturn(2);
      EasyMock.expect(this.metric2.getLoad(context1)).andReturn(300d);
      EasyMock.expect(this.metric2.getCapacity()).andReturn(1000d);
      
      context1.close();
      
      EasyMock.expect(this.metric3.getWeight()).andReturn(0);
      
      EasyMock.replay(this.source1, this.source2, context1, this.metric1, this.metric2, this.metric3);
      
      loadBalanceFactor = this.provider.getLoadBalanceFactor();
      
      EasyMock.verify(this.source1, this.source2, context1, this.metric1, this.metric2, this.metric3);
      
      Assert.assertEquals(62, loadBalanceFactor);
      
      EasyMock.reset(this.source1, this.source2, context1, this.metric1, this.metric2, this.metric3);
   }
}
