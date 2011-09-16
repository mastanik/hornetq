/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hornetq.tests.integration.stomp.v11;

import java.io.IOException;

import javax.jms.Message;
import javax.jms.MessageConsumer;

import junit.framework.Assert;

import org.hornetq.core.logging.Logger;
import org.hornetq.tests.integration.stomp.util.ClientStompFrame;
import org.hornetq.tests.integration.stomp.util.StompClientConnection;
import org.hornetq.tests.integration.stomp.util.StompClientConnectionFactory;
import org.hornetq.tests.integration.stomp.util.StompClientConnectionV11;


public class StompTestV11 extends StompTestBase2
{
   private static final transient Logger log = Logger.getLogger(StompTestV11.class);
   
   private StompClientConnection connV11;
   
   protected void setUp() throws Exception
   {
      super.setUp();
      connV11 = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
   }
   
   protected void tearDown() throws Exception
   {
      if (connV11.isConnected())
      {
         connV11.disconnect();
      }
      super.tearDown();
   }
   
   public void testConnection() throws Exception
   {
      StompClientConnection connection = StompClientConnectionFactory.createClientConnection("1.0", hostname, port);
      
      connection.connect(defUser, defPass);
      
      assertTrue(connection.isConnected());
      
      assertEquals("1.0", connection.getVersion());
      
      connection.disconnect();

      connection = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      
      connection.connect(defUser, defPass);
      
      assertTrue(connection.isConnected());
      
      assertEquals("1.1", connection.getVersion());
      
      connection.disconnect();
      
      connection = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      
      connection.connect();
      
      assertFalse(connection.isConnected());
      
      //new way of connection
      StompClientConnectionV11 conn = (StompClientConnectionV11) StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      conn.connect1(defUser, defPass);
      
      assertTrue(conn.isConnected());
      
      conn.disconnect();
   }
   
   public void testNegotiation() throws Exception
   {
      // case 1 accept-version absent. It is a 1.0 connect
      ClientStompFrame frame = connV11.createFrame("CONNECT");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      
      ClientStompFrame reply = connV11.sendFrame(frame);
      
      assertEquals("CONNECTED", reply.getCommand());
      
      //reply headers: version, session, server
      assertEquals(null, reply.getHeader("version"));

      connV11.disconnect();

      // case 2 accept-version=1.0, result: 1.0
      connV11 = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      frame = connV11.createFrame("CONNECT");
      frame.addHeader("accept-version", "1.0");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      
      reply = connV11.sendFrame(frame);
      
      assertEquals("CONNECTED", reply.getCommand());
      
      //reply headers: version, session, server
      assertEquals("1.0", reply.getHeader("version"));
      
      connV11.disconnect();

      // case 3 accept-version=1.1, result: 1.1
      connV11 = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      frame = connV11.createFrame("CONNECT");
      frame.addHeader("accept-version", "1.1");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      
      reply = connV11.sendFrame(frame);
      
      assertEquals("CONNECTED", reply.getCommand());
      
      //reply headers: version, session, server
      assertEquals("1.1", reply.getHeader("version"));
      
      connV11.disconnect();

      // case 4 accept-version=1.0,1.1,1.2, result 1.1
      connV11 = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      frame = connV11.createFrame("CONNECT");
      frame.addHeader("accept-version", "1.0,1.1,1.2");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      
      reply = connV11.sendFrame(frame);
      
      assertEquals("CONNECTED", reply.getCommand());
      
      //reply headers: version, session, server
      assertEquals("1.1", reply.getHeader("version"));
      
      connV11.disconnect();

      // case 5 accept-version=1.2, result error
      connV11 = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      frame = connV11.createFrame("CONNECT");
      frame.addHeader("accept-version", "1.2");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      
      reply = connV11.sendFrame(frame);
      
      assertEquals("ERROR", reply.getCommand());
      
      System.out.println("Got error frame " + reply);
      
   }
   
