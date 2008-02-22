/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.bean.wcm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.WCMAppModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;

/**
 * Object holding the configuration for a deployment server.
 * 
 * @author Gavin Cornwell
 */
public final class DeploymentServerConfig implements Serializable
{
   public static final String PROP_TYPE = "type";
   public static final String PROP_NAME = "name";
   public static final String PROP_HOST = "host";
   public static final String PROP_PORT = "port";
   public static final String PROP_USER = "username";
   public static final String PROP_PASSWORD = "password";
   public static final String PROP_URL = "url";
   public static final String PROP_SOURCE_PATH = "sourcePath";
   public static final String PROP_TARGET_NAME = "targetName";
   public static final String PROP_ALLOCATED_TO = "allocatedTo";
   public static final String PROP_ON_APPROVAL = "onApproval";
   
   protected String id;
   protected NodeRef serverRef;
   protected String deployType;
   protected Map<String, Object> props;
   
   private static final long serialVersionUID = -8894113751463815194L;
   
   public DeploymentServerConfig(String deployType)
   {
      this.id = GUID.generate();
      this.deployType = deployType;
      this.props = new HashMap<String, Object>(12, 1.0f);
   }
   
   public DeploymentServerConfig(NodeRef serverRef, Map<QName, Serializable> repoProps)
   {
      this.id = GUID.generate();
      this.serverRef = serverRef;
      this.deployType = (String)repoProps.get(WCMAppModel.PROP_DEPLOYTYPE);
      
      populateFromRepoProps(repoProps);
   }

   public String toString()
   {
      StringBuilder buffer = new StringBuilder(super.toString());
      buffer.append(" (id=").append(this.id);
      buffer.append(" serverRef=").append(this.serverRef);
      buffer.append(" deployType=").append(this.deployType);
      buffer.append(" props=").append(this.props).append(")");
      return buffer.toString();
   }
   
   public String getId()
   {
      return this.id;
   }

   public NodeRef getServerRef()
   {
      return this.serverRef;
   }
   
   public String getDeployType()
   {
      return this.deployType;
   }

   public Map<String, Object> getProperties()
   {
      return this.props;
   }
   
   public void setProperties(Map<String, Object> props)
   {
      this.props = new HashMap<String, Object>(props.size());
      this.props.putAll(props);
   }

   public Map<QName, Serializable> getRepoProps()
   {
      Map<QName, Serializable> repoProps = new HashMap<QName, Serializable>(8);
      
      repoProps.put(WCMAppModel.PROP_DEPLOYTYPE, this.deployType);
      repoProps.put(WCMAppModel.PROP_DEPLOYSERVERTYPE, (Serializable)this.props.get(PROP_TYPE));
      
      if (this.props.get(PROP_HOST) != null && ((String)this.props.get(PROP_HOST)).length() > 0)
      {
         repoProps.put(WCMAppModel.PROP_DEPLOYSERVERHOST, (Serializable)this.props.get(PROP_HOST));
      }
      
      if (this.props.get(PROP_PORT) != null && ((String)this.props.get(PROP_PORT)).length() > 0)
      {
         repoProps.put(WCMAppModel.PROP_DEPLOYSERVERPORT, new Integer((String)this.props.get(PROP_PORT)));
      }
      
      if (this.props.get(PROP_NAME) != null && ((String)this.props.get(PROP_NAME)).length() > 0)
      {
         repoProps.put(WCMAppModel.PROP_DEPLOYSERVERNAME, (Serializable)this.props.get(PROP_NAME));
      }
      
      if (this.props.get(PROP_USER) != null && ((String)this.props.get(PROP_USER)).length() > 0)
      {
         repoProps.put(WCMAppModel.PROP_DEPLOYSERVERUSERNAME, (Serializable)this.props.get(PROP_USER));
      }
      
      if (this.props.get(PROP_PASSWORD) != null && ((String)this.props.get(PROP_PASSWORD)).length() > 0)
      {
         repoProps.put(WCMAppModel.PROP_DEPLOYSERVERPASSWORD, (Serializable)this.props.get(PROP_PASSWORD));
      }
      
      if (this.props.get(PROP_URL) != null && ((String)this.props.get(PROP_URL)).length() > 0)
      {
         repoProps.put(WCMAppModel.PROP_DEPLOYSERVERURL, (Serializable)this.props.get(PROP_URL));
      }
      
      if (this.props.get(PROP_SOURCE_PATH) != null && ((String)this.props.get(PROP_SOURCE_PATH)).length() > 0)
      {
         repoProps.put(WCMAppModel.PROP_DEPLOYSOURCEPATH, (Serializable)this.props.get(PROP_SOURCE_PATH));
      }
      
      if (this.props.get(PROP_ALLOCATED_TO) != null && ((String)this.props.get(PROP_ALLOCATED_TO)).length() > 0)
      {
         repoProps.put(WCMAppModel.PROP_DEPLOYSERVERALLOCATEDTO, (Serializable)this.props.get(PROP_ALLOCATED_TO));
      }
      
      // only save the target if it's an FSR (File System Receiver)
      if (WCMAppModel.CONSTRAINT_FILEDEPLOY.equals(this.deployType))
      {
         if (this.props.get(PROP_TARGET_NAME) != null && ((String)this.props.get(PROP_TARGET_NAME)).length() > 0)
         {
            repoProps.put(WCMAppModel.PROP_DEPLOYSERVERTARGET, (Serializable)this.props.get(PROP_TARGET_NAME));
         }
      }
      
      // only save the approval flag if the server type is a 'live' server
      if (WCMAppModel.CONSTRAINT_LIVESERVER.equals(this.props.get(PROP_TYPE)))
      {
         if (this.props.get(PROP_ON_APPROVAL) != null)
         {
            repoProps.put(WCMAppModel.PROP_DEPLOYONAPPROVAL, (Serializable)this.props.get(PROP_ON_APPROVAL));
         }
      }
      
      return repoProps;
   }
   
