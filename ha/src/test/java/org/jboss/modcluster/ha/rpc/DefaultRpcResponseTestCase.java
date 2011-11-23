package org.jboss.modcluster.ha.rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;

import org.jboss.ha.framework.interfaces.ClusterNode;
import org.junit.Assert;
import org.junit.Test;

public class DefaultRpcResponseTestCase
{
   @Test
   public void testTyped() throws Exception
   {
      this.test(Integer.valueOf(1));
   }
   
   @Test
   public void testVoid() throws Exception
   {
      this.test((Void) null);
   }
   
   private <T> void test(T result) throws Exception
   {
      DefaultRpcResponse<T> response = new DefaultRpcResponse<T>(new ClusterNodeImpl());
      
      this.validate(response);
      
      response.setResult(null);
      
      this.validate(response);
      
      response.setException(new Exception());
      
      this.validate(response);
   }

   private void validate(RpcResponse<?> response) throws Exception
   {
      Assert.assertEquals(response, this.deserialize(this.serialize(response), response.getClass()));
   }
   
   private byte[] serialize(Object object) throws IOException
   {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      
      try
      {
         ObjectOutputStream output = new ObjectOutputStream(out);
         
         try
         {
            output.writeObject(object);
         }
         finally
         {
            output.close();
         }
         
         return out.toByteArray();
      }
      finally
      {
         out.close();
      }
   }

   private <T> T deserialize(byte[] bytes, Class<T> targetClass) throws IOException, ClassNotFoundException
   {
      ByteArrayInputStream in = new ByteArrayInputStream(bytes);
      
      try
      {
         ObjectInputStream input = new ObjectInputStream(in);
         
         try
         {
            return targetClass.cast(input.readObject());
         }
         finally
         {
            input.close();
         }
      }
      finally
      {
         in.close();
      }
   }
   
   public static class ClusterNodeImpl implements ClusterNode
   {
      private static final long serialVersionUID = -1189923414078831527L;

      public int compareTo(ClusterNode o)
      {
         return 0;
      }

      public String getName()
      {
         return null;
      }

      public InetAddress getIpAddress()
      {
         return null;
      }

      public int getPort()
      {
         return 0;
      }
      
      @Override
      public boolean equals(Object object)
      {
         return true;
      }
   }
}