   public void testSendAndReceive() throws Exception
   {
      connV11.connect(defUser, defPass);
      ClientStompFrame frame = connV11.createFrame("SEND");
      frame.addHeader("destination", getQueuePrefix() + getQueueName());
      frame.addHeader("content-type", "text/plain");
      frame.setBody("Hello World 1!");
      
      ClientStompFrame response = connV11.sendFrame(frame);
      
      assertNull(response);
      
      frame.addHeader("receipt", "1234");
      frame.setBody("Hello World 2!");
      
      response = connV11.sendFrame(frame);
      
      assertNotNull(response);
      
      assertEquals("RECEIPT", response.getCommand());
      
      assertEquals("1234", response.getHeader("receipt-id"));
      
      //subscribe
      StompClientConnection newConn = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      newConn.connect(defUser, defPass);
      
      ClientStompFrame subFrame = newConn.createFrame("SUBSCRIBE");
      subFrame.addHeader("id", "a-sub");
      subFrame.addHeader("destination", getQueuePrefix() + getQueueName());
      subFrame.addHeader("ack", "auto");
      
      newConn.sendFrame(subFrame);
      
      frame = newConn.receiveFrame();
      
      System.out.println("received " + frame);
      
      assertEquals("MESSAGE", frame.getCommand());
      
      assertEquals("a-sub", frame.getHeader("subscription"));
      
      assertNotNull(frame.getHeader("message-id"));
      
      assertEquals(getQueuePrefix() + getQueueName(), frame.getHeader("destination"));
      
      assertEquals("Hello World 1!", frame.getBody());
      
      frame = newConn.receiveFrame();
      
      System.out.println("received " + frame);      
      
      //unsub
      ClientStompFrame unsubFrame = newConn.createFrame("UNSUBSCRIBE");
      unsubFrame.addHeader("id", "a-sub");
      
      newConn.disconnect();
   }

   public void testHeaderContentType() throws Exception
   {
      connV11.connect(defUser, defPass);
      ClientStompFrame frame = connV11.createFrame("SEND");
      frame.addHeader("destination", getQueuePrefix() + getQueueName());
      frame.addHeader("content-type", "application/xml");
      frame.setBody("Hello World 1!");
      
      connV11.sendFrame(frame);
      
      //subscribe
      StompClientConnection newConn = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      newConn.connect(defUser, defPass);
      
      ClientStompFrame subFrame = newConn.createFrame("SUBSCRIBE");
      subFrame.addHeader("id", "a-sub");
      subFrame.addHeader("destination", getQueuePrefix() + getQueueName());
      subFrame.addHeader("ack", "auto");
      
      newConn.sendFrame(subFrame);
      
      frame = newConn.receiveFrame();
      
      System.out.println("received " + frame);
      
      assertEquals("MESSAGE", frame.getCommand());
      
      assertEquals("application/xml", frame.getHeader("content-type"));
      
      //unsub
      ClientStompFrame unsubFrame = newConn.createFrame("UNSUBSCRIBE");
      unsubFrame.addHeader("id", "a-sub");
      
      newConn.disconnect();
   }

   public void testHeaderContentLength() throws Exception
   {
      connV11.connect(defUser, defPass);
      ClientStompFrame frame = connV11.createFrame("SEND");
      
      String body = "Hello World 1!";
      String cLen = String.valueOf(body.getBytes("UTF-8").length);
      
      frame.addHeader("destination", getQueuePrefix() + getQueueName());
      frame.addHeader("content-type", "application/xml");
      frame.addHeader("content-length", cLen);
      frame.setBody(body + "extra");
      
      connV11.sendFrame(frame);
      
      //subscribe
      StompClientConnection newConn = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      newConn.connect(defUser, defPass);
      
      ClientStompFrame subFrame = newConn.createFrame("SUBSCRIBE");
      subFrame.addHeader("id", "a-sub");
      subFrame.addHeader("destination", getQueuePrefix() + getQueueName());
      subFrame.addHeader("ack", "auto");
      
      newConn.sendFrame(subFrame);
      
      frame = newConn.receiveFrame();
      
      System.out.println("received " + frame);
      
      assertEquals("MESSAGE", frame.getCommand());
      
      assertEquals(cLen, frame.getHeader("content-length"));
      
      //unsub
      ClientStompFrame unsubFrame = newConn.createFrame("UNSUBSCRIBE");
      unsubFrame.addHeader("id", "a-sub");
      
      newConn.disconnect();
   }