   protected void populateFromRepoProps(Map<QName, Serializable> repoProps)
   {
      this.props = new HashMap<String, Object>(repoProps.size());
      
      this.props.put(PROP_TYPE, (String)repoProps.get(WCMAppModel.PROP_DEPLOYSERVERTYPE));
      this.props.put(PROP_HOST, (String)repoProps.get(WCMAppModel.PROP_DEPLOYSERVERHOST));
      
      if (repoProps.get(WCMAppModel.PROP_DEPLOYSERVERNAME) != null)
      {
         this.props.put(PROP_NAME, (String)repoProps.get(WCMAppModel.PROP_DEPLOYSERVERNAME));
      }
      
      if (repoProps.get(WCMAppModel.PROP_DEPLOYSERVERPORT) != null)
      {
         Integer port = (Integer)repoProps.get(WCMAppModel.PROP_DEPLOYSERVERPORT);
         this.props.put(PROP_PORT, port.toString());
      }
      
      if (repoProps.get(WCMAppModel.PROP_DEPLOYSERVERUSERNAME) != null)
      {
         this.props.put(PROP_USER, (String)repoProps.get(WCMAppModel.PROP_DEPLOYSERVERUSERNAME));
      }
      
      if (repoProps.get(WCMAppModel.PROP_DEPLOYSERVERPASSWORD) != null)
      {
         this.props.put(PROP_PASSWORD, (String)repoProps.get(WCMAppModel.PROP_DEPLOYSERVERPASSWORD));
      }
      
      if (repoProps.get(WCMAppModel.PROP_DEPLOYSERVERURL) != null)
      {
         this.props.put(PROP_URL, (String)repoProps.get(WCMAppModel.PROP_DEPLOYSERVERURL));
      }
      
      if (repoProps.get(WCMAppModel.PROP_DEPLOYSOURCEPATH) != null)
      {
         this.props.put(PROP_SOURCE_PATH, (String)repoProps.get(WCMAppModel.PROP_DEPLOYSOURCEPATH));
      }
      
      if (repoProps.get(WCMAppModel.PROP_DEPLOYSERVERALLOCATEDTO) != null)
      {
         this.props.put(PROP_ALLOCATED_TO, (String)repoProps.get(WCMAppModel.PROP_DEPLOYSERVERALLOCATEDTO));
      }
      
      if (WCMAppModel.CONSTRAINT_FILEDEPLOY.equals(this.deployType))
      {
         if (repoProps.get(WCMAppModel.PROP_DEPLOYSERVERTARGET) != null)
         {
            this.props.put(PROP_TARGET_NAME, (String)repoProps.get(WCMAppModel.PROP_DEPLOYSERVERTARGET));
         }
      }
      
      if (WCMAppModel.CONSTRAINT_LIVESERVER.equals(this.props.get(PROP_TYPE)))
      {
         Boolean onApproval = null;
         if (repoProps.get(WCMAppModel.PROP_DEPLOYONAPPROVAL) != null)
         {
            onApproval = (Boolean)repoProps.get(WCMAppModel.PROP_DEPLOYONAPPROVAL);
         }
         else
         {
            onApproval = Boolean.FALSE;
         }
         
         this.props.put(PROP_ON_APPROVAL, onApproval);
      }
   }
}