   public void testHeaderEncoding() throws Exception
   {
      connV11.connect(defUser, defPass);
      ClientStompFrame frame = connV11.createFrame("SEND");
      
      String body = "Hello World 1!";
      String cLen = String.valueOf(body.getBytes("UTF-8").length);
      
      frame.addHeader("destination", getQueuePrefix() + getQueueName());
      frame.addHeader("content-type", "application/xml");
      frame.addHeader("content-length", cLen);
      String hKey = "special-header\\\\\\n\\:";
      String hVal = "\\:\\\\\\ngood";
      frame.addHeader(hKey, hVal);
      
      System.out.println("key: |" + hKey + "| val: |" + hVal);
      
      frame.setBody(body);
      
      connV11.sendFrame(frame);
      
      //subscribe
      StompClientConnection newConn = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      newConn.connect(defUser, defPass);
      
      ClientStompFrame subFrame = newConn.createFrame("SUBSCRIBE");
      subFrame.addHeader("id", "a-sub");
      subFrame.addHeader("destination", getQueuePrefix() + getQueueName());
      subFrame.addHeader("ack", "auto");
      
      newConn.sendFrame(subFrame);
      
      frame = newConn.receiveFrame();
      
      System.out.println("received " + frame);
      
      assertEquals("MESSAGE", frame.getCommand());
      
      String value = frame.getHeader("special-header" + "\\" + "\n" + ":");
      
      assertEquals(":" + "\\" + "\n" + "good", value);
      
      //unsub
      ClientStompFrame unsubFrame = newConn.createFrame("UNSUBSCRIBE");
      unsubFrame.addHeader("id", "a-sub");
      
      newConn.disconnect();
   }
   
   public void testHeartBeat() throws Exception
   {
      //no heart beat at all if heat-beat absent
      ClientStompFrame frame = connV11.createFrame("CONNECT");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      
      ClientStompFrame reply = connV11.sendFrame(frame);
      
      assertEquals("CONNECTED", reply.getCommand());
      
      Thread.sleep(5000);
      
      assertEquals(0, connV11.getFrameQueueSize());
      
      connV11.disconnect();
      
      //no heart beat for (0,0)
      connV11 = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      frame = connV11.createFrame("CONNECT");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      frame.addHeader("heart-beat", "0,0");
      frame.addHeader("accept-version", "1.0,1.1");
      
      reply = connV11.sendFrame(frame);
      
      assertEquals("CONNECTED", reply.getCommand());
      
      assertEquals("0,0", reply.getHeader("heart-beat"));
      
      Thread.sleep(5000);
      
      assertEquals(0, connV11.getFrameQueueSize());
      
      connV11.disconnect();

      //heart-beat (1,0), should receive a min client ping accepted by server
      connV11 = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      frame = connV11.createFrame("CONNECT");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      frame.addHeader("heart-beat", "1,0");
      frame.addHeader("accept-version", "1.0,1.1");
      
      reply = connV11.sendFrame(frame);
      
      assertEquals("CONNECTED", reply.getCommand());
      
      assertEquals("0,500", reply.getHeader("heart-beat"));
      
      Thread.sleep(2000);
      
      //now server side should be disconnected because we didn't send ping for 2 sec
      frame = connV11.createFrame("SEND");
      frame.addHeader("destination", getQueuePrefix() + getQueueName());
      frame.addHeader("content-type", "text/plain");
      frame.setBody("Hello World");

      //send will fail
      try
      {
         connV11.sendFrame(frame);
         fail("connection should have been destroyed by now");
      }
      catch (IOException e)
      {
         //ignore
      }
      
      //heart-beat (1,0), start a ping, then send a message, should be ok.
      connV11 = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      frame = connV11.createFrame("CONNECT");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      frame.addHeader("heart-beat", "1,0");
      frame.addHeader("accept-version", "1.0,1.1");
      
      reply = connV11.sendFrame(frame);
      
      assertEquals("CONNECTED", reply.getCommand());
      
      assertEquals("0,500", reply.getHeader("heart-beat"));
      
      System.out.println("========== start pinger!");
      
      connV11.startPinger(500);
      
      Thread.sleep(2000);
      
      //now server side should be disconnected because we didn't send ping for 2 sec
      frame = connV11.createFrame("SEND");
      frame.addHeader("destination", getQueuePrefix() + getQueueName());
      frame.addHeader("content-type", "text/plain");
      frame.setBody("Hello World");

      //send will be ok
      connV11.sendFrame(frame);
      
      connV11.stopPinger();
      
      connV11.disconnect();

   }
   
   //server ping
   public void testHeartBeat2() throws Exception
   {
      //heart-beat (1,1)
      ClientStompFrame frame = connV11.createFrame("CONNECT");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      frame.addHeader("heart-beat", "1,1");
      frame.addHeader("accept-version", "1.0,1.1");
      
      ClientStompFrame reply = connV11.sendFrame(frame);
      
      assertEquals("CONNECTED", reply.getCommand());
      
      assertEquals("500,500", reply.getHeader("heart-beat"));
      
      connV11.disconnect();
      
      //heart-beat (500,1000)
      connV11 = StompClientConnectionFactory.createClientConnection("1.1", hostname, port);
      frame = connV11.createFrame("CONNECT");
      frame.addHeader("host", "127.0.0.1");
      frame.addHeader("login", this.defUser);
      frame.addHeader("passcode", this.defPass);
      frame.addHeader("heart-beat", "500,1000");
      frame.addHeader("accept-version", "1.0,1.1");
      
      reply = connV11.sendFrame(frame);
      
      assertEquals("CONNECTED", reply.getCommand());
      
      assertEquals("1000,500", reply.getHeader("heart-beat"));
      
      System.out.println("========== start pinger!");
      
      connV11.startPinger(500);
      
      Thread.sleep(10000);
      
      //now check the frame size
      int size = connV11.getFrameQueueSize();
      
      System.out.println("ping received: " + size);
      
      assertTrue(size > 5);
      
      //now server side should be disconnected because we didn't send ping for 2 sec
      frame = connV11.createFrame("SEND");
      frame.addHeader("destination", getQueuePrefix() + getQueueName());
      frame.addHeader("content-type", "text/plain");
      frame.setBody("Hello World");

      //send will be ok
      connV11.sendFrame(frame);
      
      connV11.stopPinger();
      
      connV11.disconnect();

   }
   
   public void testNack() throws Exception
   {
      connV11.connect(defUser, defPass);

      subscribe(connV11, "sub1", "client");

      sendMessage(getName());

      ClientStompFrame frame = connV11.receiveFrame();

      String messageID = frame.getHeader("message-id");
      
      System.out.println("Received message with id " + messageID);
      
      nack(connV11, "sub1", messageID);
      
      connV11.disconnect();

      //Nack makes the message be dropped.
      MessageConsumer consumer = session.createConsumer(queue);
      Message message = consumer.receive(1000);
      Assert.assertNull(message);
   }

   public void testNackWithWrongSubId() throws Exception
   {
      connV11.connect(defUser, defPass);

      subscribe(connV11, "sub1", "client");

      sendMessage(getName());

      ClientStompFrame frame = connV11.receiveFrame();

      String messageID = frame.getHeader("message-id");
      
      System.out.println("Received message with id " + messageID);
      
      nack(connV11, "sub2", messageID);
      
      ClientStompFrame error = connV11.receiveFrame();
      
      System.out.println("Receiver error: " + error);
      
      connV11.disconnect();

      //message should be still there
      MessageConsumer consumer = session.createConsumer(queue);
      Message message = consumer.receive(1000);
      Assert.assertNotNull(message);
   }

   public void testNackWithWrongMessageId() throws Exception
   {
      connV11.connect(defUser, defPass);

      subscribe(connV11, "sub1", "client");

      sendMessage(getName());

      ClientStompFrame frame = connV11.receiveFrame();

      String messageID = frame.getHeader("message-id");
      
      System.out.println("Received message with id " + messageID);
      
      nack(connV11, "sub2", "someother");
      
      ClientStompFrame error = connV11.receiveFrame();
      
      System.out.println("Receiver error: " + error);
      
      connV11.disconnect();

      //message should still there
      MessageConsumer consumer = session.createConsumer(queue);
      Message message = consumer.receive(1000);
      Assert.assertNotNull(message);
   }
   
   
   public void testAck() throws Exception
   {
      connV11.connect(defUser, defPass);

      subscribe(connV11, "sub1", "client");

      sendMessage(getName());

      ClientStompFrame frame = connV11.receiveFrame();

      String messageID = frame.getHeader("message-id");
      
      System.out.println("Received message with id " + messageID);
      
      ack(connV11, "sub1", messageID);
      
      connV11.disconnect();

      //Nack makes the message be dropped.
      MessageConsumer consumer = session.createConsumer(queue);
      Message message = consumer.receive(1000);
      Assert.assertNull(message);
   }

   public void testAckWithWrongSubId() throws Exception
   {
      connV11.connect(defUser, defPass);

      subscribe(connV11, "sub1", "client");

      sendMessage(getName());

      ClientStompFrame frame = connV11.receiveFrame();

      String messageID = frame.getHeader("message-id");
      
      System.out.println("Received message with id " + messageID);
      
      ack(connV11, "sub2", messageID);
      
      ClientStompFrame error = connV11.receiveFrame();
      
      System.out.println("Receiver error: " + error);
      
      connV11.disconnect();

      //message should be still there
      MessageConsumer consumer = session.createConsumer(queue);
      Message message = consumer.receive(1000);
      Assert.assertNotNull(message);
   }

   public void testAckWithWrongMessageId() throws Exception
   {
      connV11.connect(defUser, defPass);

      subscribe(connV11, "sub1", "client");

      sendMessage(getName());

      ClientStompFrame frame = connV11.receiveFrame();

      String messageID = frame.getHeader("message-id");
      
      System.out.println("Received message with id " + messageID);
      
      ack(connV11, "sub2", "someother");
      
      ClientStompFrame error = connV11.receiveFrame();
      
      System.out.println("Receiver error: " + error);
      
      connV11.disconnect();

      //message should still there
      MessageConsumer consumer = session.createConsumer(queue);
      Message message = consumer.receive(1000);
      Assert.assertNotNull(message);
   }
   
   public void testErrorWithReceipt() throws Exception
   {
      connV11.connect(defUser, defPass);

      subscribe(connV11, "sub1", "client");

      sendMessage(getName());

      ClientStompFrame frame = connV11.receiveFrame();

      String messageID = frame.getHeader("message-id");
      
      System.out.println("Received message with id " + messageID);
      
      ClientStompFrame ackFrame = connV11.createFrame("ACK");
      //give it a wrong sub id
      ackFrame.addHeader("subscription", "sub2");
      ackFrame.addHeader("message-id", messageID);
      ackFrame.addHeader("receipt", "answer-me");
      
      ClientStompFrame error = connV11.sendFrame(ackFrame);
      
      System.out.println("Receiver error: " + error);
      
      assertEquals("ERROR", error.getCommand());
      
      assertEquals("answer-me", error.getHeader("receipt-id"));
      
      connV11.disconnect();

      //message should still there
      MessageConsumer consumer = session.createConsumer(queue);
      Message message = consumer.receive(1000);
      Assert.assertNotNull(message);      
   }
   
   public void testErrorWithReceipt2() throws Exception
   {
      connV11.connect(defUser, defPass);

      subscribe(connV11, "sub1", "client");

      sendMessage(getName());

      ClientStompFrame frame = connV11.receiveFrame();

      String messageID = frame.getHeader("message-id");
      
      System.out.println("Received message with id " + messageID);
      
      ClientStompFrame ackFrame = connV11.createFrame("ACK");
      //give it a wrong sub id
      ackFrame.addHeader("subscription", "sub1");
      ackFrame.addHeader("message-id", String.valueOf(Long.valueOf(messageID) + 1));
      ackFrame.addHeader("receipt", "answer-me");
      
      ClientStompFrame error = connV11.sendFrame(ackFrame);
      
      System.out.println("Receiver error: " + error);
      
      assertEquals("ERROR", error.getCommand());
      
      assertEquals("answer-me", error.getHeader("receipt-id"));
      
      connV11.disconnect();

      //message should still there
      MessageConsumer consumer = session.createConsumer(queue);
      Message message = consumer.receive(1000);
      Assert.assertNotNull(message);      
   }

   private void ack(StompClientConnection conn, String subId, String mid) throws IOException, InterruptedException
   {
      ClientStompFrame ackFrame = conn.createFrame("ACK");
      ackFrame.addHeader("subscription", subId);
      ackFrame.addHeader("message-id", mid);
      
      conn.sendFrame(ackFrame);
   }

   private void nack(StompClientConnection conn, String subId, String mid) throws IOException, InterruptedException
   {
      ClientStompFrame ackFrame = conn.createFrame("NACK");
      ackFrame.addHeader("subscription", subId);
      ackFrame.addHeader("message-id", mid);
      
      conn.sendFrame(ackFrame);
   }
   
   private void subscribe(StompClientConnection conn, String subId, String ack) throws IOException, InterruptedException
   {
      ClientStompFrame subFrame = conn.createFrame("SUBSCRIBE");
      subFrame.addHeader("id", subId);
      subFrame.addHeader("destination", getQueuePrefix() + getQueueName());
      subFrame.addHeader("ack", ack);
      
      conn.sendFrame(subFrame);
   }

}





